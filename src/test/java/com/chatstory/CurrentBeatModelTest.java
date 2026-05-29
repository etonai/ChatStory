package com.chatstory;

import com.chatstory.beat.CurrentBeatModel;
import com.chatstory.beat.CurrentBeatModel.ResultKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrentBeatModelTest {

    private CurrentBeatModel model;

    @BeforeEach
    void setUp() {
        model = new CurrentBeatModel();
    }

    @Test
    void firstBeatReturnsFirstBeatKind() {
        CurrentBeatModel.UpdateResult result = model.update(1, "Beat 1 text");
        assertEquals(ResultKind.FIRST_BEAT, result.kind);
        assertNull(result.textToAppend);
    }

    @Test
    void firstBeatTrackedCorrectly() {
        model.update(1, "Beat 1 text");
        assertEquals(1, model.getBeatNumber());
        assertEquals("Beat 1 text", model.getText());
        assertTrue(model.hasUnappendedBeat());
    }

    @Test
    void sameBeatNumberReturnsReplaced() {
        model.update(1, "Beat 1 original");
        CurrentBeatModel.UpdateResult result = model.update(1, "Beat 1 rewrite");
        assertEquals(ResultKind.REPLACED, result.kind);
        assertNull(result.textToAppend);
    }

    @Test
    void sameBeatRewriteUpdatesText() {
        model.update(1, "Beat 1 original");
        model.update(1, "Beat 1 rewrite");
        assertEquals("Beat 1 rewrite", model.getText());
        assertEquals(1, model.getBeatNumber());
    }

    @Test
    void newBeatNumberReturnsRolledOver() {
        model.update(1, "Beat 1 text");
        CurrentBeatModel.UpdateResult result = model.update(2, "Beat 2 text");
        assertEquals(ResultKind.ROLLED_OVER, result.kind);
        assertEquals("Beat 1 text", result.textToAppend);
    }

    @Test
    void rolledOverTracksNewBeat() {
        model.update(1, "Beat 1 text");
        model.update(2, "Beat 2 text");
        assertEquals(2, model.getBeatNumber());
        assertEquals("Beat 2 text", model.getText());
        assertTrue(model.hasUnappendedBeat());
    }

    @Test
    void markAppendedClearsUnappendedFlag() {
        model.update(1, "Beat 1 text");
        assertTrue(model.hasUnappendedBeat());
        model.markAppended();
        assertFalse(model.hasUnappendedBeat());
    }

    @Test
    void sameBeatRewriteResetsAppendedFlag() {
        model.update(1, "Beat 1 text");
        model.markAppended();
        assertFalse(model.hasUnappendedBeat());
        model.update(1, "Beat 1 rewrite");
        assertTrue(model.hasUnappendedBeat());
    }

    @Test
    void noCurrentBeatMeansNothingUnappended() {
        assertFalse(model.hasUnappendedBeat());
    }

    @Test
    void clearResetsAllState() {
        model.update(3, "Beat 3 text");
        model.clear();
        assertNull(model.getBeatNumber());
        assertNull(model.getText());
        assertFalse(model.hasUnappendedBeat());
    }

    @Test
    void rolledOverTextIsOldBeatText() {
        model.update(1, "Old beat content");
        CurrentBeatModel.UpdateResult result = model.update(2, "New beat content");
        assertEquals("Old beat content", result.textToAppend);
    }

    @Test
    void multipleRolloversCarryCorrectText() {
        model.update(1, "Beat 1");
        model.update(2, "Beat 2");
        CurrentBeatModel.UpdateResult result = model.update(3, "Beat 3");
        assertEquals("Beat 2", result.textToAppend);
        assertEquals(3, model.getBeatNumber());
    }
}
