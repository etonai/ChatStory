package com.chatstory.beat;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeatParser {

    private static final Pattern BEAT_PATTERN = Pattern.compile("\\bBeat\\s+(\\d+)\\b");

    private BeatParser() {}

    public static OptionalInt parse(String responseText) {
        if (responseText == null || responseText.isBlank()) return OptionalInt.empty();
        String[] lines = responseText.split("\n", 3);
        for (int i = 0; i < Math.min(2, lines.length); i++) {
            Matcher m = BEAT_PATTERN.matcher(lines[i]);
            if (m.find()) {
                return OptionalInt.of(Integer.parseInt(m.group(1)));
            }
        }
        return OptionalInt.empty();
    }
}
