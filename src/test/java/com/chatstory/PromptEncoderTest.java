package com.chatstory;

import com.chatstory.bridge.PromptEncoder;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class PromptEncoderTest {

    @Test
    void jsonEncodesEmptyPrompt() {
        assertRoundTrips("");
    }

    @Test
    void jsonEncodesQuotes() {
        assertRoundTrips("She said, \"hello\" and 'goodbye'.");
    }

    @Test
    void jsonEncodesBackslashes() {
        assertRoundTrips("C:\\Users\\Story\\file.txt");
    }

    @Test
    void jsonEncodesNewlinesAndCarriageReturns() {
        assertRoundTrips("line one\nline two\r\nline three");
    }

    @Test
    void jsonEncodesMultiParagraphText() {
        assertRoundTrips("Paragraph one.\n\nParagraph two.\n\nParagraph three.");
    }

    @Test
    void jsonEncodesNonAsciiAndEmoji() {
        assertRoundTrips("cafe naive resume - snowman \u2603 - emoji \uD83D\uDE80");
    }

    @Test
    void jsonEncodesLongPrompt() {
        assertRoundTrips("x".repeat(5000));
    }

    @Test
    void nullJsonEncodesAsEmptyString() {
        assertEquals("", JsonParser.parseString(PromptEncoder.toJsonStringLiteral(null)).getAsString());
    }

    @Test
    void base64EncodesUtf8() {
        String text = "hello \uD83D\uDE80";
        String decoded = new String(Base64.getDecoder().decode(PromptEncoder.toBase64(text)),
                StandardCharsets.UTF_8);
        assertEquals(text, decoded);
    }

    private void assertRoundTrips(String text) {
        String encoded = PromptEncoder.toJsonStringLiteral(text);
        assertTrue(encoded.startsWith("\""));
        assertTrue(encoded.endsWith("\""));
        assertEquals(text, JsonParser.parseString(encoded).getAsString());
    }
}
