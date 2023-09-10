package me.alek.serversecurity.fullstack.socket.methods;

import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import me.alek.serversecurity.fullstack.socket.IStereotypedBeanSocketTransferMethod;
import me.alek.serversecurity.fullstack.socket.IWaitableSocketTransferMethod;
import me.alek.serversecurity.fullstack.socket.SocketPipelineContext;
import me.alek.serversecurity.malware.model.result.CheckResult;
import me.alek.serversecurity.malware.scanning.MalwareScanner;
import org.apache.tomcat.util.codec.binary.Base64;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

public class PluginHashGeneratorMethod implements IWaitableSocketTransferMethod<Boolean>, IStereotypedBeanSocketTransferMethod {

    private final PluginService hashService;

    public PluginHashGeneratorMethod(PluginService hashService) { this.hashService = hashService; }

    private boolean hasFileMalware(MalwareScanner scanner) {
        scanner.startScan();

        return scanner.hasMalware() || scanner.getFlatResultData().stream()
                .flatMap(resultData -> resultData.getResults().stream())
                .anyMatch(result -> result.getDetection().equals("Cracked"));
    }

    private List<String> getResults(MalwareScanner scanner) {
        return scanner.getFlatResultData().get(0).getResults().stream().map(CheckResult::getDetection).collect(Collectors.toList());
    }

    private String getHash(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        byte[] fileData = Files.readAllBytes(Path.of(file.getPath()));
        byte[] hash = messageDigest.digest(fileData);

        return Base64.encodeBase64String(hash);
    }

    private boolean deleteFile(int id, File file) {
        try {
            Files.deleteIfExists(Paths.get(file.getPath()));

        } catch (Exception ex) {
            ex.printStackTrace();
            DiscordBot.get().log("**" + id + "**: (Hash Method) Error occurred when deleting file " + file.getName() + ".");
        }
        return false;
    }

    @Override
    public Boolean handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context) {
        String name = context.getStorage().poll(String.class);
        String version = context.getStorage().poll(String.class);

        File file = context.getStorage().poll(File.class);
        MalwareScanner scanner = new MalwareScanner(file, true);

        if (file == null || !file.exists()) {
            DiscordBot.get().log("**" + context.getId() + "**: (Hash Method) Unknown error occurred when generating plugin hash.");

            return deleteFile(context.getId(), file);
        }
        if (name == null || version == null) {
            DiscordBot.get().log("**" + context.getId() + "**: (Hash Method) Client failed to send valid details about the plugin signature.");

            return deleteFile(context.getId(), file);
        }
        try {
            String checksum = getHash(file);

            if (hasFileMalware(scanner)) {
                DiscordBot.get().log("**" + context.getId() + "**: (Hash Method) File has malware or is cracked, no hash of this file will be stored.");

                hashService.saveJarWindowOfPlugin(file.getName(), getResults(scanner), name, version, checksum, true);
                return true;
            }
            if (hashService.hasRegisteredHash(name, version, checksum)) {
                DiscordBot.get().log("**" + context.getId() + "**: (Hash Method) Hash is already stored for plugin " + name + " " + version + " (" + checksum + ")");

                return deleteFile(context.getId(), file);
            }
            if (hashService.isHashBlacklisted(name, version, checksum)) {
                DiscordBot.get().log("**" + context.getId() + "**: (Hash Method) Hash is blacklisted for plugin " + name + " " + version + " (" + checksum + ")");

                return deleteFile(context.getId(), file);
            }
            DiscordBot.get().log("**" + context.getId() + "**: (Hash Method) Saving hash of plugin " + name + " " + version + " (" + checksum + ")");

            hashService.saveJarWindowOfPlugin(file.getName(), getResults(scanner), name, version, checksum, false);
            return true;
         } catch (Exception ex) {

            if (context.hasBeenClosedByKeepAlive()) return true;

            ex.printStackTrace();
            DiscordBot.get().log("**" + context.getId() + "**: (Hash Method) Error occurred when generating plugin hash: " + ex.getMessage());

            return false;
        }
    }
}
