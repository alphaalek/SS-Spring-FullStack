package me.alek.serversecurity.restapi.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Data
@Document("plugin")
public class PluginDBEntry {

    @Id
    private PluginSignature signature;
    private String hash;
    private Map<String, Integer> otherHases = new HashMap<>();

    public PluginDBEntry(String plugin, String version, String hash) {
        this(new PluginSignature(plugin, version), hash);
    }

    public PluginDBEntry(PluginSignature signature) {
        this(signature, null);
    }

    public PluginDBEntry(PluginSignature signature, String hash) {
        this.signature = signature;
        this.hash = hash;
    }

    public boolean hasHash() {
        return this.hash != null;
    }

}