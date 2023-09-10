package me.alek.serversecurity.fullstack.bot.commands.impl;

import me.alek.serversecurity.fullstack.bot.commands.OptionableDiscordCommandImpl;
import me.alek.serversecurity.fullstack.bot.commands.model.OptionElement;
import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.*;
import java.util.List;

public class InfoCommand implements OptionableDiscordCommandImpl {

    private final PluginService pluginService;

    public InfoCommand(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    @Override
    public void perform(SlashCommandInteractionEvent event, Guild guild, Member member, User user) {
        final String name = event.getOption("name").getAsString();
        final String version = event.getOption("version") == null ? null : event.getOption("version").getAsString();

        final EmbedBuilder builder = new EmbedBuilder();

        builder.setColor(Color.ORANGE);
        builder.setFooter("Command used by " + event.getUser().getName());

        if (version != null) {
            builder.setTitle("Found following jar windows for plugin signature " + name + " " + version);

            PluginDBEntry entry = pluginService.getLiteralPlugin(name, version);
            if (entry.getJarWindows().isEmpty()) {
                event.getHook().sendMessage("Didn't find any jar windows for plugin signature " + name + " " + version).queue();
                return;
            }
            int i = 1;
            for (JarWindow jarWindow : entry.getJarWindows()) {
                builder.addField("Jar Window " + i++, jarWindow.getHash() + ", " + jarWindow.getResultData() + ", " + jarWindow.getFileName(), false);
            }

        } else {
            builder.setTitle("Found following db entries for plugin name " + name);

            final Map<String, Integer> versionInfo = new HashMap<>();
            for (PluginDBEntry entry : pluginService.getAll()) {
                if (entry.getSignature().name().equalsIgnoreCase(name)) {
                    versionInfo.put(entry.getSignature().version(), versionInfo.getOrDefault(entry.getSignature().version(), 0) + entry.getJarWindows().size());
                }
            }
            if (versionInfo.isEmpty()) {
                event.getHook().sendMessage("Didn't find any db entries for plugin name " + name).queue();
                return;
            }
            for (Map.Entry<String, Integer> entry : versionInfo.entrySet()) {
                builder.addField(entry.getKey(), entry.getValue() + " jar windows", false);
            }
            builder.setDescription("Use /info " + name + " <version> to get info of specific signature.");
        }
        event.getHook().sendMessageEmbeds(builder.build()).queue();
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Get information about db entry";
    }

    @Override
    public List<Permission> getPermissions() {
        return Collections.singletonList(Permission.ADMINISTRATOR);
    }

    @Override
    public List<OptionElement> getElements() {
        return Arrays.asList(
                new OptionElement(OptionType.STRING, "name", "Name of the plugin", true, false),
                new OptionElement(OptionType.STRING, "version", "Version of the plugin", false, false)
        );
    }
}
