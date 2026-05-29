package com.chatstory.bridge;

import com.chatstory.input.SceneInputSegment;
import com.chatstory.input.SceneInputSegmentType;
import com.chatstory.input.ScenePromptBuilder;

import java.util.List;

public enum CorrectionType {

    CONTEXT_LEAKAGE(
            "Context Leakage",
            "Do not rewrite the beat, but there is a context leakage problem with: "),
    BAD_WRITING(
            "Bad Writing",
            "Do not rewrite the beat, but this is bad writing: "),
    RE_EVALUATE(
            "Re-evaluate",
            "Do not rewrite the beat, but re-evaluate this text you generated: "),
    CONTINUITY_ERROR(
            "Continuity Error",
            "Do not rewrite the beat, but there is a continuity error with: "),
    MAKE_CANON(
            "Make Canon",
            "Do not rewrite anything, but make the following information canonical: "),
    POV_DIALOGUE(
            "POV Dialogue",
            "Do not rewrite the beat, but you invented POV character dialogue, which is not allowed: "),
    BEAT_RESET(
            "Beat Reset",
            "Do not rewrite the beat, but you reset the beat, which is not allowed: "),
    HEADER_MISSING(
            "Header Missing",
            "Do not rewrite the beat, but the Day # Beat # header is incorrect: ");

    public static final String REDO_PROMPT      = direction("Redo the last story beat");
    public static final String END_SCENE_PROMPT = direction("End the scene");

    private static String direction(String text) {
        return new ScenePromptBuilder().build(
                List.of(new SceneInputSegment(SceneInputSegmentType.DIRECTION, text)));
    }

    private final String label;
    private final String prefix;

    CorrectionType(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

    public String menuLabel() {
        return label;
    }

    public String buildPrompt(String selectedText) {
        if (selectedText == null) {
            throw new IllegalArgumentException("selectedText must not be null");
        }
        return direction(prefix + selectedText);
    }
}
