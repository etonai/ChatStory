package com.chatstory.canon;

import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CanonFolderStore {

    public static final String TEMP_FILE_NAME = "00_Canon_Temp.md";

    private final Path configFilePath;
    private Path canonFolder;
    private final List<Runnable> listeners = new ArrayList<>();
    private final Gson gson = new Gson();

    public CanonFolderStore(String configFilePath) {
        this.configFilePath = Path.of(configFilePath);
        load();
    }

    public Path getCanonFolder() {
        return canonFolder;
    }

    public void setCanonFolder(Path folder) {
        this.canonFolder = folder != null ? folder.toAbsolutePath() : null;
        save();
        for (Runnable listener : listeners) listener.run();
    }

    public Path getTempFile() {
        return canonFolder == null ? null : canonFolder.resolve(TEMP_FILE_NAME);
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void load() {
        if (!Files.exists(configFilePath)) return;
        try (Reader reader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
            StoredData data = gson.fromJson(reader, StoredData.class);
            if (data != null && data.canonFolder != null && !data.canonFolder.isBlank()) {
                canonFolder = Path.of(data.canonFolder).toAbsolutePath();
            }
        } catch (Exception e) {
            System.err.println("[CanonFolderStore] Failed to load: " + e.getMessage());
        }
    }

    private void save() {
        StoredData data = new StoredData();
        data.canonFolder = canonFolder != null ? canonFolder.toString() : null;
        try (Writer writer = Files.newBufferedWriter(configFilePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[CanonFolderStore] Failed to save: " + e.getMessage());
        }
    }

    private static class StoredData {
        String canonFolder;
    }
}
