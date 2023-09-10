package me.alek.serversecurity.fullstack.bot.commands;

import me.alek.serversecurity.fullstack.bot.commands.impl.*;
import me.alek.serversecurity.fullstack.bot.commands.model.OptionElement;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandManager extends ListenerAdapter {

    private final Map<String, DiscordCommandImpl> commandLookup = new HashMap<>();
    private final PluginService hashService;

    public CommandManager(final Guild guild, PluginService hashService) {
        this.hashService = hashService;

        for (DiscordCommandImpl command : getCommands()) {
            final String name = command.getName();
            final String desc = command.getDescription();

            CommandCreateAction action = guild.upsertCommand(name, desc);
            if (command instanceof OptionableDiscordCommandImpl optionableDiscordCommand) {

                for (OptionElement element: optionableDiscordCommand.getElements()) {
                    action = action.addOption(element.type(), element.name(), element.desc(), element.required(), element.autoComplete());
                }
            }
            action.queue();
            commandLookup.put(name, command);
        }
    }

    private List<DiscordCommandImpl> getCommands() {
        return Arrays.asList(
                new ClearOldFileReferencesCmd(hashService),
                new BlacklistCommand(hashService),
                new FindConflictsCommand(hashService),
                new GetFileCommand(hashService),
                new InfoCommand(hashService)
        );
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Optional<DiscordCommandImpl> commandOptional = Optional.ofNullable(commandLookup.get(event.getName()));
        if (commandOptional.isPresent()) {

            event.deferReply().queue();
            Member member = event.getMember();

            if (member != null) {
                DiscordCommandImpl command = commandOptional.get();
                boolean hasPermission = false;

                for (Permission permission : command.getPermissions()) {
                    if (event.getMember().hasPermission(permission)) {
                        hasPermission =  true;
                        break;
                    }
                }
                if (!hasPermission) {
                    event.getHook().sendMessage("Du har ikke adgang til denne kommando.").queue();
                    return;
                }
                command.perform(event, event.getGuild(), member, event.getUser());
            }
        }
        else event.getHook().sendMessage("Der opstod en fejl. Prøv igen senere.").queue();
    }

}
