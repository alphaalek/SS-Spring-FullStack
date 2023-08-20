package me.alek.serversecurity.fullstack.socket.methods;

import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.restapi.service.HashService;
import me.alek.serversecurity.fullstack.socket.ISocketTransferMethod;
import me.alek.serversecurity.fullstack.socket.IStereotypedBeanSocketTransferMethod;
import me.alek.serversecurity.fullstack.socket.IWaitableSocketTransferMethod;
import me.alek.serversecurity.fullstack.socket.SocketPipelineContext;
import me.alek.serversecurity.malware.scanning.VulnerabilityScanner;
import org.apache.tomcat.util.codec.binary.Base64;

import java.io.File;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;

public class PluginHashGeneratorMethod implements ISocketTransferMethod<Boolean>, IStereotypedBeanSocketTransferMethod {

    private final HashService hashService;

    public PluginHashGeneratorMethod(HashService hashService) { this.hashService = hashService; }

    private boolean hasFileMalware(File file) {
        VulnerabilityScanner scanner = new VulnerabilityScanner(file);

        scanner.startScan();
        scanner.await();

        return scanner.hasMalware();
    }

    private boolean hasAlreadyhash(String name, String version, String hash) {
        Optional<Map<String, String>> mapOptional = hashService.getHashesOfPlugin(name, version);

        return mapOptional.map(stringStringMap -> stringStringMap.containsKey(hash)).orElse(false);
    }

    @Override
    public Boolean handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context) {
        String name = context.getStorage().poll(String.class);
        String version = context.getStorage().poll(String.class);

        File file = context.getStorage().poll(File.class);
        if (hasFileMalware(file)) {
            DiscordBot.log("**" + context.getId() + "**: (Hash Method) File has malware, no hash of this file will be stored.");
            return false;
        }
        context.sendKeepAlive();

        if (file == null || !file.exists()) {
            DiscordBot.log("**" + context.getId() + "**: (Hash Method) Unknown error occurred when generating plugin hash.");
            return false;
        }
        if (name == null || version == null) {
            DiscordBot.log("**" + context.getId() + "**: (Hash Method) Client failed to send valid details about the plugin signature.");
            return false;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] fileData = Files.readAllBytes(Path.of(file.getPath()));
            byte[] hash = messageDigest.digest(fileData);

            String checksum = Base64.encodeBase64String(hash);

            if (hasAlreadyhash(name, version, checksum)) {
                DiscordBot.log("**" + context.getId() + "**: (Hash Method) Hash is already stored for plugin " + name + " " + version + " (" + checksum + ")");

                Files.delete(Paths.get(file.getPath()));
                return false;
            }

            DiscordBot.log("**" + context.getId() + "**: (Hash Method) Saving hash of plugin " + name + " " + version + " (" + checksum + ")");

            hashService.saveHashOfPlugin(file.getName(), name, version, checksum);

            return true;
         } catch (Exception ex) {

            if (context.hasBeenClosedByKeepAlive()) return true;

            ex.printStackTrace();
            DiscordBot.log("**" + context.getId() + "**: (Hash Method) Error occurred when generating plugin hash: " + ex.getMessage());
            return false;
        }
    }
}
