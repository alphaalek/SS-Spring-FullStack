package me.alek.serversecurity.fullstack.restapi.model;

public record PluginSignature(String name, String version) {

    @Override
    public String toString() {
        return "Name: " + name + ", Version: " + version;
    }

}
