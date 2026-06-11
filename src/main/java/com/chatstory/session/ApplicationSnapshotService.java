package com.chatstory.session;

import com.chatstory.canon.CanonFolderStore;
import com.chatstory.context.ContextFileStore;
import com.chatstory.controller.FinalControllerStore;
import com.chatstory.controller.IntermediateControllerStore;
import com.chatstory.controller.SessionControllerStore;
import com.chatstory.mode.AppMode;
import com.chatstory.mode.AppModeModel;
import com.chatstory.picture.PictureFileStore;
import com.chatstory.rules.RulesFileStore;
import com.chatstory.theme.NativeTheme;
import com.chatstory.theme.NativeThemeModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ApplicationSnapshotService {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final SessionControllerStore sessionControllerStore;
    private final IntermediateControllerStore intermediateControllerStore;
    private final FinalControllerStore finalControllerStore;
    private final ContextFileStore contextFileStore;
    private final RulesFileStore rulesFileStore;
    private final CanonFolderStore canonFolderStore;
    private final PictureFileStore pictureFileStore;
    private final RedoCountStore redoCountStore;
    private final AppModeModel modeModel;
    private final NativeThemeModel themeModel;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ApplicationSnapshotService(SessionControllerStore sessionControllerStore,
                                      IntermediateControllerStore intermediateControllerStore,
                                      FinalControllerStore finalControllerStore,
                                      ContextFileStore contextFileStore,
                                      RulesFileStore rulesFileStore,
                                      CanonFolderStore canonFolderStore,
                                      PictureFileStore pictureFileStore,
                                      RedoCountStore redoCountStore,
                                      AppModeModel modeModel,
                                      NativeThemeModel themeModel) {
        this.sessionControllerStore = sessionControllerStore;
        this.intermediateControllerStore = intermediateControllerStore;
        this.finalControllerStore = finalControllerStore;
        this.contextFileStore = contextFileStore;
        this.rulesFileStore = rulesFileStore;
        this.canonFolderStore = canonFolderStore;
        this.pictureFileStore = pictureFileStore;
        this.redoCountStore = redoCountStore;
        this.modeModel = modeModel;
        this.themeModel = themeModel;
    }

    public ApplicationSnapshot capture() {
        ApplicationSnapshot snapshot = new ApplicationSnapshot();
        snapshot.schemaVersion = CURRENT_SCHEMA_VERSION;
        snapshot.savedAt = Instant.now().toString();

        snapshot.controllers = new ApplicationSnapshot.Controllers();
        snapshot.controllers.sessionControllerFile = toString(sessionControllerStore.getControllerFile());
        snapshot.controllers.intermediateControllerFile = toString(intermediateControllerStore.getControllerFile());
        snapshot.controllers.finalControllerFile = toString(finalControllerStore.getControllerFile());

        snapshot.context = new ApplicationSnapshot.Context();
        snapshot.context.files = toStrings(contextFileStore.getEntries());
        snapshot.context.checkedFiles = toStrings(contextFileStore.getCheckedEntries());
        snapshot.context.lastDirectory = toString(contextFileStore.getLastDirectory());
        snapshot.context.stagingPath = toString(contextFileStore.getStagingPath());

        snapshot.rules = new ApplicationSnapshot.Rules();
        snapshot.rules.files = toStrings(rulesFileStore.getEntries());
        snapshot.rules.lastDirectory = toString(rulesFileStore.getLastDirectory());

        snapshot.canon = new ApplicationSnapshot.Canon();
        snapshot.canon.canonFolder = toString(canonFolderStore.getCanonFolder());

        snapshot.picture = new ApplicationSnapshot.Picture();
        snapshot.picture.imageFile = toString(pictureFileStore.getImageFile());

        snapshot.redoCount = redoCountStore.getCount();
        snapshot.appMode = modeModel.current().name();
        snapshot.nativeTheme = themeModel.current().name();

        return snapshot;
    }

    public void save(Path file) throws ApplicationSnapshotException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                gson.toJson(capture(), writer);
            }
        } catch (IOException e) {
            throw new ApplicationSnapshotException("Could not save snapshot: " + e.getMessage(), e);
        }
    }

    public ApplicationSnapshot load(Path file) throws ApplicationSnapshotException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ApplicationSnapshot snapshot = gson.fromJson(reader, ApplicationSnapshot.class);
            validate(snapshot);
            return snapshot;
        } catch (JsonSyntaxException e) {
            throw new ApplicationSnapshotException("Snapshot file is not valid JSON.", e);
        } catch (IOException e) {
            throw new ApplicationSnapshotException("Could not read snapshot: " + e.getMessage(), e);
        }
    }

    public void loadAndApply(Path file) throws ApplicationSnapshotException {
        ApplicationSnapshot snapshot = load(file);
        apply(snapshot);
    }

    public void apply(ApplicationSnapshot snapshot) throws ApplicationSnapshotException {
        validate(snapshot);

        if (snapshot.controllers != null) {
            sessionControllerStore.setControllerFile(toPath(snapshot.controllers.sessionControllerFile));
            intermediateControllerStore.setControllerFile(toPath(snapshot.controllers.intermediateControllerFile));
            finalControllerStore.setControllerFile(toPath(snapshot.controllers.finalControllerFile));
        }

        if (snapshot.context != null) {
            contextFileStore.replaceAll(
                    toPaths(snapshot.context.files),
                    toPaths(snapshot.context.checkedFiles),
                    toPath(snapshot.context.lastDirectory),
                    toPath(snapshot.context.stagingPath));
        }

        if (snapshot.rules != null) {
            rulesFileStore.replaceAll(
                    toPaths(snapshot.rules.files),
                    toPath(snapshot.rules.lastDirectory));
        }

        if (snapshot.canon != null) {
            canonFolderStore.setCanonFolder(toPath(snapshot.canon.canonFolder));
        }

        if (snapshot.picture != null) {
            pictureFileStore.setImageFile(toPath(snapshot.picture.imageFile));
        }

        if (snapshot.redoCount != null) {
            redoCountStore.setCount(snapshot.redoCount);
        }

        if (snapshot.appMode != null && !snapshot.appMode.isBlank()) {
            modeModel.setMode(AppMode.valueOf(snapshot.appMode));
        }

        if (snapshot.nativeTheme != null && !snapshot.nativeTheme.isBlank()) {
            themeModel.setTheme(NativeTheme.valueOf(snapshot.nativeTheme));
        }
    }

    public void validate(ApplicationSnapshot snapshot) throws ApplicationSnapshotException {
        if (snapshot == null) {
            throw new ApplicationSnapshotException("Snapshot file is empty or missing its root object.");
        }
        if (snapshot.schemaVersion <= 0) {
            throw new ApplicationSnapshotException("Snapshot is missing a supported schema version.");
        }
        if (snapshot.schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new ApplicationSnapshotException("Snapshot schema version " + snapshot.schemaVersion
                    + " is newer than this app supports.");
        }
        if (snapshot.redoCount != null && snapshot.redoCount < 0) {
            throw new ApplicationSnapshotException("Snapshot redo count cannot be negative.");
        }
        validateEnum(snapshot.appMode, AppMode.class, "appMode");
        validateEnum(snapshot.nativeTheme, NativeTheme.class, "nativeTheme");
        validatePaths(snapshot);
    }

    private <E extends Enum<E>> void validateEnum(String value, Class<E> enumType, String field)
            throws ApplicationSnapshotException {
        if (value == null || value.isBlank()) return;
        try {
            Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            throw new ApplicationSnapshotException("Snapshot has unsupported " + field + ": " + value);
        }
    }

    private void validatePaths(ApplicationSnapshot snapshot) throws ApplicationSnapshotException {
        if (snapshot.controllers != null) {
            validatePath(snapshot.controllers.sessionControllerFile, "controllers.sessionControllerFile");
            validatePath(snapshot.controllers.intermediateControllerFile, "controllers.intermediateControllerFile");
            validatePath(snapshot.controllers.finalControllerFile, "controllers.finalControllerFile");
        }
        if (snapshot.context != null) {
            validatePathList(snapshot.context.files, "context.files");
            validatePathList(snapshot.context.checkedFiles, "context.checkedFiles");
            validatePath(snapshot.context.lastDirectory, "context.lastDirectory");
            validatePath(snapshot.context.stagingPath, "context.stagingPath");
        }
        if (snapshot.rules != null) {
            validatePathList(snapshot.rules.files, "rules.files");
            validatePath(snapshot.rules.lastDirectory, "rules.lastDirectory");
        }
        if (snapshot.canon != null) {
            validatePath(snapshot.canon.canonFolder, "canon.canonFolder");
        }
        if (snapshot.picture != null) {
            validatePath(snapshot.picture.imageFile, "picture.imageFile");
        }
    }

    private void validatePathList(List<String> values, String field) throws ApplicationSnapshotException {
        if (values == null) return;
        for (String value : values) {
            validatePath(value, field);
        }
    }

    private void validatePath(String value, String field) throws ApplicationSnapshotException {
        if (value == null || value.isBlank()) return;
        try {
            Path.of(value);
        } catch (InvalidPathException e) {
            throw new ApplicationSnapshotException("Snapshot has invalid path in " + field + ": " + value);
        }
    }

    private String toString(Path path) {
        return path == null ? null : path.toString();
    }

    private List<String> toStrings(List<Path> paths) {
        List<String> result = new ArrayList<>();
        for (Path path : paths) {
            result.add(path.toString());
        }
        return result;
    }

    private Path toPath(String value) {
        return value == null || value.isBlank() ? null : Path.of(value).toAbsolutePath();
    }

    private List<Path> toPaths(List<String> values) {
        List<Path> result = new ArrayList<>();
        if (values == null) return result;
        for (String value : values) {
            Path path = toPath(value);
            if (path != null) result.add(path);
        }
        return result;
    }
}
