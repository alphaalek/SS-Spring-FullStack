package me.alek.serversecurity.socket;

import me.alek.serversecurity.bot.DiscordBot;
import me.alek.serversecurity.restapi.service.HashService;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SocketHandlerTask implements Runnable {

    private static final Map<Integer, SocketPipelineContext> sharedContextMap = new ConcurrentHashMap<>();

    private final Socket clientSocket;
    private final ServerSocket serverSocket;
    private final HashService hashService;
    private SocketPipelineContext context;

    public SocketHandlerTask(ServerSocket serverSocket, Socket clientSocket, HashService hashService) {
        this.serverSocket = serverSocket;
        this.clientSocket = clientSocket;
        this.hashService = hashService;
    }

    public static void removeId(int id) {
        synchronized (sharedContextMap) {
            sharedContextMap.remove(id);
        }
    }

    @Override
    public synchronized void run() {

        try {
            InputStream stream = clientSocket.getInputStream();

            int id = stream.read() << 8 + stream.read();

            // has a shared context, look for other transfers
            if (id != 0)
                context = sharedContextMap.computeIfAbsent(id, (d) -> new SocketPipelineContext(id, serverSocket, hashService));
            else
                context = new SocketPipelineContext(id, serverSocket, hashService);

            context.addClientToPipeline(clientSocket);

        } catch (Exception ex) {
            ex.printStackTrace();

            if (ex.getMessage().equals("Connection reset"))
                DiscordBot.log("Client socket did never send anything");
            else
                DiscordBot.log("Socket transfer error: " + ex.getMessage());

            try {
                clientSocket.close();

            } catch (Exception ex2) {
                DiscordBot.log("Error occurred when closing socket: " + ex2.getMessage());
            }
        }
    }
}
