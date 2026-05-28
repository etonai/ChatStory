# ClaudeDC2Plan01: Proposed Scope for DevCycle 002

**Created:** 2026-05-28
**Author:** Claude
**Purpose:** Proposed scope for DevCycle 002 — Application Foundation.
Serves as input for creating `doc/planning/DevCycle002.md`.

---

## Context

DevCycle 001 produced a working spike in `com.chatstory.spike.Main` — a single file containing everything: JCEF init, window, router, load handler, and bridge handler. It proved the premise. DevCycle 002 replaces that spike with a proper, extensible application structure.

**DC001 API discoveries that affect DC002:**
- `CefMessageRouter` is in `org.cef.browser` (not `org.cef.network`)
- DevTools is `browser.openDevTools()` (not `showDevTools`)
- `CefMessageRouterHandlerAdapter` is in `org.cef.handler` and handles `getNativeRef` — use it rather than implementing `CefMessageRouterHandler` directly
- These are already correct in the spike; just carry them forward

---

## Goal

Replace the DC001 spike with a clean, extensible application scaffold. At the end of DC002:

- The spike package `com.chatstory.spike` is deleted
- The application is structured into the target package layout
- `AppState`, `UiThread`, `DomBridge`, and the bridge interfaces are in place
- `chatgpt_selectors.json` loads cleanly on startup
- Config file provides the target URL
- The status label reflects live `AppState` transitions
- Unit tests cover `AppState` and `BridgeMessage`
- The application still loads ChatGPT, keeps session, and has DevTools — nothing regresses

---

## What DC002 Is NOT

DC002 is foundation only. The following are explicitly deferred:

- Prompt injection (`inject_prompt.js`, "Test Inject" button) — DC003
- Native `InputPanel` — DC003
- `OutputPanel` — DC004
- Completion detection (`detect_generation.js`) — DC004
- Response extraction (`extract_response.js`) — DC004
- Debug status bar — DC005
- Any ChatGPT DOM interaction beyond the existing ping — DC003+

---

## Target Package Structure

```
src/main/java/com/chatstory/
├── Main.java                  ← entry point: JCEF init, config, shutdown hook
├── AppFrame.java              ← JFrame: browser panel + status label + DevTools button
├── AppState.java              ← state machine (all states defined; only startup transitions wired)
├── UiThread.java              ← EDT dispatch helper
├── browser/
│   ├── BrowserPanel.java      ← wraps CefBrowser, exposes getUIComponent()
│   ├── BrowserClient.java     ← CefClientAdapter: handles load events, drives AppState
│   └── DomBridge.java         ← owns CefMessageRouter, routes messages by type
├── bridge/
│   ├── ChatBridge.java        ← interface (implemented in DC003)
│   ├── ResponseListener.java  ← callback interface (implemented in DC003)
│   └── BridgeMessage.java     ← JSON envelope: parse, validate, hold fields
└── config/
    └── AppConfig.java         ← loads %APPDATA%\ChatStory\config.properties

src/main/resources/
└── js/
    ├── chatgpt_selectors.json ← selector arrays (all four selectors defined)
    └── ping.js                ← the ping script, moved out of Java string literals

src/test/java/com/chatstory/
├── AppStateTest.java
├── BridgeMessageTest.java
└── ResourceLoadingTest.java
```

The `com.chatstory.spike` package is deleted once the new structure is working.

---

## Class Specifications

### `Main.java`

Responsibilities:
- Load `AppConfig` to get the target URL and profile path
- Build JCEF via `CefAppBuilder` (same pattern as spike, just extracted)
- Register JVM shutdown hook
- Create `CefClient`, wire `DomBridge` into it
- Create `CefBrowser` pointed at the configured URL
- Hand off to `AppFrame` on the EDT

The JCEF init console banner (currently in the spike) can be simplified or removed — it was useful during DC001 but no longer needs to be prominent.

### `AppFrame.java`

Responsibilities:
- `JFrame` containing `BrowserPanel` (center) and a toolbar (north)
- Toolbar: DevTools button + status label
- Observes `AppState` to update the status label text
- Window close handler: `System.exit(0)` (same as spike)

The status label should show the current state name in plain English:
- `Starting` → "Starting..."
- `LoadingChatGPT` → "Loading ChatGPT..."
- `NeedsLogin` → "Please log in to ChatGPT"
- `Ready` → "Ready"
- (Later states will be added in DC003/DC004)

