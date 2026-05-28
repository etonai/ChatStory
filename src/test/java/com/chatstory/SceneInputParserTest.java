package com.chatstory;

import com.chatstory.input.SceneInputParser;
import com.chatstory.input.SceneInputSegment;
import com.chatstory.input.SceneInputSegmentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneInputParserTest {

    private final SceneInputParser parser = new SceneInputParser();

    @Test
    void parsesAllDialogue() {
        assertSegments(parser.parse("What is the problem?"),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "What is the problem?"));
    }

    @Test
    void parsesAllDirection() {
        assertSegments(parser.parse("<I walk over to the bar. Jane looks upset.>"),
                segment(SceneInputSegmentType.DIRECTION, "I walk over to the bar. Jane looks upset."));
    }

    @Test
    void parsesMixedOrderedInput() {
        assertSegments(parser.parse(
                        "Hi <I walk over to the bar. Jane looks upset.> How are you? <Jane looks up at me>"),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "Hi"),
                segment(SceneInputSegmentType.DIRECTION, "I walk over to the bar. Jane looks upset."),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "How are you?"),
                segment(SceneInputSegmentType.DIRECTION, "Jane looks up at me"));
    }

    @Test
    void autoClosesMissingDirectionEnd() {
        assertSegments(parser.parse("Hi <I walk over to the bar"),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "Hi"),
                segment(SceneInputSegmentType.DIRECTION, "I walk over to the bar"));
    }

    @Test
    void treatsStrayClosingBracketAsDialogue() {
        assertSegments(parser.parse("Hi > there"),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "Hi > there"));
    }

    @Test
    void trimsWhitespaceAndSuppressesEmptySegments() {
        assertSegments(parser.parse("  Hi  <  >  <  Jane looks up.  >   "),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "Hi"),
                segment(SceneInputSegmentType.DIRECTION, "Jane looks up."));
    }

    @Test
    void keepsAdjacentDirectionSegmentsSeparate() {
        assertSegments(parser.parse("<Jane turns.><She pauses.>"),
                segment(SceneInputSegmentType.DIRECTION, "Jane turns."),
                segment(SceneInputSegmentType.DIRECTION, "She pauses."));
    }

    @Test
    void treatsNestedOpeningBracketInsideDirectionAsLiteralText() {
        assertSegments(parser.parse("Hi <Jane notices <something odd>.>"),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "Hi"),
                segment(SceneInputSegmentType.DIRECTION, "Jane notices <something odd"),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, ".>"));
    }

    @Test
    void parsesDirectionOverride() {
        assertSegments(parser.parseAsDirection("  Jane looks upset and avoids eye contact.  "),
                segment(SceneInputSegmentType.DIRECTION, "Jane looks upset and avoids eye contact."));
    }

    @Test
    void directionOverrideSuppressesEmptyInput() {
        assertTrue(parser.parseAsDirection("   ").isEmpty());
    }

    private SceneInputSegment segment(SceneInputSegmentType type, String text) {
        return new SceneInputSegment(type, text);
    }

    private void assertSegments(List<SceneInputSegment> actual, SceneInputSegment... expected) {
        assertEquals(List.of(expected), actual);
    }
}
