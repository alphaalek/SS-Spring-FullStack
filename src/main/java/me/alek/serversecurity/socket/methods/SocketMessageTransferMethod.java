package me.alek.serversecurity.socket.methods;

import me.alek.serversecurity.bot.SingletonBotInitializer;
import me.alek.serversecurity.socket.INestableSocketTransferMethod;
import me.alek.serversecurity.socket.SocketContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class SocketMessageTransferMethod implements INestableSocketTransferMethod {

    private final List<String> messages = new ArrayList<>();

    @Override
    public void handle(ServerSocket serverSocket, InputStream stream, SocketContext context) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        try {
            String msg;
            while ((msg = reader.readLine()) != null) {
                messages.add(msg);
            }
            SingletonBotInitializer.log("Succesfully transfered " + messages.size() + " messages");

        } catch (Exception ex) {
            ex.printStackTrace();
            SingletonBotInitializer.log("Error occurred in message transfer: " + ex.getMessage());
        }
        try {
            if (!context.getClientSocket().isClosed()) reader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            SingletonBotInitializer.log("Error occurred when closing stream.");
        }
    }

    @Override
    public void putIntoSharedContext(SocketContext context) {
        for (String message : messages) {
            context.getStorage().putString(message);
        }
    }
}