### `AppState.java`

Define the full enum of states now so DC003 and DC004 don't need to add new states:

```
Starting → LoadingChatGPT → NeedsLogin → Ready
LoadingChatGPT → Ready  (if already logged in)
Ready → InjectingPrompt  (DC003)
InjectingPrompt → Sending | Error  (DC003)
Sending → WaitingForResponse  (DC003)
WaitingForResponse → Complete | Error  (DC004)
Complete → Ready  (on next send, DC003)
Error → Ready  (via retry)
```

DC002 only wires the startup transitions: `Starting → LoadingChatGPT → NeedsLogin/Ready`.

Implement as a thread-safe class (state field is `volatile`; transitions guarded by `synchronized`). Expose:
- `transition(AppState next)` — validates the transition is legal; throws `IllegalStateException` if not
- `current()` — returns current state
- `isSendEnabled()` — returns true when state is `Ready` or `Complete`
- A `StateListener` interface + `addListener` / `removeListener` so `AppFrame` can observe changes and update the status label via `UiThread.run()`

`AppState` has no dependency on JCEF or Swing — it must be testable in pure JUnit.

### `UiThread.java`

Identical to the design in ClaudePlan03:

```java
final class UiThread {
    static void run(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }
}
```

All JCEF callbacks touching Swing must go through this. Apply it in `BrowserClient` load events and `DomBridge` message handlers from DC002 onward.

### `BrowserPanel.java`

Thin wrapper:
- Constructor takes `CefBrowser`
- `getUIComponent()` delegates to `browser.getUIComponent()`
- Exists to give the UI layer a stable type it can depend on without importing JCEF directly

### `BrowserClient.java`

Extends `CefClientAdapter`. Responsibilities:
- `onLoadStart` → transition `AppState` to `LoadingChatGPT` via `UiThread.run()`
- `onLoadEnd` (main frame) → transition to `Ready` if already logged in, or `NeedsLogin` if redirected to login page
  - Login detection heuristic: if the final URL contains `/auth/` or `/login` treat as `NeedsLogin`; otherwise treat as `Ready`
  - This heuristic is approximate — automated login detection is deferred
- `onLoadError` → log the error; do not crash

### `DomBridge.java`

Owns the `CefMessageRouter`. Responsibilities:
- Constructor takes `CefClient`; creates and registers the router
- Registers a base `CefMessageRouterHandlerAdapter` that parses each incoming request as a `BridgeMessage` and dispatches by `type`
- Provides `registerHandler(String type, BridgeMessageHandler handler)` so DC003/DC004 can add handlers for `injectResult`, `sendResult`, etc. without touching `DomBridge`
- Handles `ping` type internally: logs received time and requestId
- Provides `execute(CefBrowser browser, String script)` — a thin wrapper around `browser.executeJavaScript` for scripts that only inject commands and don't return values
- Loads `ping.js` from resources and injects it on load end (replaces the inlined JS string from the spike)

`BridgeMessageHandler` can be a simple functional interface:
```java
@FunctionalInterface
interface BridgeMessageHandler {
    void handle(BridgeMessage message, CefQueryCallback callback);
}
```

### `BridgeMessage.java`

Parses the JSON envelope from `window.cefQuery`. Fields:

| Field | Type | Notes |
|-------|------|-------|
| `type` | String | Required. Routes to the correct handler. |
| `requestId` | long | 0 if absent. Used for matching async responses. |
| `ok` | boolean | true by default if absent |
| `text` | String | nullable |
| `errorCode` | String | nullable |
| `message` | String | nullable (human-readable) |

Static factory: `BridgeMessage.parse(String json)` — returns a `BridgeMessage` or throws `BridgeMessageException` if the JSON is malformed or `type` is missing.

Use `org.json` or `com.google.gson` for parsing — whichever is lighter. Gson is already a likely transitive dependency via jcefmaven; check before adding a new dependency.

### `ChatBridge.java` and `ResponseListener.java`

Interface definitions only — no implementation in DC002. These are written so DC003 has a clear contract to implement against.

```java
public interface ChatBridge {
    void sendPrompt(String prompt, ResponseListener listener);
}

public interface ResponseListener {
    void onPromptSubmitted(long requestId);
    void onResponsePartial(long requestId, String responseText);   // no-op until DC004
    void onResponseComplete(long requestId, String responseText);
    void onError(long requestId, String errorCode, String message);
}
```

