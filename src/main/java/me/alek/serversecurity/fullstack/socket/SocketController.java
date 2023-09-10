package me.alek.serversecurity.fullstack.socket;

import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.socket.tasks.SocketHandlerTask;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final PluginService hashService;

    @Autowired
    public SocketController(PluginService hashService) {
        this.hashService = hashService;

        setup();
    }

    public synchronized void setup() {
        if (initializingWaitingLatch != null) return;

        initializingWaitingLatch = new CountDownLatch(1);
        try {
            socket = new ServerSocket(PORT);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initializingWaitingLatch.countDown();

        new Thread(() -> {
            ExecutorService executorService = Executors.newCachedThreadPool();
            while (!socket.isClosed()) {

                try {
                    Socket client = socket.accept();

                    SocketHandlerTask clientSocketHandler = new SocketHandlerTask(socket, client, hashService);
                    executorService.execute(clientSocketHandler);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    DiscordBot.get().log("Error occurred in socket client: " + ex.getMessage());
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
