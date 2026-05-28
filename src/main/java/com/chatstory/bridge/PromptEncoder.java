package com.chatstory.bridge;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes prompt text before it is passed into JavaScript.
 * User text must never be inserted into JS via raw string replacement.
 */
public final class PromptEncoder {

    private static final Gson GSON = new Gson();

    private PromptEncoder() {}

    /** Returns a quoted JSON string literal, ready to use as a JavaScript argument. */
    public static String toJsonStringLiteral(String text) {
        return GSON.toJson(text == null ? "" : text);
    }

    /** Fallback encoding if JSON string arguments prove unreliable in a JS context. */
    public static String toBase64(String text) {
        return Base64.getEncoder().encodeToString(
                (text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
    }
}

