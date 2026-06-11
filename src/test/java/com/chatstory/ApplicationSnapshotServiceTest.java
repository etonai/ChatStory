package com.chatstory;

import com.chatstory.canon.CanonFolderStore;
import com.chatstory.context.ContextFileStore;
import com.chatstory.controller.FinalControllerStore;
import com.chatstory.controller.IntermediateControllerStore;
import com.chatstory.controller.SessionControllerStore;
import com.chatstory.mode.AppMode;
import com.chatstory.mode.AppModeModel;
import com.chatstory.picture.PictureFileStore;
import com.chatstory.rules.RulesFileStore;
import com.chatstory.session.ApplicationSnapshot;
import com.chatstory.session.ApplicationSnapshotException;
import com.chatstory.session.ApplicationSnapshotService;
import com.chatstory.session.RedoCountStore;
import com.chatstory.theme.NativeTheme;
import com.chatstory.theme.NativeThemeModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationSnapshotServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadRoundTrip() throws Exception {
        Stores stores = stores("roundtrip");
        Path session = Files.createFile(tempDir.resolve("session.md"));
        Path intermediate = Files.createFile(tempDir.resolve("intermediate.md"));
        Path fin = Files.createFile(tempDir.resolve("final.md"));
        Path context = Files.createFile(tempDir.resolve("context.md"));
        Path rules = Files.createFile(tempDir.resolve("rules.md"));
        Path canon = Files.createDirectory(tempDir.resolve("canon"));
        Path image = Files.createFile(tempDir.resolve("picture.png"));
        Path staging = Files.createDirectory(tempDir.resolve("custom-staging"));

        stores.sessionControllerStore.setControllerFile(session);
        stores.intermediateControllerStore.setControllerFile(intermediate);
        stores.finalControllerStore.setControllerFile(fin);
        stores.contextFileStore.add(context);
        stores.contextFileStore.setChecked(context, true);
        stores.contextFileStore.setLastDirectory(tempDir);
        stores.contextFileStore.setStagingPath(staging);
        stores.rulesFileStore.add(rules);
        stores.rulesFileStore.setLastDirectory(tempDir);
        stores.canonFolderStore.setCanonFolder(canon);
        stores.pictureFileStore.setImageFile(image);
        stores.redoCountStore.setCount(7);
        stores.modeModel.setMode(AppMode.UNASSISTED);
        stores.themeModel.setTheme(NativeTheme.LIGHT);

        Path snapshotFile = tempDir.resolve("state.json");
        stores.service.save(snapshotFile);
        ApplicationSnapshot loaded = stores.service.load(snapshotFile);

        assertEquals(ApplicationSnapshotService.CURRENT_SCHEMA_VERSION, loaded.schemaVersion);
        assertEquals(session.toAbsolutePath().toString(), loaded.controllers.sessionControllerFile);
        assertEquals(List.of(context.toAbsolutePath().toString()), loaded.context.files);
        assertEquals(List.of(context.toAbsolutePath().toString()), loaded.context.checkedFiles);
        assertEquals(List.of(rules.toAbsolutePath().toString()), loaded.rules.files);
        assertEquals(canon.toAbsolutePath().toString(), loaded.canon.canonFolder);
        assertEquals(image.toAbsolutePath().toString(), loaded.picture.imageFile);
        assertEquals(7, loaded.redoCount);
        assertEquals("UNASSISTED", loaded.appMode);
        assertEquals("LIGHT", loaded.nativeTheme);
    }

    @Test
    void applyRestoresStoreBackedState() throws Exception {
        Stores stores = stores("apply");
        Path session = Files.createFile(tempDir.resolve("apply-session.md"));
        Path context = Files.createFile(tempDir.resolve("apply-context.md"));
        Path rules = Files.createFile(tempDir.resolve("apply-rules.md"));

        ApplicationSnapshot snapshot = new ApplicationSnapshot();
        snapshot.controllers = new ApplicationSnapshot.Controllers();
        snapshot.controllers.sessionControllerFile = session.toString();
        snapshot.context = new ApplicationSnapshot.Context();
        snapshot.context.files = List.of(context.toString());
        snapshot.context.checkedFiles = List.of(context.toString());
        snapshot.rules = new ApplicationSnapshot.Rules();
        snapshot.rules.files = List.of(rules.toString());
        snapshot.redoCount = 3;
        snapshot.appMode = "UNASSISTED";
        snapshot.nativeTheme = "LIGHT";

        stores.service.apply(snapshot);

        assertEquals(session.toAbsolutePath(), stores.sessionControllerStore.getControllerFile());
        assertEquals(List.of(context.toAbsolutePath()), stores.contextFileStore.getEntries());
        assertEquals(List.of(context.toAbsolutePath()), stores.contextFileStore.getCheckedEntries());
        assertEquals(List.of(rules.toAbsolutePath()), stores.rulesFileStore.getEntries());
        assertEquals(3, stores.redoCountStore.getCount());
        assertEquals(AppMode.UNASSISTED, stores.modeModel.current());
        assertEquals(NativeTheme.LIGHT, stores.themeModel.current());
    }

    @Test
    void malformedJsonThrowsSnapshotException() throws IOException {
        Stores stores = stores("malformed");
        Path file = tempDir.resolve("bad.json");
        Files.writeString(file, "{not valid json");

        assertThrows(ApplicationSnapshotException.class, () -> stores.service.load(file));
    }

    @Test
    void unsupportedFutureSchemaThrowsSnapshotException() {
        Stores stores = stores("future");
        ApplicationSnapshot snapshot = new ApplicationSnapshot();
        snapshot.schemaVersion = ApplicationSnapshotService.CURRENT_SCHEMA_VERSION + 1;

        assertThrows(ApplicationSnapshotException.class, () -> stores.service.apply(snapshot));
    }

    private Stores stores(String name) {
        Path base = tempDir.resolve(name);
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        ContextFileStore context = new ContextFileStore(
                base.resolve("context.json").toString(),
                base.resolve("staging").toString());
        RulesFileStore rules = new RulesFileStore(base.resolve("rules.json").toString());
        CanonFolderStore canon = new CanonFolderStore(base.resolve("canon.json").toString());
        PictureFileStore picture = new PictureFileStore(base.resolve("picture.json").toString());
        SessionControllerStore session = new SessionControllerStore(base.resolve("session.json").toString());
        IntermediateControllerStore intermediate = new IntermediateControllerStore(base.resolve("intermediate.json").toString());
        FinalControllerStore fin = new FinalControllerStore(base.resolve("final.json").toString());
        RedoCountStore redo = new RedoCountStore(base.resolve("redo.json").toString());
        AppModeModel mode = new AppModeModel();
        NativeThemeModel theme = new NativeThemeModel();

        ApplicationSnapshotService service = new ApplicationSnapshotService(
                session, intermediate, fin, context, rules, canon, picture, redo, mode, theme);
        return new Stores(service, session, intermediate, fin, context, rules, canon, picture, redo, mode, theme);
    }

    private record Stores(ApplicationSnapshotService service,
                          SessionControllerStore sessionControllerStore,
                          IntermediateControllerStore intermediateControllerStore,
                          FinalControllerStore finalControllerStore,
                          ContextFileStore contextFileStore,
                          RulesFileStore rulesFileStore,
                          CanonFolderStore canonFolderStore,
                          PictureFileStore pictureFileStore,
                          RedoCountStore redoCountStore,
                          AppModeModel modeModel,
                          NativeThemeModel themeModel) {}
}
