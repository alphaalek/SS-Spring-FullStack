package me.alek.serversecurity.socket;

import java.io.InputStream;
import java.net.ServerSocket;

public interface ISocketTransferMethod<T> {

    public T handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context);

}
