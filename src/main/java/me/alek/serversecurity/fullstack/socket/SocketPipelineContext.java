package me.alek.serversecurity.fullstack.socket;

import me.alek.serversecurity.fullstack.socket.tasks.SocketHandlerTask;
import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SocketPipelineContext {

    private static class SharedStorageHolder {

        private volatile SharedStorage sharedStorage;

    }

    private final SharedStorageHolder holder = new SharedStorageHolder();

    public SharedStorage getStorage() {

        synchronized (holder) {
            if (holder.sharedStorage == null) holder.sharedStorage = new SharedStorage();
        }

        return holder.sharedStorage;
    }

    private static final long KEEP_ALIVE_TIMESPAN = 2000L;
    private static final long GARBAGE_COLLECT_SCHEDULED_CHECK = 15000L;

    private final int id;
    private final ServerSocket serverSocket;
    private final PluginService hashService;

    private final LinkedBlockingQueue<Socket> clientSockets = new LinkedBlockingQueue<>();
    private final ExecutorService singleExecutorService = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    private final AtomicReference<Socket> currentHandledSocket = new AtomicReference<>();
    private final AtomicReference<ISocketTransferMethod<?>> currentHandledMethod = new AtomicReference<>();

    private final AtomicBoolean hasClosed = new AtomicBoolean();
    private final AtomicBoolean hasShutDown = new AtomicBoolean();
    private final AtomicBoolean hasSentKeepAlive = new AtomicBoolean();
    private final AtomicBoolean hasSentConnected = new AtomicBoolean();
    private final AtomicBoolean shouldGarbageCollect = new AtomicBoolean();

    public SocketPipelineContext(int id, ServerSocket serverSocket, PluginService hashService) {
        this.id = id;
        this.serverSocket = serverSocket;
        this.hashService = hashService;

        startKeepAliveHandlerTask();
        startScheduledGarbageCollectionTask();
        startHandlingInputWorkerThread();
    }

    public ServerSocket getServerSocket () {
        return serverSocket;
    }

    public Socket getCurrentHandledSocket() { return currentHandledSocket.get(); }

    public LinkedBlockingQueue<Socket> getClientSockets() {
        return clientSockets;
    }

    public boolean hasBeenClosedByKeepAlive() {
        boolean hasClosed = this.hasClosed.get();
        this.hasClosed.compareAndSet(true, false);
        return hasClosed;
    }

    public void addClientToPipeline(Socket clientSocket) {
        clientSockets.add(clientSocket);

        if (hasSentConnected.compareAndSet(false, true)) {

            DiscordBot.get().log("Client connected to socket: " + clientSocket.getInetAddress() +
                    ", Initializing socket pipeline with ID **" + id + "**");
        }
    }

    public void sendKeepAlive() {
        shouldGarbageCollect.compareAndSet(true, false);
        hasSentKeepAlive.compareAndSet(false, true);
    }

    public synchronized void close() {
        if (hasShutDown.get()) return;

        // close all waiting sockets
        for (Socket socket : clientSockets) {
            try {
                socket.close();
            } catch (Exception ex) {
                DiscordBot.get().log("**" + id + "**: (Shutdown) Error occurred when shutting down the pipeline: " + ex.getMessage());

                ex.printStackTrace();
            }
        }
        // unregister the id of this pipeline context
        SocketHandlerTask.removeId(id);

        // shutdown the threads running
        singleExecutorService.shutdownNow();
        scheduledExecutorService.shutdownNow();

        hasShutDown.set(true);
        DiscordBot.get().log("**" + id + "**: (Shutdown) Shutting down the pipeline.");
    }

    private void startHandlingInputWorkerThread() {
        singleExecutorService.execute(() -> {

            while (true) {

                if (hasShutDown.get()) return;
                try {
                    Socket clientSocket = clientSockets.take();
                    if (clientSocket.isClosed()) continue;

                    currentHandledSocket.set(clientSocket);

                    CompletableFuture<?> future = handleInputMethod(clientSocket);

                    future.get();
                } catch (Exception ex) {
                }
            }
        });
    }

    private void startKeepAliveHandlerTask() {
        final CompletableFuture<ScheduledFuture<?>> scheduledFuture = new CompletableFuture<>();
        final Runnable closeHeartbeat = () -> {
            if (!scheduledFuture.isDone()) return;
            try {
                scheduledFuture.get().cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        };
        final ScheduledFuture<?> scheduledTask = scheduledExecutorService.scheduleAtFixedRate(() -> {
            if (hasShutDown.get()) closeHeartbeat.run();

            Socket currentHandledSocket = this.currentHandledSocket.get();
            if (currentHandledSocket == null || currentHandledSocket.isClosed()) {

                this.currentHandledSocket.set(null);
                return;
            }
            System.out.println("checking keep alive + " + currentHandledSocket + " " + currentHandledMethod.get() + " " + hasSentKeepAlive.get());

            if (currentHandledMethod.get() instanceof IWaitableSocketTransferMethod<?>) return;

            if (!hasSentKeepAlive.get()) {

                try {
                    DiscordBot.get().log("**" + id + "**: (Keep Alive) Closing socket because no keep alive packet was sent.");
                    hasClosed.set(true);
                    currentHandledSocket.close();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    DiscordBot.get().log("**" + id + "**: (Keep Alive) Error occurred when closing socket: " + ex.getMessage());
                }
            }
            hasSentKeepAlive.compareAndSet(true, false);

        }, 0, KEEP_ALIVE_TIMESPAN, TimeUnit.MILLISECONDS);

        scheduledFuture.complete(scheduledTask);
    }

    private void startScheduledGarbageCollectionTask() {
        final ScheduledFuture<?>[] scheduledTask = new ScheduledFuture<?>[1];

        scheduledTask[0] = scheduledExecutorService.scheduleAtFixedRate(() -> {

            if (hasShutDown.get()) scheduledTask[0].cancel(true);

            if (shouldGarbageCollect.get() && clientSockets.isEmpty()) {
                close();
                scheduledTask[0].cancel(true);
            }
            shouldGarbageCollect.set(true);

        }, 0, GARBAGE_COLLECT_SCHEDULED_CHECK, TimeUnit.MILLISECONDS);
    }

    private <T> CompletableFuture<T> handleInputMethod(Socket clientSocket) {
        try {
            shouldGarbageCollect.set(false);
            InputStream stream = clientSocket.getInputStream();

            SocketTransferFactory factory = SocketTransferFactory.getById(stream.read());
            if (factory == SocketTransferFactory.UNKNOWN) {
                DiscordBot.get().log("**" + id + "**: (Input Handle) Unknown socket transfer method");

                clientSocket.close();
                return CompletableFuture.completedFuture(null);
            }
            ISocketTransferMethod<T> method = (ISocketTransferMethod<T>) factory.createMethod(hashService);
            System.out.println(id + ": " + method);
            currentHandledMethod.set(method);

            return CompletableFuture.supplyAsync(() -> {
                T value = method.handle(serverSocket, stream, this);

                if (method instanceof INestableSocketTransferMethod<?> nestableMethod)
                    nestableMethod.putIntoSharedContext(this);

                return value;
            });
        } catch (Exception ex) {

            ex.printStackTrace();
            DiscordBot.get().log("**" + id + "**: (Input Handle) Socket transfer error: " + ex.getMessage());

            return CompletableFuture.completedFuture(null);
        }
    }

    public int getId() {
        return id;
    }

    public static class SharedStorage {

        private final Map<Class<?>, ContainerStructure<?>> dataContextStorageMap = new ConcurrentHashMap<>();

        public void putBoolean(boolean bool) {
            put(Boolean.class, bool);
        }

        public void putString(String string) {
            put(String.class, string);
        }

        public void putChar(char character) {
            put(Character.class, character);
        }

        public void putInt(int integer) {
            put(Integer.class, integer);
        }

        public void putFloat(float floatValue) {
            put(Float.class, floatValue);
        }

        public void putByte(byte byteValue) {
            put(Byte.class, byteValue);
        }

        public boolean getBoolean(int index) {
            Boolean bool = get(Boolean.class, index);
            if (bool == null) return false;

            return bool;
        }

        public String getString(int index) {
            return get(String.class, index);
        }

        public char getChar(int index) {
            Character character = get(Character.class, index);
            if (character == null) return ' ';

            return character;
        }

        public int getInt(int index) {
            Integer integer = get(Integer.class, index);
            if (integer == null) return 0;

            return integer;
        }

        public float getFloat(int index) {
            Float floatValue = get(Float.class, index);
            if (floatValue == null) return 0f;

            return floatValue;
        }

        public byte getByte(int index) {
            Byte byteValue = get(Byte.class, index);
            if (byteValue == null) return 0;

            return byteValue;
        }

        public synchronized <T> void put(Class<T> clazz, T value) {
            ContainerStructure<T> structure = getStructure(clazz);
            if (structure == null) {
                dataContextStorageMap.put(clazz, new ContainerStructure<>());
                put(clazz, value);
                return;
            }
            structure.add(value);
        }

        public synchronized <T> T get(Class<T> clazz, int index) {
            ContainerStructure<T> structure = getStructure(clazz);
            if (structure == null) return null;

            return structure.get(index);
        }

        public <T> T poll(Class<T> clazz) {
            return take(clazz, 0);
        }

        public synchronized <T> T take(Class<T> clazz, int index) {
            ContainerStructure<T> structure = getStructure(clazz);
            if (structure == null) return null;

            T value = structure.get(index);
            structure.remove(index);
            return value;
        }

        public <T> ContainerStructure<T> getStructure(Class<T> clazz) {
            return (ContainerStructure<T>) dataContextStorageMap.get(clazz);
        }

        public final static class ContainerStructure<T> {

            private final List<T> values = new ArrayList<>();

            public void add(T value) {
                values.add(value);
            }

            public T get(int index) {
                if (!has(index)) {
                    return null;
                }
                return values.get(index);
            }

            public void remove(int index) {
                values.remove(index);
            }

            public boolean has(int index) {
                return values.size() > index;
            }

            public List<T> getValues() {
                return values;
            }
        }
    }
}
