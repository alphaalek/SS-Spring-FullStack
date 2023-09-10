package me.alek.serversecurity.fullstack.restapi.controller;

import me.alek.serversecurity.fullstack.bot.DiscordBot;
import me.alek.serversecurity.fullstack.bot.LoggingMethod;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.service.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/plugin")
public class HashController {

    private final PluginService hashService;

    @Autowired
    public HashController(PluginService hashService) { this.hashService = hashService; }

    @GetMapping
    public PluginDBEntry getPlugin(@RequestParam(value = "name") String name, @RequestParam(value = "version") String version) {
        DiscordBot.get().log(LoggingMethod.RESTAPI, "RestController: A client is accessing entry " + name + " " + version + "");

        return hashService.getPluginAndIncrementUsage(name, version);
    }

    @GetMapping("/all")
    public List<PluginDBEntry> getAll() { return hashService.getAll(); }

}
