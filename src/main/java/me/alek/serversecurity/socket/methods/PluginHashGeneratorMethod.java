package me.alek.serversecurity.socket.methods;

import me.alek.serversecurity.bot.DiscordBot;
import me.alek.serversecurity.malware.scanning.VulnerabilityScanner;
import me.alek.serversecurity.restapi.model.PluginDBEntry;
import me.alek.serversecurity.restapi.service.HashService;
import me.alek.serversecurity.socket.ISocketTransferMethod;
import me.alek.serversecurity.socket.IStereotypedBeanSocketTransferMethod;
import me.alek.serversecurity.socket.IWaitableSocketTransferMethod;
import me.alek.serversecurity.socket.SocketPipelineContext;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;

public class PluginHashGeneratorMethod implements IWaitableSocketTransferMethod<Boolean>, IStereotypedBeanSocketTransferMethod {

    private final HashService hashService;

    public PluginHashGeneratorMethod(HashService hashService) { this.hashService = hashService; }

    private boolean hasFileMalware(File file) {
        VulnerabilityScanner scanner = new VulnerabilityScanner(Collections.singletonList(file));

        scanner.startScan();
        scanner.await();

        return scanner.hasMalware();
    }

    private boolean alreadyExistsHash(String name, String version) {
        return hashService.getHashOfPlugin(name, version).isPresent();
    }

    @Override
    public Boolean handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context) {
        String name = context.getStorage().poll(String.class);
        String version = context.getStorage().poll(String.class);
        if (alreadyExistsHash(name, version)) return false;

        File file = context.getStorage().poll(File.class);
        if (hasFileMalware(file)) return false;

        if (file == null || !file.exists()) {

            DiscordBot.log("Unknown error occurred when generating plugin hash.");
            return false;
        }
        if (name == null || version == null) {

            DiscordBot.log("Client failed to send valid details about the plugin signature.");
            return false;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] fileData = Files.readAllBytes(Path.of(file.getPath()));
            byte[] hash = messageDigest.digest(fileData);

            String checksum = Base64.encodeBase64String(hash);

            DiscordBot.log("Hash of file " + file.getName() + " (" + name + ", " + version + "): " + checksum);

            hashService.setHashOfPlugin(name, version, checksum);
         } catch (Exception ex) {

            ex.printStackTrace();
            DiscordBot.log("Error occurred when generating plugin hash: " + ex.getMessage());
            return false;
        }
        return true;
    }
}
