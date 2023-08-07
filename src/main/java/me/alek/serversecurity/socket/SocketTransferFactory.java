package me.alek.serversecurity.socket;

import me.alek.serversecurity.restapi.service.HashService;
import me.alek.serversecurity.socket.methods.PluginHashGeneratorMethod;
import me.alek.serversecurity.socket.methods.SocketFileTransferMethod;
import me.alek.serversecurity.socket.methods.SocketMessageTransferMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public enum SocketTransferFactory {

    FILE_TRANSFER(1, SocketFileTransferMethod.class),
    MESSAGE_TRANSFER(2, SocketMessageTransferMethod.class),
    PLUGIN_HASH_GENERATOR(3,PluginHashGeneratorMethod.class),
    UNKNOWN(-1, null);

    private final int id;
    private final Class<? extends ISocketTransferMethod<?>> clazz;
    private static final Map<Integer, SocketTransferFactory> methodLookup = new HashMap<>();

    static {
        for (SocketTransferFactory method : SocketTransferFactory.values()) {
            methodLookup.put(method.id, method);
        }
    }

    SocketTransferFactory(int id, Class<? extends ISocketTransferMethod<?>> clazz) {
        this.id = id;
        this.clazz = clazz;
    }

    public int getId() {
        return id;
    }

    public Class<? extends ISocketTransferMethod<?>> getClazz() {
        return clazz;
    }

    public ISocketTransferMethod<?> createMethod(HashService hashService) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        if (IStereotypedBeanSocketTransferMethod.class.isAssignableFrom(clazz)) {
            return clazz.getDeclaredConstructor(HashService.class).newInstance(hashService);
        }

        return clazz.getDeclaredConstructor().newInstance();
    }

    public static SocketTransferFactory getById(int id) {
        if (methodLookup.containsKey(id)) return methodLookup.get(id);

        return UNKNOWN;
    }

}
