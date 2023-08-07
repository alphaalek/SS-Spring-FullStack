package me.alek.serversecurity.restapi.service;

import me.alek.serversecurity.restapi.model.PluginDBEntry;
import me.alek.serversecurity.restapi.model.PluginSignature;
import me.alek.serversecurity.restapi.repository.HashRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DefaultHashServiceImpl implements HashService {

    private final HashRepository hashRepository;

    @Autowired
    public DefaultHashServiceImpl(HashRepository hashRepository) { this.hashRepository = hashRepository; }

    public Optional<String> getHashOfPlugin(String plugin, String version) {
        return Optional.ofNullable(getPlugin(plugin, version).getHash());
    }

    public void setHashOfPlugin(String plugin, String version, String hash) {
        PluginDBEntry entry = getPlugin(plugin, version);
        entry.setHash(hash);

        savePlugin(entry);
    }

    public PluginDBEntry getPlugin(String plugin, String version) {
        PluginDBEntry entry = getLiteralPlugin(plugin, version);

        String now = LocalDateTime.now().toString();
        if (entry.getFirstUsage() == null) entry.setFirstUsage(now);
        entry.setLastUsage(now);

        entry.setUsedEntries(entry.getUsedEntries() + 1);
        savePlugin(entry);

        return entry;
    }

    private PluginDBEntry getLiteralPlugin(String plugin, String version) {
        PluginSignature signature = new PluginSignature(plugin, version);
        Optional<PluginDBEntry> optionalEntry = hashRepository.findBySignature(signature);

        return optionalEntry.orElseGet(() -> new PluginDBEntry(signature));
    }

    public List<PluginDBEntry> getAll() {
        return hashRepository.findAll();
    }

    public List<String> getAllHashes() {
        return getAll().stream().map(PluginDBEntry::getHash).collect(Collectors.toList());
    }

    public void savePlugin(PluginDBEntry pluginEntry) {
        hashRepository.save(pluginEntry);
    }
}
