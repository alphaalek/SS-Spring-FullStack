package me.alek.serversecurity.fullstack.bot.commands.model.search;

import me.alek.serversecurity.fullstack.restapi.model.JarWindow;

import java.io.File;
import java.util.*;

public class JarConflictSearch {

    private boolean doneSearch;
    private boolean searching;

    private final Map<String, Object> dataSearchMap = new HashMap<>();

    private final File file;
    private final List<String> resultData;
    private final JarConflictContext context;
    private final JarWindow jarWindow;

    public JarConflictSearch(JarWindow jarWindow, String fileName, List<String> resultData, JarConflictContext context) {
        this.file = new File("tmp/" + fileName);
        this.jarWindow = jarWindow;
        this.resultData = resultData;
        this.context = context;

        // start the search
        startSearch();
    }

    private void handleCommonContext(List<String> missing, List<String> unique, List<String> context, List<String> all, boolean isString) {
        for (String string: all) {
            if (!context.contains(string)) missing.add(string);
            else if (this.context.getUniquenessFor(string, isString) == 1) unique.add(string);
        }
    }

    private String getValue(String message) {
        if (message.length() > 400) return message.substring(0, 400);
        return message;
    }

    private synchronized void startSearch() {
        if (doneSearch || searching) return;

        searching = true;

        List<String> missingStrings = new ArrayList<>();
        List<String> uniqueStrings = new ArrayList<>();
        handleCommonContext(missingStrings, uniqueStrings, context.getStringsFor(jarWindow), context.getAllStrings(), true);

        List<String> missingFiles = new ArrayList<>();
        List<String> uniqueFiles = new ArrayList<>();
        handleCommonContext(missingFiles, uniqueFiles, new ArrayList<>(context.getFilesFor(jarWindow)), context.getAllFiles(), false);

        dataSearchMap.put("Size", file.length());
        dataSearchMap.put("Scan Results", resultData);
        dataSearchMap.put("Strings", String.format("Missing (%s/%s) Unique (%s/%s)", missingStrings.size(), context.getAllStrings().size(), uniqueStrings.size(), context.getAllStrings().size()));
        dataSearchMap.put("Missing Strings", getValue(missingStrings.toString()));
        dataSearchMap.put("Unique Strings", getValue(uniqueStrings.toString()));
        dataSearchMap.put("Missing Files", getValue(missingFiles.toString()));
        dataSearchMap.put("Unique Files", getValue(uniqueFiles.toString()));

        List<String> unequalFiles = new ArrayList<>();
        for (String file : context.getUnequalFileContents()) {
            unequalFiles.add(file + ": " + context.getSizeFor(jarWindow, file));
        }
        dataSearchMap.put("File Inequality", getValue(unequalFiles.toString()));
    }

    public Map<String, Object> getDataSearchMap() {
        return dataSearchMap;
    }

    public boolean isSearching() {
        return searching;
    }

    public boolean isDone() {
        return doneSearch;
    }
}
