package me.alek.serversecurity.socket.methods;

import me.alek.serversecurity.bot.DiscordBot;
import me.alek.serversecurity.socket.INestableSocketTransferMethod;
import me.alek.serversecurity.socket.SocketPipelineContext;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;

public class SocketFileTransferMethod implements INestableSocketTransferMethod<File> {

    private File file = null;

    @Override
    public File handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context) {

        String fileName = context.getStorage().poll(String.class);
        int totalBytes = 0;
        try {
            file = new File("tmp/" + fileName);
            if (file.exists()) {
                DiscordBot.log("A file named " + file.getName() + " is already existing.");
                return file;
            }
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            DataInputStream dataStream = new DataInputStream(stream);
            FileOutputStream fileOutputStream = new FileOutputStream("tmp/" + fileName);

            byte[] buffer = new byte[4096];
            int bytes;
            while ((bytes = dataStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytes);
                totalBytes += bytes;
            }

            fileOutputStream.close();
            dataStream.close();
        }
        catch (Exception ex) {

            if (!context.hasClosed()) {
                ex.printStackTrace();
                DiscordBot.log("Error occurred in file transfer: " + ex.getMessage());
            }
        }
        System.out.println("hey");
        DiscordBot.log("Successfully transfered file " + fileName + " with " + totalBytes + " total bytes");

        return file;
    }

    @Override
    public void putIntoSharedContext(SocketPipelineContext context) {
        context.getStorage().put(File.class, file);
    }
}
