package me.alek.serversecurity.fullstack.search;

import me.alek.serversecurity.fullstack.restapi.model.JarWindow;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private synchronized void startSearch() {
        if (doneSearch || searching) return;

        searching = true;

        List<String> missingStrings = new ArrayList<>();
        for (String string: context.getAllStrings()) {
            if (!context.getStringsFor(jarWindow).contains(string)) missingStrings.add(string);
        }

        dataSearchMap.put("Size", file.length());
        dataSearchMap.put("Missing Strings (" + missingStrings.size() + ")", missingStrings);
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
