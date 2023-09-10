package me.alek.serversecurity.fullstack.bot.commands.model.search;

import me.alek.serversecurity.fullstack.restapi.model.JarWindow;
import me.alek.serversecurity.malware.scanning.FileSizeScanner;
import me.alek.serversecurity.malware.scanning.StringScanner;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class JarConflictContext {

    private final List<JarWindow> jarWindows;

    private final List<String> allStrings = new ArrayList<>();
    private final Map<String, Integer> stringUniqueness = new HashMap<>();
    private final Map<JarWindow, List<String>> windowStringMap = new HashMap<>();

    private final Map<JarWindow, Map<String, Long>> windowFileSizeMap = new HashMap<>();
    private final Map<String, Integer> fileUniqueness = new HashMap<>();
    private final Set<String> unequalFileContents = new HashSet<>();

    public JarConflictContext(List<JarWindow> jarWindows) {
        this.jarWindows = jarWindows;

        startScanStrings();
    }

    private void startScanStrings() {
        for (JarWindow jarWindow : jarWindows) {
            File file = new File("tmp/" + jarWindow.getFileName());
            if (!file.exists()) continue;

            StringScanner stringScanner = new StringScanner(file);
            stringScanner.startScan();

            windowStringMap.put(jarWindow, stringScanner.getFlatResultData());

            FileSizeScanner fileSizeScanner = new FileSizeScanner(file);
            fileSizeScanner.startScan();

            windowFileSizeMap.put(jarWindow, fileSizeScanner.getFlatResultData().get(0));
        }
        for (List<String> strings : windowStringMap.values()) {
            for (String string : strings) {
                stringUniqueness.put(string, stringUniqueness.getOrDefault(string, 0) + 1);

                if (allStrings.contains(string)) allStrings.add(string);
            }
        }
        for (Map<String, Long> map : windowFileSizeMap.values()) {
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                fileUniqueness.put(entry.getKey(), fileUniqueness.getOrDefault(entry.getKey(), 0) + 1);

                if (!getFilesSizesFor(entry.getKey()).stream().allMatch((size) -> Objects.equals(size, entry.getValue()))) {
                    unequalFileContents.add(entry.getKey());
                }
            }
        }
    }

    public int getUniquenessFor(String string, boolean isString) {
        if (isString) return stringUniqueness.getOrDefault(string, 0);

        return fileUniqueness.getOrDefault(string, 0);
    }

    public List<String> getAllStrings() {
        return allStrings;
    }

    public List<String> getStringsFor(JarWindow jarWindow) {
        return windowStringMap.get(jarWindow);
    }

    public List<String> getAllFiles() {
        return windowFileSizeMap.values().stream().flatMap((map) -> map.keySet().stream()).collect(Collectors.toList());
    }

    public long getSizeFor(JarWindow window, String file) {
        return windowFileSizeMap
                .getOrDefault(window, new HashMap<>())
                .getOrDefault(file, 0L);
    }

    public List<Long> getFilesSizesFor(String fileName) {
        return windowFileSizeMap.values().stream()
                .map((map) -> map.get(fileName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    public Set<String> getFilesFor(JarWindow jarWindow) {
        return windowFileSizeMap.get(jarWindow).keySet();
    }

    public Set<String> getUnequalFileContents() {
        return unequalFileContents;
    }
}
