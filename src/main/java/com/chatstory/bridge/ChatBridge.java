package com.chatstory.bridge;

/** Implemented in DC003. */
public interface ChatBridge {
    void sendPrompt(String prompt, ResponseListener listener);
    void sendRawPrompt(String prompt, ResponseListener listener);
    void reset();
}
