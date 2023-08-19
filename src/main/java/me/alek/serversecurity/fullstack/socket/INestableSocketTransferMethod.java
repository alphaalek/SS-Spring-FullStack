package me.alek.serversecurity.fullstack.socket;

public interface INestableSocketTransferMethod<T> extends ISocketTransferMethod<T> {

    void putIntoSharedContext(SocketPipelineContext context);
}
