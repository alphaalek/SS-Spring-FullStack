package me.alek.serversecurity.fullstack.search;

import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.malware.scanning.StringScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JarConflictContext {

    private final List<JarWindow> jarWindows;

    private final List<String> allStrings = new ArrayList<>();
    private final Map<JarWindow, List<String>> windowStringMap = new HashMap<>();

    public JarConflictContext(List<JarWindow> jarWindows) {
        this.jarWindows = jarWindows;

        startScanStrings();
    }

    private void startScanStrings() {
        for (JarWindow jarWindow: jarWindows) {

            File file = new File("tmp/" + jarWindow.getFileName());
            if (!file.exists()) continue;

            StringScanner scanner = new StringScanner(file);
            scanner.startScan();

            windowStringMap.put(jarWindow, scanner.getResultData());
        }
        for (List<String> strings : windowStringMap.values()) {
            for (String string : strings) {

                if (allStrings.contains(string)) allStrings.add(string);
            }
        }
    }

    public List<String> getAllStrings() {
        return allStrings;
    }

    public List<String> getStringsFor(JarWindow jarWindow) {
        return windowStringMap.get(jarWindow);
    }
}
