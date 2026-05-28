package com.chatstory.canon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CanonStore {

    private final List<String> entries = new ArrayList<>();

    public void add(String responseText) {
        if (responseText == null || responseText.isBlank()) return;
        entries.add(responseText);
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public String export() {
        return String.join("\n\n---\n\n", entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