### `AppConfig.java`

Responsibilities:
- Resolve profile path: `%LOCALAPPDATA%\ChatStory\profile`, fallback `{user.home}/.chatstory/profile`
- Resolve config file: `%APPDATA%\ChatStory\config.properties`, fallback `{user.home}/.chatstory/config.properties`
- Load `target.chat.url` from config file; default `https://chatgpt.com` if absent or file not found
- All paths resolved once at construction; exposed as simple getters

### `ping.js`

Move the ping JS from the inlined Java string to `src/main/resources/js/ping.js`:

```javascript
if (typeof window.cefQuery === 'function') {
    window.cefQuery({
        request: JSON.stringify({ type: 'ping', requestId: 0 }),
        onSuccess: function(r) { console.log('[bridge] ping ok:', r); },
        onFailure: function(e, m) { console.error('[bridge] ping fail:', e, m); }
    });
}
```

### `chatgpt_selectors.json`

Create with all four selector groups. These are starting candidates — DC003 will verify and update them via DevTools before using them in real scripts:

```json
{
    "promptEditor": [
        "[contenteditable='true'][data-id]",
        "div[contenteditable='true']"
    ],
    "sendButton": [
        "button[data-testid='send-button']",
        "button[aria-label='Send message']"
    ],
    "stopButton": [
        "button[aria-label='Stop generating']",
        "[data-testid='stop-button']"
    ],
    "assistantMsg": [
        "[data-message-author-role='assistant']"
    ]
}
```

---

## Unit Tests

### `AppStateTest`

- All valid startup transitions succeed (`Starting → LoadingChatGPT`, etc.)
- All remaining valid transitions succeed (`Ready → InjectingPrompt`, etc.)
- Invalid transitions throw `IllegalStateException`
- `isSendEnabled()` returns true only in `Ready` and `Complete`
- `StateListener` is notified on each transition
- State field is the expected value after each transition

### `BridgeMessageTest`

- Parse a complete well-formed message — all fields populated correctly
- Parse a message with only `type` — optional fields are null/default
- Missing `type` field throws `BridgeMessageException`
- Malformed JSON throws `BridgeMessageException`
- `requestId` defaults to 0 when absent
- `ok` defaults to true when absent
- `errorCode` and `message` are independent nullable fields

### `ResourceLoadingTest`

- `ping.js` can be loaded as a non-empty string from the classpath
- `chatgpt_selectors.json` can be loaded as a non-empty string from the classpath
- `chatgpt_selectors.json` parses to an object containing `promptEditor`, `sendButton`, `stopButton`, and `assistantMsg` keys

---

## Gradle Changes

Add a JSON library if Gson is not already a transitive dependency. Check with:

```
gradlew dependencies --configuration runtimeClasspath | findstr gson
```

If absent, add:
```kotlin
implementation("com.google.code.gson:gson:2.11.0")
```

Add JUnit 5 for tests:
```kotlin
testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
testRuntimeOnly("org.junit.platform:junit-platform-launcher")

tasks.test {
    useJUnitPlatform()
}
```

---

## Success Criteria

- `com.chatstory.spike` package deleted; all spike code gone
- `gradlew run` launches the application with the new structure
- Status label shows `AppState` transitions during startup
- ChatGPT loads, session persists, DevTools button works — no regression from DC001
- `DomBridge` ping fires and is logged via the new `BridgeMessage`-based handler
- `chatgpt_selectors.json` loads at startup without error
- All three unit test classes pass

---

## Open Questions

1. **Is Gson already a transitive dependency of jcefmaven 146?**
   Check before adding it explicitly. If it is, use it. If not, Gson 2.11 is the lightest JSON option that avoids adding a large new dependency.

2. **How granular should the `NeedsLogin` detection be in DC002?**
   Proposed: simple URL heuristic (`/auth/` or `/login` in the URL). This is good enough for the status label. Full DOM-based detection is deferred.

3. **Should `AppFrame` use a `JSplitPane` between browser and a future output panel, or `BorderLayout.CENTER` for now?**
   Recommendation: `BorderLayout.CENTER` for DC002. The split pane is added in DC004 when `OutputPanel` is built. Introducing it now before there's anything to split against adds complexity with no benefit.
