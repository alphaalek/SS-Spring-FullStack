package me.alek.serversecurity.socket;

import me.alek.serversecurity.bot.DiscordBot;
import me.alek.serversecurity.restapi.service.HashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class SocketController {

    private static final int PORT = 12345;

    private ServerSocket socket;
    private static CountDownLatch initializingWaitingLatch = null;

    private final HashService hashService;

    @Autowired
    public SocketController(HashService hashService) {
        this.hashService = hashService;

        setup();
    }

    public void setup() {
        if (initializingWaitingLatch != null) return;

        initializingWaitingLatch = new CountDownLatch(1);
        try {
            socket = new ServerSocket(PORT);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initializingWaitingLatch.countDown();

        new Thread(() -> {

            ExecutorService virtualThreadExecutorService = Executors.newCachedThreadPool();
            while (!socket.isClosed()) {

                try {
                    Socket client = socket.accept();

                    SocketHandlerTask clientSocketHandler = new SocketHandlerTask(socket, client, hashService);
                    virtualThreadExecutorService.execute(clientSocketHandler);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    DiscordBot.log("Error occurred in socket client: " + ex.getMessage());
                }
            }
        }).start();
    }

    public synchronized ServerSocket get() {
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
