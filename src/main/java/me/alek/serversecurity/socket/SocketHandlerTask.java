package me.alek.serversecurity.socket;

import me.alek.serversecurity.bot.SingletonBotInitializer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SocketHandlerTask implements Runnable {

    private static final Map<Integer, SocketContext> sharedContextMap = new HashMap<>();

    private final Socket clientSocket;
    private final ServerSocket serverSocket;

    public SocketHandlerTask(ServerSocket serverSocket, Socket clientSocket) {
        this.serverSocket = serverSocket;
        this.clientSocket = clientSocket;
    }


    @Override
    public void run() {
        while (!clientSocket.isClosed()) {

            try {
                InputStream stream = clientSocket.getInputStream();

                int sharedContextID = stream.read() << 8 + stream.read();
                SocketContext context = null;

                // has a shared context, look for other transfers
                if (sharedContextID != 0) {

                    if (sharedContextMap.containsKey(sharedContextID)) {
                        context = sharedContextMap.get(sharedContextID);
                        context.setClientSocket(clientSocket);
                    }
                }
                if (context == null) context = new SocketContext(serverSocket, clientSocket);
                if (sharedContextID != 0 && !sharedContextMap.containsKey(sharedContextID)) sharedContextMap.put(sharedContextID, context);

                context.handleInputMethod();
            } catch (Exception ex) {
                ex.printStackTrace();

                if (ex.getMessage().equals("Connection reset"))
                    SingletonBotInitializer.log("Client socket did never send anything");
                else
                    SingletonBotInitializer.log("Socket transfer error: " + ex.getMessage());

                break;
            }
        }
    }
}
