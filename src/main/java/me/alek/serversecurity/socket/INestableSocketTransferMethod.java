package me.alek.serversecurity.socket;

public interface INestableSocketTransferMethod extends ISocketTransferMethod {

    void putIntoSharedContext(SocketContext context);
}
