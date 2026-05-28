package com.chatstory;

import com.chatstory.canon.CanonStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanonStoreTest {

    @Test
    void emptyOnConstruction() {
        assertTrue(new CanonStore().isEmpty());
    }

    @Test
    void addAndRetrieve() {
        CanonStore store = new CanonStore();
        store.add("Hello");
        assertFalse(store.isEmpty());
        assertEquals(List.of("Hello"), store.getEntries());
    }

    @Test
    void nullAddIsIgnored() {
        CanonStore store = new CanonStore();
        store.add(null);
        assertTrue(store.isEmpty());
    }

    @Test
    void blankAddIsIgnored() {
        CanonStore store = new CanonStore();
        store.add("   ");
        assertTrue(store.isEmpty());
    }

    @Test
    void emptyStringAddIsIgnored() {
        CanonStore store = new CanonStore();
        store.add("");
        assertTrue(store.isEmpty());
    }

    @Test
    void exportSingleEntry() {
        CanonStore store = new CanonStore();
        store.add("First entry");
        assertEquals("First entry", store.export());
    }

    @Test
    void exportMultipleEntries() {
        CanonStore store = new CanonStore();
        store.add("First");
        store.add("Second");
        assertEquals("First\n\n---\n\nSecond", store.export());
    }

    @Test
    void exportEmptyIsEmptyString() {
        assertEquals("", new CanonStore().export());
    }

    @Test
    void getEntriesIsUnmodifiable() {
        CanonStore store = new CanonStore();
        store.add("entry");
        assertThrows(UnsupportedOperationException.class, () -> store.getEntries().add("extra"));
    }

    @Test
    void duplicatesAllowed() {
        CanonStore store = new CanonStore();
        store.add("same");
        store.add("same");
        assertEquals(2, store.getEntries().size());
    }

    @Test
    void multipleEntriesPreserveOrder() {
        CanonStore store = new CanonStore();
        store.add("Alpha");
        store.add("Beta");
        store.add("Gamma");
        assertEquals(List.of("Alpha", "Beta", "Gamma"), store.getEntries());
    }
}
