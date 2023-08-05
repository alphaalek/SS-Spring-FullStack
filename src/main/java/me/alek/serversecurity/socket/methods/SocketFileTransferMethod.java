package me.alek.serversecurity.socket.methods;

import me.alek.serversecurity.bot.SingletonBotInitializer;
import me.alek.serversecurity.socket.ISocketTransferMethod;
import me.alek.serversecurity.socket.SocketContext;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;

public class SocketFileTransferMethod implements ISocketTransferMethod {


    @Override
    public void handle(ServerSocket serverSocket, InputStream stream, SocketContext context) {
        try {
            String fileName = context.getStorage().poll(String.class);

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

            SingletonBotInitializer.log("Succesfully transfered file " + fileName + " with " + totalBytes + " total bytes");
        }
        catch (Exception ex) {

            ex.printStackTrace();
            SingletonBotInitializer.log("Error occurred in file transfer: " + ex.getMessage());
        }
    }
}
