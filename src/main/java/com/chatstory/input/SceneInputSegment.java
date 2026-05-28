package com.chatstory.input;

public record SceneInputSegment(SceneInputSegmentType type, String text) {

    public SceneInputSegment {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        text = text == null ? "" : text;
    }
}
