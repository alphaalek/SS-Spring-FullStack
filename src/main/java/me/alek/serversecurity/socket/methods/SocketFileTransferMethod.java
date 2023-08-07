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
        try {
            String fileName = context.getStorage().poll(String.class);

            file = new File("tmp/" + fileName);
            if (file.exists()) return file;

            DataInputStream dataStream = new DataInputStream(stream);
            FileOutputStream fileOutputStream = new FileOutputStream("tmp/" + fileName);

            byte[] buffer = new byte[4096];
            int totalBytes = 0;
            int bytes;
            while ((bytes = dataStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytes);
                totalBytes += bytes;
            }

            fileOutputStream.close();
            dataStream.close();

            DiscordBot.log("Successfully transfered file " + fileName + " with " + totalBytes + " total bytes");
        }
        catch (Exception ex) {

            if (!context.hasClosed()) {
                ex.printStackTrace();
                DiscordBot.log("Error occurred in file transfer: " + ex.getMessage());
            }
        }
        return file;
    }

    @Override
    public void putIntoSharedContext(SocketPipelineContext context) {
        context.getStorage().put(File.class, file);
    }
}
