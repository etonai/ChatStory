package com.chatstory.input;

import java.util.ArrayList;
import java.util.List;

public class SceneInputParser {

    public List<SceneInputSegment> parse(String input) {
        String text = input == null ? "" : input;
        List<SceneInputSegment> segments = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        SceneInputSegmentType currentType = SceneInputSegmentType.PLAYER_CHARACTER_SAYS;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '<' && currentType == SceneInputSegmentType.PLAYER_CHARACTER_SAYS) {
                addSegment(segments, currentType, buffer);
                currentType = SceneInputSegmentType.DIRECTION;
                continue;
            }
            if (ch == '>' && currentType == SceneInputSegmentType.DIRECTION) {
                addSegment(segments, currentType, buffer);
                currentType = SceneInputSegmentType.PLAYER_CHARACTER_SAYS;
                continue;
            }
            buffer.append(ch);
        }

        addSegment(segments, currentType, buffer);
        return List.copyOf(segments);
    }

    public List<SceneInputSegment> parseAsDirection(String input) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty()) {
            return List.of();
        }
        return List.of(new SceneInputSegment(SceneInputSegmentType.DIRECTION, text));
    }

    private void addSegment(List<SceneInputSegment> segments, SceneInputSegmentType type,
                            StringBuilder buffer) {
        String text = buffer.toString().trim();
        if (!text.isEmpty()) {
            segments.add(new SceneInputSegment(type, text));
        }
        buffer.setLength(0);
    }
}
