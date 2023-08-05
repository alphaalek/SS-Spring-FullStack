package me.alek.serversecurity.restapi.service;

import me.alek.serversecurity.restapi.model.PluginDBEntry;

import java.util.List;
import java.util.Optional;

public interface HashService {

    Optional<String> getHashOfPlugin(String plugin, String version);

    PluginDBEntry getPlugin(String plugin, String version);

    List<String> getAllHashes();

    List<PluginDBEntry> getAll();

    boolean savePlugin(String plugin, String version, String hash);

    boolean savePlugin(PluginDBEntry entry);
}
