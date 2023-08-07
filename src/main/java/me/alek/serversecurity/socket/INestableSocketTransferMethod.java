package me.alek.serversecurity.socket;

public interface INestableSocketTransferMethod<T> extends ISocketTransferMethod<T> {

    void putIntoSharedContext(SocketPipelineContext context);
}
