package com.chatstory;

import com.chatstory.bridge.CorrectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrectionTypeTest {

    @Test
    void contextLeakagePrefixPlusText() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite the beat, but there is a context leakage problem with: some text\"",
                CorrectionType.CONTEXT_LEAKAGE.buildPrompt("some text"));
    }

    @Test
    void badWritingPrefixPlusText() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite the beat, but this is bad writing: some text\"",
                CorrectionType.BAD_WRITING.buildPrompt("some text"));
    }

    @Test
    void reEvaluatePrefixPlusText() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite the beat, but re-evaluate this text you generated: some text\"",
                CorrectionType.RE_EVALUATE.buildPrompt("some text"));
    }

    @Test
    void emptySelectedTextProducesPrefixOnly() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite the beat, but this is bad writing: \"",
                CorrectionType.BAD_WRITING.buildPrompt(""));
    }

    @Test
    void nullSelectedTextThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> CorrectionType.CONTEXT_LEAKAGE.buildPrompt(null));
    }

    @Test
    void continuityErrorPrefixPlusText() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite the beat, but there is a continuity error with: some text\"",
                CorrectionType.CONTINUITY_ERROR.buildPrompt("some text"));
    }

    @Test
    void makeCanonPrefixPlusText() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite anything, but make the following information canonical: some text\"",
                CorrectionType.MAKE_CANON.buildPrompt("some text"));
    }

    @Test
    void povDialoguePrefixPlusText() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite the beat, but you invented POV character dialogue, which is not allowed: some text\"",
                CorrectionType.POV_DIALOGUE.buildPrompt("some text"));
    }

    @Test
    void beatResetPrefixPlusText() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite the beat, but you reset the beat, which is not allowed: some text\"",
                CorrectionType.BEAT_RESET.buildPrompt("some text"));
    }

    @Test
    void headerMissingPrefixPlusText() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Do not rewrite the beat, but the Day # Beat # header is incorrect: some text\"",
                CorrectionType.HEADER_MISSING.buildPrompt("some text"));
    }

    @Test
    void menuLabels() {
        assertEquals("Context Leakage", CorrectionType.CONTEXT_LEAKAGE.menuLabel());
        assertEquals("Bad Writing", CorrectionType.BAD_WRITING.menuLabel());
        assertEquals("Re-evaluate", CorrectionType.RE_EVALUATE.menuLabel());
        assertEquals("Continuity Error", CorrectionType.CONTINUITY_ERROR.menuLabel());
        assertEquals("Make Canon", CorrectionType.MAKE_CANON.menuLabel());
        assertEquals("POV Dialogue", CorrectionType.POV_DIALOGUE.menuLabel());
        assertEquals("Beat Reset", CorrectionType.BEAT_RESET.menuLabel());
        assertEquals("Header Missing", CorrectionType.HEADER_MISSING.menuLabel());
    }

    @Test
    void redoPromptIsFormattedDirection() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"Redo the last story beat\"",
                CorrectionType.REDO_PROMPT);
    }

    @Test
    void endScenePromptIsFormattedDirection() {
        assertEquals(
                "SCENE_INPUT_SEQUENCE:\n1. DIRECTION: \"End the scene\"",
                CorrectionType.END_SCENE_PROMPT);
    }
}
