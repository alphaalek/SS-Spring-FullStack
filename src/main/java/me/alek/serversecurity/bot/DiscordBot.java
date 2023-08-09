package me.alek.serversecurity.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class DiscordBot {

    private final static String TOKEN = "MTEzNzAxMDgzMTQ1MjgxOTU0Ng.G5C1a6.nDSnOuzsMWS8KhHYrcIjMYgZFTB0v4K6sSu1nk";

    private final static long DEFAULT_GUILD_ID = 1114316101136953535L;
    private final static long DEFAULT_CHANNEL_ID = 1138150877325168660L;

    private static JDA jda;
    private static CountDownLatch initializingWaitingLatch = null;

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

    public static Optional<TextChannel> getDefaultTextChannel() {
        return Optional.ofNullable(get().getGuildById(DEFAULT_GUILD_ID)).map(guild -> guild.getTextChannelById(DEFAULT_CHANNEL_ID));
    }

    public static void log(String message) {
        Optional<TextChannel> optionalTextChannel = getDefaultTextChannel();
        if (optionalTextChannel.isEmpty()) return;

        optionalTextChannel.get().sendMessage(message).complete();
    }
}
