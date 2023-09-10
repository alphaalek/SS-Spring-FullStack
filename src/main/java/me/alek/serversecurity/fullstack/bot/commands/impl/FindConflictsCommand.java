package me.alek.serversecurity.fullstack.bot.commands.impl;

import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.bot.commands.DiscordCommandImpl;
import me.alek.serversecurity.fullstack.bot.commands.model.search.JarConflictContext;
import me.alek.serversecurity.fullstack.bot.commands.model.search.JarConflictSearch;
import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.model.PluginSignature;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class FindConflictsCommand implements DiscordCommandImpl {

    private final PluginService hashService;
    private final Map<String, ReactionResponseListener> listenerMap = new HashMap<>();

    public FindConflictsCommand(PluginService hashService) {
        this.hashService = hashService;

        DiscordBot.get().getJDA().addEventListener(new ReactionListener());
    }

    @FunctionalInterface
    private interface ReactionResponseListener {

        void onResponse(int position);
    }

    @Override
    public void perform(SlashCommandInteractionEvent event, Guild guild, Member member, User user) {
        for (PluginDBEntry entry: hashService.getAll()) {

            final List<JarWindow> whitelistedWindows = entry.getJarWindows().stream()
                    .filter(Predicate.not(JarWindow::isBlacklisted))
                    .toList();

            if (whitelistedWindows.size() < 2) continue;

            final EmbedBuilder builder = new EmbedBuilder();
            final PluginSignature signature = entry.getSignature();
            builder.setColor(Color.ORANGE);
            builder.setTitle("Found a whitelist conflict for signature " + signature.name() + " " + signature.version());
            builder.setFooter("Command used by " + event.getUser().getName());
            builder.setDescription("Pick a signature to be whitelisted");

            final JarConflictContext context = new JarConflictContext(whitelistedWindows);
            int i = 0;
            for (JarWindow jarWindow : whitelistedWindows) {
                final JarConflictSearch search = new JarConflictSearch(jarWindow, jarWindow.getFileName(), jarWindow.getResultData(), context);

                final StringBuilder valueStringBuilder = new StringBuilder();
                for (Map.Entry<String, Object> dataEntry : search.getDataSearchMap().entrySet()) {
                    valueStringBuilder.append(dataEntry.getKey() + ": ").append(dataEntry.getValue()).append("\n");
                }
                builder.addField("Signature " + ++i + ": " + jarWindow.getHash(), valueStringBuilder.toString(), true);
            }

            final int finalI = i;
            event.getHook().sendMessageEmbeds(builder.build()).queue(message -> {
                synchronized (listenerMap) {

                    for (int j = 0; j < finalI; j++) {
                        final String unicode = EmojiNumber.values()[j].unicode;
                        message.addReaction(Emoji.fromUnicode(unicode)).queue();

                        listenerMap.put(message.getId(), new ReactionResponseListener() {
                            @Override
                            public void onResponse(int position) {
                                if (position >= whitelistedWindows.size()) return;

                                for (JarWindow jarWindow : whitelistedWindows) {
                                    if (whitelistedWindows.get(position) == jarWindow) continue;

                                    hashService.addBlacklistedHash(signature.name(), signature.version(), jarWindow.getHash());
                                }
                                listenerMap.remove(message.getId());
                                event.getHook().sendMessage("Successfully blacklisted " + (whitelistedWindows.size() - 1) + " jar windows for signature " + signature.name() + " " + signature.version() + "." +
                                        " You are left with: " + whitelistedWindows.get(position).getHash()).queue();
                            }
                        });
                    }
                }
            });
            return;
        }
        event.getHook().sendMessage("No plugin entry with whitelist conflict was found. ").queue();
    }

    private enum EmojiNumber {

        ONE("\u0031\uFE0F\u20E3"),
        TWO("\u0032\uFE0F\u20E3"),
        THREE("\u0033\uFE0F\u20E3"),
        FOUR("\u0034\uFE0F\u20E3"),
        FIVE("\u0035\uFE0F\u20E3");

        private final String unicode;
        private static final Map<String, Integer> unicodeLookupPosition = new HashMap<>();

        EmojiNumber(String unicode) {
            this.unicode = unicode;
        }

        static {
            for (final EmojiNumber number : EmojiNumber.values())  {
                unicodeLookupPosition.put(number.unicode, number.ordinal());
            }
        }
    }

    public class ReactionListener extends ListenerAdapter {

        @Override
        public void onMessageReactionAdd(MessageReactionAddEvent event) {
            final User user = event.getUser();
            if (user != null && user.isBot()) return;

            final String messageId = event.getMessageId();
            final String unicode = event.getReaction().getEmoji().getName();
            synchronized (listenerMap) {
                if (!listenerMap.containsKey(messageId) || !EmojiNumber.unicodeLookupPosition.containsKey(unicode)) return;

                listenerMap.get(messageId).onResponse(EmojiNumber.unicodeLookupPosition.get(unicode));
            }
        }
    }

    @Override
    public String getName() {
        return "conflicts";
    }

    @Override
    public String getDescription() {
        return "Find plugin entries with jar windows that needs to be checked, if they should be blacklisted or not.";
    }

    @Override
    public List<Permission> getPermissions() {
        return Collections.singletonList(Permission.ADMINISTRATOR);
    }
}
