package me.alek.serversecurity.fullstack.bot.commands.impl;

import me.alek.serversecurity.fullstack.bot.commands.DiscordCommandImpl;
import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClearOldFileReferencesCmd implements DiscordCommandImpl {

    private final AtomicBoolean running = new AtomicBoolean();
    private final PluginService hashService;

    public ClearOldFileReferencesCmd(PluginService hashService) { this.hashService = hashService; }

    @Override
    public synchronized void perform(SlashCommandInteractionEvent event, Guild guild, Member member, User user) {
        if (running.get()) {
            event.getHook().sendMessage("Already in use! Wait some time...").queue();
            return;
        }
        final Set<String> fileNames = new HashSet<>();

        int clearedFileReferences = 0;
        int clearedWindowReferences = 0;

        // save the names of all the files registered
        File tmpDirectory = new File("tmp/");
        for (File file : Optional.ofNullable(tmpDirectory.listFiles()).orElseGet(() -> new File[0])) {
            fileNames.add(file.getName());
        }

        // loop through all the entries
        for (PluginDBEntry entry: hashService.getAll()) {

            Iterator<JarWindow> jarWindowIterator = entry.getJarWindows().iterator();
            while (jarWindowIterator.hasNext())  {
                JarWindow jarWindow = jarWindowIterator.next();

                if (!Files.exists(Paths.get("tmp/" + jarWindow.getFileName()))) {
                    jarWindowIterator.remove();
                    clearedFileReferences++;
                }
                fileNames.remove(jarWindow.getFileName());
            }
            hashService.savePlugin(entry);
        }

        // delete all the files that did not exist
        for (String fileName : fileNames) {
            clearedWindowReferences++;
            try {
                Files.deleteIfExists(Paths.get("tmp/" + fileName));
            } catch (Exception ex) {
            }
        }

        event.getHook().sendMessage("Removed " + clearedFileReferences + " file references and " + clearedWindowReferences + " window references.").queue();
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clear old file references in entries of db documents.";
    }

    @Override
    public List<Permission> getPermissions() {
        return Collections.singletonList(Permission.ADMINISTRATOR);
    }
}
