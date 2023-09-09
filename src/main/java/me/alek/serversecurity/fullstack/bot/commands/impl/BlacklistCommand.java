package me.alek.serversecurity.fullstack.bot.commands.impl;

import me.alek.serversecurity.fullstack.bot.commands.DiscordCommandImpl;
import me.alek.serversecurity.fullstack.restapi.service.HashService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Collections;
import java.util.List;

public class BlacklistCommand implements DiscordCommandImpl {

    private final HashService hashService;

    public BlacklistCommand(HashService hashService) {
        this.hashService = hashService;
    }

    @Override
    public void perform(SlashCommandInteractionEvent event, Guild guild, Member member, User user) {
        event.getHook().sendMessage("Ikke lavet endnu").queue();
    }

    @Override
    public String getName() {
        return "blacklist";
    }

    @Override
    public String getDescription() {
        return "Blacklist hash of jar window.";
    }

    @Override
    public List<Permission> getPermissions() {
        return Collections.singletonList(Permission.ADMINISTRATOR);
    }
}
