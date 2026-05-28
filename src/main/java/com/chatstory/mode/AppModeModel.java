package com.chatstory.mode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppModeModel {

    public interface Listener {
        void onModeChanged(AppMode previous, AppMode current);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private AppMode mode = AppMode.STORY;

    public synchronized AppMode current() {
        return mode;
    }

    public void setMode(AppMode next) {
        AppMode previous;
        synchronized (this) {
            if (next == null || next == mode) {
                return;
            }
            previous = mode;
            mode = next;
        }
        for (Listener listener : listeners) {
            listener.onModeChanged(previous, next);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
