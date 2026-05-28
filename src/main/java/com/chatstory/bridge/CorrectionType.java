package com.chatstory.bridge;

public enum CorrectionType {

    CONTEXT_LEAKAGE(
            "Context Leakage",
            "Do not rewrite the beat, but there is a context leakage problem with: "),
    BAD_WRITING(
            "Bad Writing",
            "Do not rewrite the beat, but this is bad writing: "),
    RE_EVALUATE(
            "Re-evaluate",
            "Do not rewrite the beat, but re-evaluate this text you generated: ");

    public static final String REDO_PROMPT = "Please redo the last story beat";

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
        return prefix + selectedText;
    }
}
