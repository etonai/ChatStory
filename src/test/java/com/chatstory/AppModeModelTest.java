package com.chatstory;

import com.chatstory.mode.AppMode;
import com.chatstory.mode.AppModeModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppModeModelTest {

    @Test
    void defaultsToStoryMode() {
        assertEquals(AppMode.STORY, new AppModeModel().current());
    }

    @Test
    void notifiesModeChanges() {
        AppModeModel model = new AppModeModel();
        List<AppMode> seen = new ArrayList<>();
        model.addListener((previous, current) -> seen.add(current));

        model.setMode(AppMode.UNASSISTED);

        assertEquals(AppMode.UNASSISTED, model.current());
        assertEquals(List.of(AppMode.UNASSISTED), seen);
    }
}
