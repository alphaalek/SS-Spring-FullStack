package me.alek.serversecurity.fullstack.socket.methods;

import me.alek.serversecurity.fullstack.socket.ISocketTransferMethod;
import me.alek.serversecurity.fullstack.socket.SocketPipelineContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;

public class CloseContextMethod implements ISocketTransferMethod<Boolean> {

    @Override
    public Boolean handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context) {
        context.close();

        try {
            OutputStream outputStream = context.getCurrentHandledSocket().getOutputStream();

            outputStream.write(1);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }
}
