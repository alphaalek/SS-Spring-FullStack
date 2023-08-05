package me.alek.serversecurity.restapi.service;

import me.alek.serversecurity.restapi.model.PluginDBEntry;
import me.alek.serversecurity.restapi.model.PluginSignature;
import me.alek.serversecurity.restapi.repository.HashRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DefaultHashServiceImpl implements HashService {

    private final HashRepository hashRepository;

    @Autowired
    public DefaultHashServiceImpl(HashRepository hashRepository) { this.hashRepository = hashRepository; }

    public Optional<String> getHashOfPlugin(String plugin, String version) {
        return Optional.of(getPlugin(plugin, version).getHash());
    }

    public PluginDBEntry getPlugin(String plugin, String version) {

        PluginSignature signature = new PluginSignature(plugin, version);
        Optional<PluginDBEntry> optionalEntry = hashRepository.findBySignature(signature);

        if (optionalEntry.isEmpty()) {

            PluginDBEntry entry = new PluginDBEntry(signature);
            savePlugin(entry);

            return entry;
        }
        return optionalEntry.get();
    }

    public List<PluginDBEntry> getAll() {
        return hashRepository.findAll();
    }

    public List<String> getAllHashes() {
        return getAll().stream().map(PluginDBEntry::getHash).collect(Collectors.toList());
    }

    public boolean savePlugin(String plugin, String version, String hash) {
        return savePlugin(new PluginDBEntry(plugin, version, hash));
    }

    public boolean savePlugin(PluginDBEntry pluginEntry) {
        hashRepository.save(pluginEntry);

        return true;
    }
}
