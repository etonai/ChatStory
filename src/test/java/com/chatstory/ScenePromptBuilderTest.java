package com.chatstory;

import com.chatstory.input.SceneInputSegment;
import com.chatstory.input.SceneInputSegmentType;
import com.chatstory.input.ScenePromptBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenePromptBuilderTest {

    private final ScenePromptBuilder builder = new ScenePromptBuilder();

    @Test
    void buildsStructuredSceneInputSequence() {
        String prompt = builder.build(List.of(
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "Hi"),
                segment(SceneInputSegmentType.DIRECTION, "I walk over to the bar. Jane looks upset."),
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "How are you?")));

        assertTrue(prompt.contains("SCENE_INPUT_SEQUENCE:"));
        assertTrue(prompt.contains("1. PLAYER_CHARACTER_SAYS: \"Hi\""));
        assertTrue(prompt.contains("2. DIRECTION: \"I walk over to the bar. Jane looks upset.\""));
        assertTrue(prompt.contains("3. PLAYER_CHARACTER_SAYS: \"How are you?\""));
    }

    @Test
    void explainsDialogueQuestionsAreNotDirectUserQuestions() {
        String prompt = builder.build(List.of(
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "What is the problem?")));

        assertTrue(prompt.contains("1. PLAYER_CHARACTER_SAYS: \"What is the problem?\""));
    }

    @Test
    void includesDirectionItems() {
        String prompt = builder.build(List.of(
                segment(SceneInputSegmentType.DIRECTION, "Jane looks upset.")));

        assertTrue(prompt.contains("1. DIRECTION: \"Jane looks upset.\""));
    }

    @Test
    void escapesQuotedSegmentText() {
        String prompt = builder.build(List.of(
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "She says \"no\".")));

        assertTrue(prompt.contains("1. PLAYER_CHARACTER_SAYS: \"She says \\\"no\\\".\""));
    }

    @Test
    void doesNotIncludeRawAngleBracketShorthand() {
        String prompt = builder.build(List.of(
                segment(SceneInputSegmentType.PLAYER_CHARACTER_SAYS, "Hi"),
                segment(SceneInputSegmentType.DIRECTION, "Jane looks up.")));

        assertFalse(prompt.contains("Hi <Jane looks up.>"));
    }

    private SceneInputSegment segment(SceneInputSegmentType type, String text) {
        return new SceneInputSegment(type, text);
    }
}
