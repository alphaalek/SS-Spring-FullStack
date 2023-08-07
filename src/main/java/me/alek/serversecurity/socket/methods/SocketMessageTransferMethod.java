package me.alek.serversecurity.socket.methods;

import me.alek.serversecurity.bot.DiscordBot;
import me.alek.serversecurity.socket.INestableSocketTransferMethod;
import me.alek.serversecurity.socket.SocketPipelineContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class SocketMessageTransferMethod implements INestableSocketTransferMethod<List<String>> {

    private final List<String> messages = new ArrayList<>();

    @Override
    public List<String> handle(ServerSocket serverSocket, InputStream stream, SocketPipelineContext context) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        StringBuilder builder = new StringBuilder();
        try {
            char character;
            while ((character = (char) reader.read()) != Character.MIN_VALUE) {

                context.sendKeepAlive();

                if (character == '\n') {
                    messages.add(builder.toString());
                    builder = new StringBuilder();
                    continue;
                }
                builder.append(character);
            }
        } catch (Exception ex) {
            if (!context.hasClosed()) {
                ex.printStackTrace();
                DiscordBot.log("Error occurred in message transfer: " + ex.getMessage());
            }

            if (!builder.isEmpty()) messages.add(builder.toString());
        }

        DiscordBot.log("Successfully transfered " + messages.size() + " messages");

        return messages;
    }

    @Override
    public void putIntoSharedContext(SocketPipelineContext context) {
        for (String message : messages) {
            context.getStorage().putString(message);
        }
    }
}
