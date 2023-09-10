package me.alek.serversecurity.fullstack.bot.commands;

import me.alek.serversecurity.fullstack.bot.commands.model.OptionElement;

import java.util.List;

public interface OptionableDiscordCommandImpl extends DiscordCommandImpl {

    List<OptionElement> getElements();
}
