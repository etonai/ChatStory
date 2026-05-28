package com.chatstory;

import com.chatstory.bridge.CorrectionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrectionTypeTest {

    @Test
    void contextLeakagePrefixPlusText() {
        assertEquals(
                "Do not rewrite the beat, but there is a context leakage problem with: some text",
                CorrectionType.CONTEXT_LEAKAGE.buildPrompt("some text"));
    }

    @Test
    void badWritingPrefixPlusText() {
        assertEquals(
                "Do not rewrite the beat, but this is bad writing: some text",
                CorrectionType.BAD_WRITING.buildPrompt("some text"));
    }

    @Test
    void reEvaluatePrefixPlusText() {
        assertEquals(
                "Do not rewrite the beat, but re-evaluate this text you generated: some text",
                CorrectionType.RE_EVALUATE.buildPrompt("some text"));
    }

    @Test
    void emptySelectedTextProducesPrefixOnly() {
        assertEquals(
                "Do not rewrite the beat, but this is bad writing: ",
                CorrectionType.BAD_WRITING.buildPrompt(""));
    }

    @Test
    void nullSelectedTextThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> CorrectionType.CONTEXT_LEAKAGE.buildPrompt(null));
    }

    @Test
    void menuLabels() {
        assertEquals("Context Leakage", CorrectionType.CONTEXT_LEAKAGE.menuLabel());
        assertEquals("Bad Writing", CorrectionType.BAD_WRITING.menuLabel());
        assertEquals("Re-evaluate", CorrectionType.RE_EVALUATE.menuLabel());
    }

    @Test
    void redoPromptConstant() {
        assertEquals("Please redo the last story beat", CorrectionType.REDO_PROMPT);
    }
}
