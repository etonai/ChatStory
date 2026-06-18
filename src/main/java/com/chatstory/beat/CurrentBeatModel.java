package com.chatstory.beat;

public class CurrentBeatModel {

    public enum ResultKind {
        FIRST_BEAT,
        REPLACED,
        ROLLED_OVER
    }

    public static final class UpdateResult {
        public final ResultKind kind;
        public final String textToAppend;

        UpdateResult(ResultKind kind, String textToAppend) {
            this.kind = kind;
            this.textToAppend = textToAppend;
        }
    }

    private Integer beatNumber = null;
    private String text = null;
    private boolean appendedToCanon = false;

    public synchronized UpdateResult update(int newBeatNumber, String responseText) {
        System.out.println("[CurrentBeatModel] update enter newBeat=" + newBeatNumber
                + " currentBeat=" + beatNumber);
        UpdateResult result;
        if (beatNumber == null) {
            beatNumber = newBeatNumber;
            text = responseText;
            appendedToCanon = false;
            result = new UpdateResult(ResultKind.FIRST_BEAT, null);
        } else if (newBeatNumber == beatNumber) {
            text = responseText;
            appendedToCanon = false;
            result = new UpdateResult(ResultKind.REPLACED, null);
        } else {
            String oldText = text;
            beatNumber = newBeatNumber;
            text = responseText;
            appendedToCanon = false;
            result = new UpdateResult(ResultKind.ROLLED_OVER, oldText);
        }
        System.out.println("[CurrentBeatModel] update exit kind=" + result.kind);
        return result;
    }

    public synchronized void markAppended() {
        appendedToCanon = true;
    }

    public synchronized boolean hasUnappendedBeat() {
        return beatNumber != null && !appendedToCanon;
    }

    public synchronized String getText() {
        return text;
    }

    public synchronized Integer getBeatNumber() {
        return beatNumber;
    }

    public synchronized void clear() {
        beatNumber = null;
        text = null;
        appendedToCanon = false;
    }
}
