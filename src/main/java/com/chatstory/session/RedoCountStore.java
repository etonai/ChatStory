package com.chatstory.session;

import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class RedoCountStore {

    private final Path configFilePath;
    private int redoCount;
    private final Gson gson = new Gson();

    public RedoCountStore(String configFilePath) {
        this.configFilePath = Path.of(configFilePath);
        load();
    }

    public int getCount() {
        return redoCount;
    }

    public void setCount(int count) {
        this.redoCount = count;
        save();
    }

    private void load() {
        if (!Files.exists(configFilePath)) return;
        try (Reader reader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
            StoredData data = gson.fromJson(reader, StoredData.class);
            if (data != null) {
                redoCount = data.redoCount;
            }
        } catch (Exception e) {
            System.err.println("[RedoCountStore] Failed to load: " + e.getMessage());
        }
    }

    private void save() {
        StoredData data = new StoredData();
        data.redoCount = redoCount;
        try (Writer writer = Files.newBufferedWriter(configFilePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[RedoCountStore] Failed to save: " + e.getMessage());
        }
    }

    private static class StoredData {
        int redoCount;
    }
}
