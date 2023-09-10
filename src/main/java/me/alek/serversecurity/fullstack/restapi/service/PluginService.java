package me.alek.serversecurity.fullstack.restapi.service;

import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;

import java.util.List;

public interface PluginService {

    List<String> getBlacklistedHashesOfPlugin(String plugin, String version);

    List<JarWindow> getJarWindowsOfPlugin(String plugin, String version);

    void saveJarWindowOfPlugin(String fileName, List<String> resultData, String plugin, String version, String hash, boolean blackListed);

    void addBlacklistedHash(String plugin, String version, String hash);

    PluginDBEntry getLiteralPlugin(String plugin, String version);

    PluginDBEntry getPluginAndIncrementUsage(String plugin, String version);

    boolean hasRegisteredHash(String name, String version, String hash);

    boolean isHashBlacklisted(String name, String version, String hash);

    List<PluginDBEntry> getAll();

    void savePlugin(PluginDBEntry entry);
}
