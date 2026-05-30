package com.chatstory.transcript;

import java.util.ArrayList;
import java.util.List;

public class TranscriptStore {

    private final List<String> entries = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public void add(String text) {
        if (text == null || text.isBlank()) return;
        entries.add(text);
        for (Runnable listener : listeners) listener.run();
    }

    public String export() {
        return String.join("\n\n---\n\n", entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }
}
