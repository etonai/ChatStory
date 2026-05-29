package com.chatstory;

import com.chatstory.beat.BeatParser;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class BeatParserTest {

    @Test
    void beatOnFirstLine() {
        OptionalInt result = BeatParser.parse("Beat 1\nSome story content.");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    void beatOnSecondLine() {
        OptionalInt result = BeatParser.parse("Day 1\nBeat 5\nMore content.");
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    void beatOnThirdLineIsIgnored() {
        OptionalInt result = BeatParser.parse("Line one\nLine two\nBeat 3");
        assertFalse(result.isPresent());
    }

    @Test
    void noMarkerReturnsEmpty() {
        OptionalInt result = BeatParser.parse("Just some assistant text with no beat marker.");
        assertFalse(result.isPresent());
    }

    @Test
    void nullInputReturnsEmpty() {
        assertFalse(BeatParser.parse(null).isPresent());
    }

    @Test
    void blankInputReturnsEmpty() {
        assertFalse(BeatParser.parse("   ").isPresent());
    }

    @Test
    void largeBeaNumber() {
        OptionalInt result = BeatParser.parse("Beat 100\nContent.");
        assertTrue(result.isPresent());
        assertEquals(100, result.getAsInt());
    }

    @Test
    void invalidMarkerNoDigit() {
        assertFalse(BeatParser.parse("Beat abc").isPresent());
    }

    @Test
    void beatWithLeadingWhitespace() {
        OptionalInt result = BeatParser.parse("  Beat 7  \nContent.");
        assertTrue(result.isPresent());
        assertEquals(7, result.getAsInt());
    }

    @Test
    void singleLineResponseWithBeat() {
        OptionalInt result = BeatParser.parse("Beat 2");
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    void beatMidLine() {
        OptionalInt result = BeatParser.parse("Day 1 Beat 4 extra text");
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());
    }

    @Test
    void wordEndingInBeatDoesNotMatch() {
        assertFalse(BeatParser.parse("Downbeat 1\nContent.").isPresent());
    }
}
