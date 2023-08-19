package me.alek.serversecurity.fullstack.restapi.service;

import me.alek.serversecurity.fullstack.restapi.model.PluginDBEntry;
import me.alek.serversecurity.fullstack.restapi.model.PluginSignature;
import me.alek.serversecurity.fullstack.restapi.repository.HashRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultHashServiceImpl implements HashService {

    private final HashRepository hashRepository;

    @Autowired
    public DefaultHashServiceImpl(HashRepository hashRepository) { this.hashRepository = hashRepository; }

    public Optional<Map<String, String>> getHashesOfPlugin(String plugin, String version) {
        return Optional.ofNullable(getPlugin(plugin, version).getHashes());
    }

    public void saveHashOfPlugin(String fileName, String plugin, String version, String hash) {
        PluginDBEntry entry = getPlugin(plugin, version);

        // pull the already stored hashes for this plugin version if there are any
        Optional<Map<String, String>> hashes = getHashesOfPlugin(plugin, version);

        Map<String, String> map = hashes.orElseGet(HashMap::new);
        map.put(hash, fileName);

        // remove saved hashes of files which no longer exists
        for (Map.Entry<String, String> fileEntry : map.entrySet()) {
            if (!Files.exists(Paths.get(fileEntry.getValue()))) {
                map.remove(fileEntry.getKey());
            }
        }
        // save the hast
        entry.setHashes(map);
        savePlugin(entry);
    }

    public PluginDBEntry getPlugin(String plugin, String version) {
        PluginDBEntry entry = getLiteralPlugin(plugin, version);

        // set the last usage of this plugin version entry
        String now = LocalDateTime.now().toString();
        if (entry.getFirstUsage() == null) entry.setFirstUsage(now);
        entry.setLastUsage(now);

        // set the amount of total times this plugin version entry has been accessed
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

    public void savePlugin(PluginDBEntry pluginEntry) {
        hashRepository.save(pluginEntry);
    }
}
