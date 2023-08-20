package me.alek.serversecurity.fullstack.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class DiscordBot {

    private final static String TOKEN = "";

    private final static long DEFAULT_GUILD_ID = 1114316101136953535L;

    private final static long PIPELINE_LOGGING_CHANNEL_ID = 1138150877325168660L;
    private static final long RESTAPI_LOGGING_CHANNEL_ID = 1142742221544759378L;

    private static JDA jda;
    private static CountDownLatch initializingWaitingLatch = null;
    private static final Semaphore loggingSemaphore = new Semaphore(1);

    public static synchronized void setup() {
        if (initializingWaitingLatch != null) return;

        initializingWaitingLatch = new CountDownLatch(1);

        try {
            jda = JDABuilder.createDefault(TOKEN)
                    .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .setBulkDeleteSplittingEnabled(false)
                    .setActivity(Activity.watching("servers"))
                    .build();

            jda.awaitReady();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        initializingWaitingLatch.countDown();
    }

    public static synchronized JDA get() {
        if (jda == null) {

            if (initializingWaitingLatch == null) setup();
            try {
                initializingWaitingLatch.await();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return jda;
    }

    public static boolean isInitialized() {
        return initializingWaitingLatch.getCount() == 0;
    }

    public static Optional<TextChannel> getTextChannel(LoggingMethod method) {
        Optional<Guild> guildOptional = Optional.ofNullable(get().getGuildById(DEFAULT_GUILD_ID));

        AtomicLong id = new AtomicLong();
        switch (method) {
            case PIPELINE -> id.set(PIPELINE_LOGGING_CHANNEL_ID);
            case RESTAPI -> id.set(RESTAPI_LOGGING_CHANNEL_ID);
        }

        return guildOptional.map(guild -> guild.getTextChannelById(id.get()));
    }

    public static void log(String message) { log(LoggingMethod.PIPELINE, message); }

    public static void log(LoggingMethod method, String message) {
        try {
            // try acquiring the semaphore, and maybe wait till the bot is ready to log the message
            //loggingSemaphore.acquire();

            Optional<TextChannel> optionalTextChannel = getTextChannel(method);

            // send the message if the text channel was found
            optionalTextChannel.ifPresent(textChannel -> textChannel.sendMessage(message).complete());

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // just release the barrier and let other threads send their logging message
            //loggingSemaphore.release();
        }
    }
}
