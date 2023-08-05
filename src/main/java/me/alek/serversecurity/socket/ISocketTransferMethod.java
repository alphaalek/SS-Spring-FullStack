package me.alek.serversecurity.socket;

import java.io.InputStream;
import java.net.ServerSocket;

public interface ISocketTransferMethod {

    public void handle(ServerSocket serverSocket, InputStream stream, SocketContext context);

}
