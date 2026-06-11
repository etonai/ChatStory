package com.chatstory.session;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SnapshotFileStore {

    private final Path configFilePath;
    private Path lastDirectory;
    private final Gson gson = new Gson();

    public SnapshotFileStore(String configFilePath) {
        this.configFilePath = Path.of(configFilePath);
        load();
    }

    public Path getLastDirectory() {
        return lastDirectory;
    }

    public void setLastDirectory(Path directory) {
        this.lastDirectory = directory != null ? directory.toAbsolutePath() : null;
        save();
    }

    private void load() {
        if (!Files.exists(configFilePath)) return;
        try (Reader reader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
            StoredData data = gson.fromJson(reader, StoredData.class);
            if (data != null && data.lastDirectory != null && !data.lastDirectory.isBlank()) {
                lastDirectory = Path.of(data.lastDirectory).toAbsolutePath();
            }
        } catch (Exception e) {
            System.err.println("[SnapshotFileStore] Failed to load: " + e.getMessage());
        }
    }

    private void save() {
        StoredData data = new StoredData();
        data.lastDirectory = lastDirectory != null ? lastDirectory.toString() : null;
        try (Writer writer = Files.newBufferedWriter(configFilePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[SnapshotFileStore] Failed to save: " + e.getMessage());
        }
    }

    private static class StoredData {
        String lastDirectory;
    }
}
