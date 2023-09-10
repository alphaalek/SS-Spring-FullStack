package me.alek.serversecurity.fullstack.restapi.service;

import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.model.PluginSignature;
import me.alek.serversecurity.fullstack.restapi.repository.HashRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultPluginServiceImpl implements PluginService {

    private final HashRepository hashRepository;

    @Autowired
    public DefaultPluginServiceImpl(HashRepository hashRepository) { this.hashRepository = hashRepository; }

    public List<JarWindow> getJarWindowsOfPlugin(String plugin, String version) {
        return getLiteralPlugin(plugin, version).getJarWindows();
    }

    public List<String> getBlacklistedHashesOfPlugin(String plugin, String version) {
        return getLiteralPlugin(plugin, version).getBlacklistedHashes();
    }

    public void addBlacklistedHash(PluginDBEntry entry, String hash) {
        List<String> blacklistedHashes = entry.getBlacklistedHashes();
        List<JarWindow> jarWindows = entry.getJarWindows();

        // check if this hash can be stored in the blacklisted section
        if (blacklistedHashes.stream().anyMatch(blacklistedHash -> blacklistedHash.equals(hash))) return;
        if (jarWindows.stream().noneMatch(jarWindow -> jarWindow.getHash().equals(hash))) return;

        blacklistedHashes.add(hash);

        // set the jar window containing this hash to be blacklisted
        jarWindows.stream()
                .filter(jarWindow -> jarWindow.getHash().equals(hash))
                .forEach(jarWindow -> jarWindow.setBlacklisted(true));

        savePlugin(entry);
    }

    public void addBlacklistedHash(String plugin, String version, String hash) {
        PluginDBEntry entry = getLiteralPlugin(plugin, version);

        addBlacklistedHash(entry, hash);
    }

    public void saveJarWindowOfPlugin(String fileName, List<String> resultData, String plugin, String version, String hash, boolean blacklisted) {
        PluginDBEntry entry = getLiteralPlugin(plugin, version);

        // pull the jar windows for this plugin version if there are any
        List<JarWindow> jarWindows = entry.getJarWindows();

        // check if the hash or filename of this plugin is already stored
        if (jarWindows.stream().anyMatch(jarWindow -> jarWindow.getHash().equals(hash) || jarWindow.getFileName().equals(fileName))) return;

        // put the jar window into the list of jar windows for this plugin version
        JarWindow newJarWindow = new JarWindow(hash, fileName, resultData, blacklisted);
        jarWindows.add(newJarWindow);

        // remove that entry from the saved hashes
        jarWindows.removeIf(jarWindow -> !Files.exists(Paths.get("tmp/" + jarWindow.getFileName())));

        if (blacklisted) {
            addBlacklistedHash(entry, hash);
        }
        else savePlugin(entry);
    }

    private PluginDBEntry updatePlugin(String plugin, String version) {
        PluginDBEntry entry = getLiteralPlugin(plugin, version);

        // set the last usage and maybe first usage of this plugin version entry
        String now = LocalDateTime.now().toString();
        if (entry.getFirstUsage() == null) entry.setFirstUsage(now);
        entry.setLastUsage(now);

        savePlugin(entry);

        return entry;
    }

    public PluginDBEntry getPluginAndIncrementUsage(String plugin, String version) {
        PluginDBEntry entry = updatePlugin(plugin, version);

        // set the amount of total times this plugin version entry has been accessed
        entry.setUsedEntries(entry.getUsedEntries() + 1);

        savePlugin(entry);

        return entry;
    }

    public PluginDBEntry getLiteralPlugin(String plugin, String version) {
        PluginSignature signature = new PluginSignature(plugin.toLowerCase(), version.toLowerCase());
        Optional<PluginDBEntry> optionalEntry = hashRepository.findBySignature(signature);

        return optionalEntry.orElseGet(() -> new PluginDBEntry(signature));
    }

    public boolean hasRegisteredHash(String name, String version, String hash) {
        List<JarWindow> jarWindows = getJarWindowsOfPlugin(name, version);

        return jarWindows.stream().anyMatch((jarWindow) -> jarWindow.getHash().equals(hash));
    }

    public boolean isHashBlacklisted(String name, String version, String hash) {
        List<String> blacklistedHashes = getBlacklistedHashesOfPlugin(name, version);

        return blacklistedHashes.contains(hash);
    }

    public List<PluginDBEntry> getAll() {
        return hashRepository.findAll();
    }

    public void savePlugin(PluginDBEntry pluginEntry) {
        hashRepository.save(pluginEntry);
    }
}
