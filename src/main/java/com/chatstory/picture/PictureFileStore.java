package com.chatstory.picture;

import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class PictureFileStore {

    private final Path configFilePath;
    private Path imageFile;
    private final List<Runnable> listeners = new ArrayList<>();
    private final Gson gson = new Gson();

    public PictureFileStore(String configFilePath) {
        this.configFilePath = Path.of(configFilePath);
        load();
    }

    public Path getImageFile() {
        return imageFile;
    }

    public void setImageFile(Path file) {
        this.imageFile = file != null ? file.toAbsolutePath() : null;
        save();
        for (Runnable listener : listeners) listener.run();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void load() {
        if (!Files.exists(configFilePath)) return;
        try (Reader reader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
            StoredData data = gson.fromJson(reader, StoredData.class);
            if (data != null && data.imageFile != null && !data.imageFile.isBlank()) {
                imageFile = Path.of(data.imageFile).toAbsolutePath();
            }
        } catch (Exception e) {
            System.err.println("[PictureFileStore] Failed to load: " + e.getMessage());
        }
    }

    private void save() {
        StoredData data = new StoredData();
        data.imageFile = imageFile != null ? imageFile.toString() : null;
        try (Writer writer = Files.newBufferedWriter(configFilePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[PictureFileStore] Failed to save: " + e.getMessage());
        }
    }

    private static class StoredData {
        String imageFile;
    }
}
