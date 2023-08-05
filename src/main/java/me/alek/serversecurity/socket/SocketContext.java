package me.alek.serversecurity.socket;

import me.alek.serversecurity.bot.SingletonBotInitializer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class SocketContext {

    private static SharedStorage sharedStorage;

    public synchronized SharedStorage getStorage() {
        if (sharedStorage == null)
            sharedStorage = new SharedStorage();

        return sharedStorage;
    }

    private final ServerSocket serverSocket;
    private Socket clientSocket;
    private CountDownLatch waitingLatch = null;

    public SocketContext(ServerSocket serverSocket, Socket clientSocket) {
        this.serverSocket = serverSocket;
        this.clientSocket = clientSocket;
    }

    public ServerSocket getServerSocket () {
        return serverSocket;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void setClientSocket(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void handleInputMethod() {
        try {
            synchronized (this) {
                if (waitingLatch != null) waitingLatch.await();

                waitingLatch = new CountDownLatch(1);
            }

            try {
                InputStream stream = clientSocket.getInputStream();

                SocketTransferFactory factory = SocketTransferFactory.getById(stream.read());
                if (factory == SocketTransferFactory.UNKNOWN) {
                    SingletonBotInitializer.log("Unknown socket transfer method");
                    return;
                }
                ISocketTransferMethod method = factory.createMethod();

                method.handle(serverSocket, clientSocket.getInputStream(), this);

                if (method instanceof INestableSocketTransferMethod nestableMethod)
                    nestableMethod.putIntoSharedContext(this);

                stream.close();
            }
            finally {
                waitingLatch.countDown();
            }
        } catch (Exception ex) {

             ex.printStackTrace();
             SingletonBotInitializer.log("Socket transfer error: " + ex.getMessage());
        }
    }

    public void awaitSharedContextInsertion() {
        try {
            if (waitingLatch != null) waitingLatch.await();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

        public synchronized <T> T poll(Class<T> clazz) {
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
