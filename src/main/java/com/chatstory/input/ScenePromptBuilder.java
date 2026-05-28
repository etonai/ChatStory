package com.chatstory.input;

import java.util.List;

public class ScenePromptBuilder {

    public String build(List<SceneInputSegment> segments) {
        List<SceneInputSegment> safeSegments = segments == null ? List.of() : segments;

        StringBuilder prompt = new StringBuilder();
        prompt.append("SCENE_INPUT_SEQUENCE:\n");

        for (int i = 0; i < safeSegments.size(); i++) {
            SceneInputSegment segment = safeSegments.get(i);
            prompt.append(i + 1)
                    .append(". ")
                    .append(segment.type())
                    .append(": \"")
                    .append(escape(segment.text()))
                    .append("\"\n");
        }

        return prompt.toString().trim();
    }

    private String escape(String text) {
        return (text == null ? "" : text)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
