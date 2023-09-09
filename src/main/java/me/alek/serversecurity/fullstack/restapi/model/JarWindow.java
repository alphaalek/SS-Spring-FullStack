package me.alek.serversecurity.fullstack.restapi.model;

import lombok.Data;

import java.util.List;

@Data
public class JarWindow {

    String hash;
    String fileName;
    List<String> resultData;
    boolean blacklisted;

    public JarWindow() {}

    public JarWindow(String hash, String fileName, List<String> resultData, boolean blacklisted) {
        this.hash = hash;
        this.fileName = fileName;
        this.resultData = resultData;
        this.blacklisted = blacklisted;
    }
}
