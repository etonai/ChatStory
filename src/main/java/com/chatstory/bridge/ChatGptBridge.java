package com.chatstory.bridge;

import com.chatstory.AppState;
import com.chatstory.browser.DomBridge;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cef.browser.CefBrowser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ChatGptBridge implements ChatBridge {

    public static final int USER_MESSAGE_CONFIRM_TIMEOUT_MS = 5000;
    public static final int SEND_OPERATION_TIMEOUT_MS = 10000;

    private static final Gson GSON = new Gson();

    private final DomBridge domBridge;
    private final CefBrowser browser;
    private final AppState appState;
    private final RequestIdGenerator requestIds = new RequestIdGenerator();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "chatgpt-bridge-timeouts");
        t.setDaemon(true);
        return t;
    });

    private final Object lock = new Object();

    private long activeRequestId = 0L;
    private ResponseListener activeListener;
    private String activePrompt;
    private ScheduledFuture<?> activeTimeout;
    private boolean injectOnly;
    private String injectScript;
    private String sendScript;
    private JsonObject selectors;

    public ChatGptBridge(DomBridge domBridge, CefBrowser browser, AppState appState) {
        this.domBridge = domBridge;
        this.browser = browser;
        this.appState = appState;

        domBridge.registerHandler("injectResult", this::handleInjectResult);
        domBridge.registerHandler("sendResult", this::handleSendResult);
        domBridge.registerHandler("error", this::handleErrorResult);
    }

    @Override
    public void sendPrompt(String prompt, ResponseListener listener) {
        startPrompt(prompt, listener, false);
    }

    public void testInjectPrompt(String prompt, ResponseListener listener) {
        startPrompt(prompt, listener, true);
    }

    private void startPrompt(String prompt, ResponseListener listener, boolean injectOnly) {
        String text = prompt == null ? "" : prompt;
        long requestId = requestIds.next();

        synchronized (lock) {
            if (!appState.isSendEnabled() || activeRequestId != 0L) {
                listener.onError(requestId, ErrorCodes.SEND_BUTTON_DISABLED,
                        "Application is not ready to send");
                return;
            }
            activeRequestId = requestId;
            activeListener = listener;
            activePrompt = text;
            this.injectOnly = injectOnly;
            activeTimeout = scheduler.schedule(
                    () -> timeoutRequest(requestId),
                    SEND_OPERATION_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS);
        }

        try {
            appState.transition(AppState.State.InjectingPrompt);
            JsonObject options = new JsonObject();
            options.addProperty("requestId", requestId);
            options.add("selectors", selectorSubset("promptEditor", "sendButton"));
            options.addProperty("text", text);
            executeFunction("/js/inject_prompt.js", "window.chatStoryInjectPrompt", options);
        } catch (Exception e) {
            failActive(requestId, ErrorCodes.PROMPT_INJECTION_FAILED, e.getMessage());
        }
    }

    private void handleInjectResult(BridgeMessage message, org.cef.callback.CefQueryCallback callback) {
        if (!isActive(message)) {
            callback.success("");
            return;
        }
        if (!message.isOk()) {
            failActive(message.getRequestId(), fallback(message.getErrorCode(), ErrorCodes.PROMPT_INJECTION_FAILED),
                    fallback(message.getMessage(), "Prompt injection failed"));
            callback.success("");
            return;
        }

        if (isInjectOnly()) {
            ResponseListener listener;
            synchronized (lock) {
                listener = activeListener;
                clearActiveLocked();
            }
            appState.transition(AppState.State.Ready);
            if (listener != null) {
                listener.onPromptSubmitted(message.getRequestId());
            }
            callback.success("");
            return;
        }

        try {
            appState.transition(AppState.State.Sending);
            JsonObject options = new JsonObject();
            options.addProperty("requestId", message.getRequestId());
            options.add("selectors", selectorSubset("sendButton", "userMsg"));
            options.addProperty("expectedText", activePrompt);
            options.addProperty("timeoutMs", USER_MESSAGE_CONFIRM_TIMEOUT_MS);
            executeFunction("/js/trigger_send.js", "window.chatStoryTriggerSend", options);
        } catch (Exception e) {
            failActive(message.getRequestId(), ErrorCodes.SEND_CLICK_FAILED, e.getMessage());
        }
        callback.success("");
    }

    private void handleSendResult(BridgeMessage message, org.cef.callback.CefQueryCallback callback) {
        if (!isActive(message)) {
            callback.success("");
            return;
        }
        if (!message.isOk()) {
            failActive(message.getRequestId(), fallback(message.getErrorCode(), ErrorCodes.USER_MESSAGE_NOT_CONFIRMED),
                    fallback(message.getMessage(), "Prompt submission was not confirmed"));
            callback.success("");
            return;
        }

        ResponseListener listener;
        synchronized (lock) {
            listener = activeListener;
            clearActiveLocked();
        }
        appState.transition(AppState.State.Ready);
        if (listener != null) {
            listener.onPromptSubmitted(message.getRequestId());
        }
        callback.success("");
    }

    private void handleErrorResult(BridgeMessage message, org.cef.callback.CefQueryCallback callback) {
        if (!isActive(message)) {
            callback.success("");
            return;
        }
        failActive(message.getRequestId(), fallback(message.getErrorCode(), ErrorCodes.BRIDGE_HANDLER_FAILED),
                fallback(message.getMessage(), "Bridge reported an error"));
        callback.success("");
    }

    private boolean isActive(BridgeMessage message) {
        synchronized (lock) {
            if (message.getRequestId() == activeRequestId) {
                return true;
            }
            System.out.println("[ChatGptBridge] Dropping stale " + message.getType()
                    + " requestId=" + message.getRequestId()
                    + " activeRequestId=" + activeRequestId);
            return false;
        }
    }

    private void timeoutRequest(long requestId) {
        failActive(requestId, ErrorCodes.TIMEOUT, "Send operation timed out");
    }

    private void failActive(long requestId, String errorCode, String message) {
        ResponseListener listener;
        synchronized (lock) {
            if (requestId != activeRequestId) {
                System.out.println("[ChatGptBridge] Ignoring stale failure requestId="
                        + requestId + " activeRequestId=" + activeRequestId);
                return;
            }
            listener = activeListener;
            clearActiveLocked();
        }

        try {
            appState.transition(AppState.State.Error);
        } catch (IllegalStateException ignored) {
            // If browser navigation changed state underneath us, still report the error.
        }
        if (listener != null) {
            listener.onError(requestId, errorCode, message);
        }
    }

    private void clearActiveLocked() {
        if (activeTimeout != null) {
            activeTimeout.cancel(false);
        }
        activeRequestId = 0L;
        activeListener = null;
        activePrompt = null;
        activeTimeout = null;
        injectOnly = false;
    }

    private boolean isInjectOnly() {
        synchronized (lock) {
            return injectOnly;
        }
    }

    private void executeFunction(String resourcePath, String functionName, JsonObject options) {
        String script = loadScript(resourcePath) + "\n" + functionName + "(" + GSON.toJson(options) + ");";
        domBridge.execute(browser, script);
    }

    private JsonObject selectorSubset(String... keys) {
        JsonObject source = selectors();
        JsonObject subset = new JsonObject();
        for (String key : keys) {
            if (source.has(key)) {
                subset.add(key, source.get(key));
            }
        }
        return subset;
    }

    private JsonObject selectors() {
        if (selectors != null) return selectors;
        synchronized (this) {
            if (selectors != null) return selectors;
            selectors = JsonParser.parseString(loadResource("/js/chatgpt_selectors.json")).getAsJsonObject();
            return selectors;
        }
    }

    private String loadScript(String resourcePath) {
        if ("/js/inject_prompt.js".equals(resourcePath)) {
            if (injectScript == null) injectScript = loadResource(resourcePath);
            return injectScript;
        }
        if ("/js/trigger_send.js".equals(resourcePath)) {
            if (sendScript == null) sendScript = loadResource(resourcePath);
            return sendScript;
        }
        return loadResource(resourcePath);
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource " + path + ": " + e.getMessage(), e);
        }
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
