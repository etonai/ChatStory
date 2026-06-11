package com.chatstory;

import com.chatstory.session.SnapshotFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotFileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void absentConfigStartsWithNoLastDirectory() {
        SnapshotFileStore store = new SnapshotFileStore(tempDir.resolve("snapshot-file.json").toString());

        assertNull(store.getLastDirectory());
    }

    @Test
    void lastDirectoryPersistsAcrossInstances() throws Exception {
        Path config = tempDir.resolve("snapshot-file.json");
        Path directory = Files.createDirectory(tempDir.resolve("snapshots"));

        SnapshotFileStore first = new SnapshotFileStore(config.toString());
        first.setLastDirectory(directory);

        SnapshotFileStore second = new SnapshotFileStore(config.toString());
        assertEquals(directory.toAbsolutePath(), second.getLastDirectory());
    }
}
