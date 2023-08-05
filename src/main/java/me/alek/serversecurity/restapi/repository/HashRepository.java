package me.alek.serversecurity.restapi.repository;

import me.alek.serversecurity.restapi.model.PluginDBEntry;
import me.alek.serversecurity.restapi.model.PluginSignature;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface HashRepository extends MongoRepository<PluginDBEntry, PluginSignature> {

    Optional<PluginDBEntry> findBySignature(PluginSignature signature);

    @NotNull
    List<PluginDBEntry> findAll();
}
