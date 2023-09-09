package me.alek.serversecurity.fullstack.bot.commands.impl;

import me.alek.serversecurity.fullstack.bot.commands.DiscordCommandImpl;
import me.alek.serversecurity.fullstack.search.JarConflictContext;
import me.alek.serversecurity.fullstack.search.JarConflictSearch;
import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.model.PluginSignature;
import me.alek.serversecurity.fullstack.restapi.service.HashService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class FindConflictsCommand implements DiscordCommandImpl {

    private final HashService hashService;

    public FindConflictsCommand(HashService hashService) {
        this.hashService = hashService;
    }

    @Override
    public void perform(SlashCommandInteractionEvent event, Guild guild, Member member, User user) {
        for (PluginDBEntry entry: hashService.getAll()) {

            List<JarWindow> whitelistedWindows = entry.getJarWindows().stream()
                    .filter(Predicate.not(JarWindow::isBlacklisted))
                    .toList();

            if (whitelistedWindows.size() < 2) continue;

            EmbedBuilder builder = new EmbedBuilder();

            final PluginSignature signature = entry.getSignature();
            builder.setColor(Color.ORANGE);
            builder.setTitle("Found a whitelist conflict for signature " + signature.name() + " " + signature.version());
            builder.setFooter("Command used by " + event.getUser().getName());
            builder.setDescription("Pick a signature to be whitelisted");

            final JarConflictContext context = new JarConflictContext(whitelistedWindows);
            int i = 0;
            for (JarWindow jarWindow : whitelistedWindows) {
                JarConflictSearch search = new JarConflictSearch(jarWindow, jarWindow.getFileName(), jarWindow.getResultData(), context);

                StringBuilder valueStringBuilder = new StringBuilder();
                for (Map.Entry<String, Object> dataEntry : search.getDataSearchMap().entrySet()) {
                    valueStringBuilder.append(dataEntry.getKey() + ": ").append(dataEntry.getValue()).append("\n");
                }
                builder.addField("Signature " + ++i + ": " + jarWindow.getHash(), valueStringBuilder.toString(), true);
            }

            event.getHook().sendMessageEmbeds(builder.build()).queue();
            return;
        }
        event.getHook().sendMessage("No plugin entry with whitelist conflict was found. ").queue();
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
