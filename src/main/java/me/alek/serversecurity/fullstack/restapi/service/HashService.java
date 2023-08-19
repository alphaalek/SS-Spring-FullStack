package me.alek.serversecurity.fullstack.restapi.service;

import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface HashService {

    Optional<Map<String, String>> getHashesOfPlugin(String plugin, String version);

    void saveHashOfPlugin(String fileName, String plugin, String version, String hash);

    PluginDBEntry getPlugin(String plugin, String version);

    List<PluginDBEntry> getAll();

    void savePlugin(PluginDBEntry entry);
}
