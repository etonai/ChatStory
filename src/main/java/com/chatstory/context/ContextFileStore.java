package com.chatstory.context;

import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ContextFileStore {

    private final Path jsonFilePath;
    private final Path defaultStagingPath;
    private Path currentStagingPath;
    private Path lastDirectory;
    private final List<Path> entries = new ArrayList<>();
    private final List<Runnable> stagingPathListeners = new ArrayList<>();
    private final Gson gson = new Gson();

    public ContextFileStore(String jsonFilePath, String defaultStagingFolderPath) {
        this.jsonFilePath = Path.of(jsonFilePath);
        this.defaultStagingPath = Path.of(defaultStagingFolderPath).toAbsolutePath();
        this.currentStagingPath = this.defaultStagingPath;
        load();
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
            if (data.stagingPath != null) {
                currentStagingPath = Path.of(data.stagingPath).toAbsolutePath();
            }
        } catch (Exception e) {
            System.err.println("[ContextFileStore] Failed to load file list: " + e.getMessage()
                    + " — starting with empty list");
        }
    }

    public void add(Path filePath) {
        Path absolute = filePath.toAbsolutePath();
        if (entries.contains(absolute)) return;
        entries.add(absolute);
        save();
    }

    public void remove(Path filePath) {
        Path absolute = filePath.toAbsolutePath();
        if (entries.remove(absolute)) save();
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

    public Path getStagingPath() {
        return currentStagingPath;
    }

    public void setStagingPath(Path path) {
        this.currentStagingPath = path.toAbsolutePath();
        save();
        for (Runnable listener : stagingPathListeners) listener.run();
    }

    public void addStagingPathListener(Runnable listener) {
        stagingPathListeners.add(listener);
    }

    public StagingResult stageSelected(List<Path> selected) {
        try {
            Files.createDirectories(currentStagingPath);
        } catch (IOException e) {
            System.err.println("[ContextFileStore] Could not create staging folder: " + e.getMessage());
            return new StagingResult(0, selected.size(),
                    List.of("Could not create staging folder: " + e.getMessage()));
        }

        int succeeded = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (Path source : selected) {
            Path target = currentStagingPath.resolve(source.getFileName());
            boolean overwriting = Files.exists(target);
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                if (overwriting) {
                    System.out.println("[ContextFileStore] Overwrote existing staged file: "
                            + source.getFileName());
                }
                succeeded++;
            } catch (IOException e) {
                failed++;
                String msg = source.getFileName() + ": " + e.getMessage();
                failures.add(msg);
                System.err.println("[ContextFileStore] Stage failed — " + msg);
            }
        }

        return new StagingResult(succeeded, failed, Collections.unmodifiableList(failures));
    }

    private void save() {
        StoredData data = new StoredData();
        for (Path p : entries) data.files.add(p.toString());
        if (lastDirectory != null) data.lastDirectory = lastDirectory.toString();
        if (!currentStagingPath.equals(defaultStagingPath)) {
            data.stagingPath = currentStagingPath.toString();
        }
        try (Writer writer = Files.newBufferedWriter(jsonFilePath, StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[ContextFileStore] Failed to save file list: " + e.getMessage());
        }
    }

    public record StagingResult(int succeeded, int failed, List<String> failureMessages) {}

    private static class StoredData {
        List<String> files = new ArrayList<>();
        String lastDirectory;
        String stagingPath;
    }
}
