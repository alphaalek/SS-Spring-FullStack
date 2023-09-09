package me.alek.serversecurity.fullstack.socket.methods;

import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.socket.INestableSocketTransferMethod;
import me.alek.serversecurity.fullstack.socket.SocketPipelineContext;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SocketFileTransferMethod implements INestableSocketTransferMethod<File> {

    private File file = null;
    private String fileName;

    @Override
    public File handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context) {
        String name = context.getStorage().getString(0);
        String version = context.getStorage().getString(1);

        if (name == null || version == null) return null;

        long size = 0;
        int totalBytes = 0;
        try {
            DataInputStream dataStream = new DataInputStream(stream);
            size = dataStream.readLong();
            file = new File("tmp/" + name + "-" + version + ".jar");

            // make a unique name for this file but still keep it related to the same name and version for other hashes of this plugin version
            if (file.exists()) {
                int uniqueCounter = 2;
                while ((file = new File("tmp/" + name + "-" + version + "-" + uniqueCounter++ + ".jar")).exists()) {}
            }
            fileName = file.getName();

            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            // store the file transfered over the socket
            FileOutputStream fileOutputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = dataStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytes);
                context.sendKeepAlive();
                totalBytes += bytes;
            }
            // close the streams to finish the transfer
            fileOutputStream.close();
            dataStream.close();
        }
        catch (Exception ex) {
            if (!context.hasBeenClosedByKeepAlive()) {
                ex.printStackTrace();
                DiscordBot.get().log("**" + context.getId() + "**: (File Method) Error occurred in file transfer: " + ex.getMessage());
            }
        }
        if (file != null) {
            if (file.length() != size) {
                try {
                    DiscordBot.get().log("**" + context.getId() + "**: (File Method) Deleting file " + fileName + " because the size did not match. (" + size + ": " + file.length() + ")");
                    Files.deleteIfExists(Paths.get("tmp/" + fileName));

                } catch (Exception ex) {
                    ex.printStackTrace();
                    DiscordBot.get().log("**" + context.getId() + "**: (File Method) Error occurred when deleting file " + fileName + ": " + ex.getMessage());
                }
            }
            else DiscordBot.get().log("**" + context.getId() + "**: (File Method) Successfully transfered file " + fileName + " with " + totalBytes + " total bytes");
        }

        return file;
    }

    @Override
    public void putIntoSharedContext(SocketPipelineContext context) {
        context.getStorage().put(File.class, file);
    }
}
