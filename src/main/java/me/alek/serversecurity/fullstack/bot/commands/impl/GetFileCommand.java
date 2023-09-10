package me.alek.serversecurity.fullstack.bot.commands.impl;

import me.alek.serversecurity.fullstack.bot.commands.OptionableDiscordCommandImpl;
import me.alek.serversecurity.fullstack.bot.commands.model.OptionElement;
import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GetFileCommand implements OptionableDiscordCommandImpl {

    private final PluginService hashService;

    public GetFileCommand(PluginService hashService) {
        this.hashService = hashService;
    }

    @Override
    public void perform(SlashCommandInteractionEvent event, Guild guild, Member member, User user) {
        final String name = event.getOption("name").getAsString();
        final String version = event.getOption("version") == null ? null : event.getOption("version").getAsString();

        final List<JarWindow> jarWindows = new ArrayList<>();
        if (version != null) {
            PluginDBEntry entry = hashService.getLiteralPlugin(name, version);

            jarWindows.addAll(entry.getJarWindows());

        } else {
            for (PluginDBEntry entry : hashService.getAll()) {
                if (entry.getSignature().name().equalsIgnoreCase(name)) {

                    jarWindows.addAll(entry.getJarWindows());
                }
            }
        }
        final List<FileUpload> fileUploadList = new ArrayList<>();
        final List<String> hashes = new ArrayList<>();
        for (JarWindow jarWindow : jarWindows) {
            File file = new File("tmp/" + jarWindow.getFileName());
            fileUploadList.add(FileUpload.fromData(file));
            hashes.add(jarWindow.getFileName() + ": " + jarWindow.getHash());
        }

        if (fileUploadList.isEmpty()) {
            event.getHook().sendMessage("Couldn't find any files matching that signature.").queue();
            return;
        }
        event.getHook().sendMessage(hashes.toString()).queue();
        event.getHook().sendFiles(fileUploadList).queue();
    }

    @Override
    public String getName() {
        return "files";
    }

    @Override
    public String getDescription() {
        return "Get the files of plugin signature";
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
