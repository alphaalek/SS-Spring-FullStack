package me.alek.serversecurity.fullstack.socket;

import java.io.InputStream;
import java.net.ServerSocket;

public interface ISocketTransferMethod<T> {

    T handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context);

}
