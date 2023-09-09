package me.alek.serversecurity.fullstack.restapi.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document("plugin")
public class PluginDBEntry {

    @Id
    private PluginSignature signature;
    private List<JarWindow> jarWindows = new ArrayList<>();
    private List<String> blacklistedHashes = new ArrayList<>();
    private int usedEntries;
    private String lastUsage;
    private String firstUsage;

    public PluginDBEntry() {}

    public PluginDBEntry(PluginSignature signature) {
        this.signature = signature;
    }
}