package com.chatstory.rules;

import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RulesFileStore {

    private final Path jsonFilePath;
    private Path lastDirectory;
    private final List<Path> entries = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private final Gson gson = new Gson();

    public RulesFileStore(String jsonFilePath) {
        this.jsonFilePath = Path.of(jsonFilePath);
        load();
    }

    public void add(Path filePath) {
        Path absolute = filePath.toAbsolutePath();
        if (entries.contains(absolute)) return;
        entries.add(absolute);
        save();
        for (Runnable listener : listeners) listener.run();
    }

    public void remove(Path filePath) {
        Path absolute = filePath.toAbsolutePath();
        if (entries.remove(absolute)) {
            save();
            for (Runnable listener : listeners) listener.run();
        }
    }

    public List<Path> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public Path getLastDirectory() {
        return lastDirectory;
    }

    public void setLastDirectory(Path dir) {
        this.lastDirectory = dir;
        save();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void load() {
        if (!Files.exists(jsonFilePath)) return;
        try (Reader reader = Files.newBufferedReader(jsonFilePath, StandardCharsets.UTF_8)) {
            StoredData data = gson.fromJson(reader, StoredData.class);
            if (data == null) return;
            if (data.files != null) {
                for (String p : data.files) {
                    Path absolute = Path.of(p).toAbsolutePath();
                    if (!entries.contains(absolute)) entries.add(absolute);
                }
            }
            if (data.lastDirectory != null) {
                lastDirectory = Path.of(data.lastDirectory);
            }
        } catch (Exception e) {
            System.err.println("[RulesFileStore] Failed to load: " + e.getMessage());
        }
    }

    private void save() {
        StoredData data = new StoredData();
        for (Path p : entries) data.files.add(p.toString());
        if (lastDirectory != null) data.lastDirectory = lastDirectory.toString();
        try (Writer writer = Files.newBufferedWriter(jsonFilePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[RulesFileStore] Failed to save: " + e.getMessage());
        }
    }

    private static class StoredData {
        List<String> files = new ArrayList<>();
        String lastDirectory;
    }
}
