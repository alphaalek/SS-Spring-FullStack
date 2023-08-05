package me.alek.serversecurity.socket;

import me.alek.serversecurity.bot.SingletonBotInitializer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketController {

    private static final int PORT = 12345;

    private static ServerSocket socket;
    private static CountDownLatch initializingWaitingLatch = null;

    public static synchronized void setup() {
        if (initializingWaitingLatch != null) return;

        initializingWaitingLatch = new CountDownLatch(1);

        try {
            socket = new ServerSocket(PORT);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initializingWaitingLatch.countDown();

        ExecutorService virtualThreadExecutorService = Executors.newCachedThreadPool();

        while (!socket.isClosed()) {

            try {
                Socket client = socket.accept();
                client.setSoTimeout(3000);

                SingletonBotInitializer.log("Client connected to socket, " + client.getLocalAddress() + ":" + client.getLocalPort());

                SocketHandlerTask clientSocketHandler = new SocketHandlerTask(socket, client);

                virtualThreadExecutorService.execute(clientSocketHandler);

            } catch (Exception ex) {
                ex.printStackTrace();
                SingletonBotInitializer.log("Error occurred in socket client: " + ex.getMessage());
            }
        }
    }

    public static synchronized ServerSocket get() {
        if (socket == null) {

            if (initializingWaitingLatch == null) setup();
            try {
                if (initializingWaitingLatch != null) initializingWaitingLatch.await();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return socket;
    }

    public static boolean isInitialized() {
        return initializingWaitingLatch.getCount() == 0;
    }
}
