package com.chatstory.session;

public class ApplicationSnapshotException extends Exception {

    public ApplicationSnapshotException(String message) {
        super(message);
    }

    public ApplicationSnapshotException(String message, Throwable cause) {
        super(message, cause);
    }
}
