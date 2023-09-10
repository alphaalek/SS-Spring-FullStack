package me.alek.serversecurity.fullstack.socket.tasks;

import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import me.alek.serversecurity.fullstack.socket.SocketPipelineContext;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SocketHandlerTask implements Runnable {

    private static final Map<Integer, SocketPipelineContext> sharedContextMap = new ConcurrentHashMap<>();

    private final Socket clientSocket;
    private final ServerSocket serverSocket;
    private final PluginService hashService;
    private SocketPipelineContext context;

    public SocketHandlerTask(ServerSocket serverSocket, Socket clientSocket, PluginService hashService) {
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
            DataInputStream dataStream = new DataInputStream(stream);

            int id = (int) dataStream.readLong();

            // check for shared context
            if (id != 0)
                // has a shared context, look for other transfers
                context = sharedContextMap.computeIfAbsent(id, (d) -> new SocketPipelineContext(id, serverSocket, hashService));
            else
                context = new SocketPipelineContext(id, serverSocket, hashService);

            context.addClientToPipeline(clientSocket);

        } catch (Exception ex) {
            ex.printStackTrace();

            if (ex.getMessage().equals("Connection reset"))
                DiscordBot.get().log("**" + context.getId() + "**: Client socket did never send anything");
            else
                DiscordBot.get().log("**" + context.getId() + "**: Socket transfer error: " + ex.getMessage());

            try {
                clientSocket.close();

            } catch (Exception ex2) {
                DiscordBot.get().log("**" + context.getId() + "**: Error occurred when closing socket: " + ex2.getMessage());
            }
        }
    }
}
