package me.alek.serversecurity.fullstack.restapi.controller;

import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.bot.LoggingMethod;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.service.HashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plugin")
public class HashController {

    private final HashService hashService;

    @Autowired
    public HashController(HashService hashService) { this.hashService = hashService; }

    @GetMapping
    public PluginDBEntry getPlugin(@RequestParam(value = "name") String name, @RequestParam(value = "version") String version) {
        DiscordBot.log(LoggingMethod.RESTAPI, "RestController: A client is accessing entry " + name + " " + version + "");

        return hashService.getPlugin(name, version);
    }

    @GetMapping("/all")
    public List<PluginDBEntry> getAll() { return hashService.getAll(); }

}
