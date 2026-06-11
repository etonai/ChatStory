package com.chatstory.session;

import java.util.ArrayList;
import java.util.List;

public class ApplicationSnapshot {

    public int schemaVersion = 1;
    public String savedAt;
    public Controllers controllers;
    public Context context;
    public Rules rules;
    public Canon canon;
    public Picture picture;
    public Integer redoCount;
    public String appMode;
    public String nativeTheme;

    public static class Controllers {
        public String sessionControllerFile;
        public String intermediateControllerFile;
        public String finalControllerFile;
    }

    public static class Context {
        public List<String> files = new ArrayList<>();
        public List<String> checkedFiles = new ArrayList<>();
        public String lastDirectory;
        public String stagingPath;
    }

    public static class Rules {
        public List<String> files = new ArrayList<>();
        public String lastDirectory;
    }

    public static class Canon {
        public String canonFolder;
    }

    public static class Picture {
        public String imageFile;
    }
}
