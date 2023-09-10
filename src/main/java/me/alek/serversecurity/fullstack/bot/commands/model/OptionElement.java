package me.alek.serversecurity.fullstack.bot.commands.model;

import net.dv8tion.jda.api.interactions.commands.OptionType;

public record OptionElement(OptionType type, String name, String desc, boolean required, boolean autoComplete) {
}
