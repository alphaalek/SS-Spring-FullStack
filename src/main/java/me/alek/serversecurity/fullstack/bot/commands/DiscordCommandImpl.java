package me.alek.serversecurity.fullstack.bot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;

public interface DiscordCommandImpl {

    void perform(SlashCommandInteractionEvent event, Guild guild, Member member, User user);

    String getName();

    String getDescription();

    List<Permission> getPermissions();
}
