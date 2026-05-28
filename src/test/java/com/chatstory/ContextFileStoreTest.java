package com.chatstory;

import com.chatstory.context.ContextFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContextFileStoreTest {

    @TempDir
    Path tempDir;

    private ContextFileStore store() {
        return new ContextFileStore(
                tempDir.resolve("context-files.json").toString(),
                tempDir.resolve("staging").toString());
    }

    @Test
    void emptyOnAbsentJson() {
        assertTrue(store().getEntries().isEmpty());
    }

    @Test
    void emptyOnMalformedJson() throws IOException {
        Path json = tempDir.resolve("context-files.json");
        Files.writeString(json, "not valid json {{{{");
        ContextFileStore s = new ContextFileStore(json.toString(),
                tempDir.resolve("staging").toString());
        assertTrue(s.getEntries().isEmpty());
    }

    @Test
    void addAndRetrieve() throws IOException {
        Path file = Files.createFile(tempDir.resolve("test.md"));
        ContextFileStore s = store();
        s.add(file);
        assertEquals(1, s.getEntries().size());
        assertEquals(file.toAbsolutePath(), s.getEntries().get(0));
    }

    @Test
    void duplicateAddIsIgnored() throws IOException {
        Path file = Files.createFile(tempDir.resolve("test.md"));
        ContextFileStore s = store();
        s.add(file);
        s.add(file);
        assertEquals(1, s.getEntries().size());
    }

    @Test
    void remove() throws IOException {
        Path file = Files.createFile(tempDir.resolve("test.md"));
        ContextFileStore s = store();
        s.add(file);
        s.remove(file.toAbsolutePath());
        assertTrue(s.getEntries().isEmpty());
    }

    @Test
    void removeNonExistentIsNoOp() {
        ContextFileStore s = store();
        assertDoesNotThrow(() -> s.remove(Path.of("/nonexistent/file.md")));
    }

    @Test
    void getEntriesIsUnmodifiable() throws IOException {
        Path file = Files.createFile(tempDir.resolve("test.md"));
        ContextFileStore s = store();
        s.add(file);
        assertThrows(UnsupportedOperationException.class,
                () -> s.getEntries().add(Path.of("/other.md")));
    }

    @Test
    void insertionOrderPreserved() throws IOException {
        Path a = Files.createFile(tempDir.resolve("alpha.md"));
        Path b = Files.createFile(tempDir.resolve("beta.md"));
        Path c = Files.createFile(tempDir.resolve("gamma.md"));
        ContextFileStore s = store();
        s.add(a);
        s.add(b);
        s.add(c);
        assertEquals(List.of(a.toAbsolutePath(), b.toAbsolutePath(), c.toAbsolutePath()),
                s.getEntries());
    }

    @Test
    void persistsAcrossInstances() throws IOException {
        Path file = Files.createFile(tempDir.resolve("test.md"));
        Path json = tempDir.resolve("context-files.json");
        Path staging = tempDir.resolve("staging");

        ContextFileStore s1 = new ContextFileStore(json.toString(), staging.toString());
        s1.add(file);

        ContextFileStore s2 = new ContextFileStore(json.toString(), staging.toString());
        assertEquals(1, s2.getEntries().size());
        assertEquals(file.toAbsolutePath(), s2.getEntries().get(0));
    }

    @Test
    void stageSelectedSucceeds() throws IOException {
        Path file = Files.createFile(tempDir.resolve("test.md"));
        Files.writeString(file, "some content");
        Path staging = tempDir.resolve("staging");

        ContextFileStore s = new ContextFileStore(
                tempDir.resolve("context-files.json").toString(),
                staging.toString());
        s.add(file);

        ContextFileStore.StagingResult result = s.stageSelected(List.of(file.toAbsolutePath()));
        assertEquals(1, result.succeeded());
        assertEquals(0, result.failed());
        assertTrue(Files.exists(staging.resolve("test.md")));
        assertEquals("some content", Files.readString(staging.resolve("test.md")));
    }

    @Test
    void stageSelectedMissingFileFails() {
        Path missing = tempDir.resolve("missing.md");
        Path staging = tempDir.resolve("staging");

        ContextFileStore s = new ContextFileStore(
                tempDir.resolve("context-files.json").toString(),
                staging.toString());

        ContextFileStore.StagingResult result = s.stageSelected(List.of(missing));
        assertEquals(0, result.succeeded());
        assertEquals(1, result.failed());
        assertFalse(result.failureMessages().isEmpty());
    }

    @Test
    void lastDirectoryPersistsAcrossInstances() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("mydir"));
        Path json = tempDir.resolve("context-files.json");
        Path staging = tempDir.resolve("staging");

        ContextFileStore s1 = new ContextFileStore(json.toString(), staging.toString());
        s1.setLastDirectory(dir);

        ContextFileStore s2 = new ContextFileStore(json.toString(), staging.toString());
        assertEquals(dir, s2.getLastDirectory());
    }

    @Test
    void stagingPathDefaultsToConstructorArg() {
        Path staging = tempDir.resolve("staging");
        ContextFileStore s = new ContextFileStore(
                tempDir.resolve("context-files.json").toString(), staging.toString());
        assertEquals(staging.toAbsolutePath(), s.getStagingPath());
    }

    @Test
    void stagingPathPersistsAcrossInstances() throws IOException {
        Path customStaging = Files.createDirectory(tempDir.resolve("custom-staging"));
        Path json = tempDir.resolve("context-files.json");
        Path defaultStaging = tempDir.resolve("staging");

        ContextFileStore s1 = new ContextFileStore(json.toString(), defaultStaging.toString());
        s1.setStagingPath(customStaging);

        ContextFileStore s2 = new ContextFileStore(json.toString(), defaultStaging.toString());
        assertEquals(customStaging.toAbsolutePath(), s2.getStagingPath());
    }

    @Test
    void stagingPathListenerNotifiedOnChange() throws IOException {
        Path customStaging = Files.createDirectory(tempDir.resolve("custom-staging"));
        ContextFileStore s = store();
        List<Integer> calls = new ArrayList<>();
        s.addStagingPathListener(() -> calls.add(1));

        s.setStagingPath(customStaging);

        assertEquals(1, calls.size());
    }

    @Test
    void stageSelectedOverwritesExisting() throws IOException {
        Path file = Files.createFile(tempDir.resolve("test.md"));
        Files.writeString(file, "new content");
        Path staging = tempDir.resolve("staging");
        Files.createDirectories(staging);
        Files.writeString(staging.resolve("test.md"), "old content");

        ContextFileStore s = new ContextFileStore(
                tempDir.resolve("context-files.json").toString(),
                staging.toString());

        s.stageSelected(List.of(file.toAbsolutePath()));
        assertEquals("new content", Files.readString(staging.resolve("test.md")));
    }
}
