# ClaudeDC2Plan02: DevCycle 002 — Application Foundation

**Created:** 2026-05-28
**Purpose:** Implementation plan for DevCycle 002. Serves as the source document for `doc/planning/DevCycle002.md`.

---

## Goal

Replace the DC001 spike with a clean, extensible application scaffold. DC002 is a structural refactor — the application must remain functionally equivalent to the spike at the end of this cycle. No new ChatGPT automation is added.

The spike code in `com.chatstory.spike` is kept as a reference until the new structure is verified, then deleted as a final cleanup step.

## Desired Outcome

At the end of DC002:

- The application is organized into the target package layout
- `AppState`, `UiThread`, `DomBridge`, and the bridge interfaces are in place
- The status label reflects live `AppState` transitions during startup and navigation without crashing on repeated load events
- `chatgpt_selectors.json` and `ping.js` load from classpath resources
- Config file provides the target URL with graceful fallback
- Unit tests cover `AppState`, `BridgeMessage`, and resource loading
- ChatGPT loads, session persists, and DevTools are accessible — no regression from DC001
- The spike package is deleted

---

## Explicit Non-Goals

These are deferred and must not be added in DC002:

- Prompt injection or any `inject_prompt.js` work
- Native `InputPanel`
- `OutputPanel`
- Completion detection
- Response extraction
- Debug status bar
- Any ChatGPT DOM interaction beyond the existing ping

---

## Gradle Changes

Make these changes to `build.gradle.kts` before writing any new Java code.

**Add Gson as an explicit dependency** — do not rely on it being a transitive dependency of jcefmaven, which could disappear when the JCEF version changes:

```kotlin
implementation("com.google.code.gson:gson:2.11.0")
```

**Add JUnit 5:**

```kotlin
testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
testRuntimeOnly("org.junit.platform:junit-platform-launcher")

tasks.test {
    useJUnitPlatform()
}
```

---

## Target Structure

```
src/main/java/com/chatstory/
├── Main.java                      ← entry point: JCEF init, config, shutdown hook
├── AppFrame.java                  ← JFrame: browser + toolbar + status label
├── AppState.java                  ← state machine
├── UiThread.java                  ← EDT dispatch helper
├── browser/
│   ├── BrowserPanel.java          ← wraps CefBrowser, exposes getUIComponent()
│   ├── BrowserClient.java         ← CefClientAdapter: emits load events to AppState
│   └── DomBridge.java             ← owns CefMessageRouter, routes messages by type
├── bridge/
│   ├── BridgeMessage.java         ← JSON envelope
│   ├── BridgeMessageException.java← thrown on malformed or missing-type messages
│   ├── BridgeMessageHandler.java  ← functional interface for type-routed handlers
│   ├── ErrorCodes.java            ← string constants for all error codes
│   ├── ChatBridge.java            ← interface (implemented in DC003)
│   └── ResponseListener.java      ← callback interface (implemented in DC003)
└── config/
    └── AppConfig.java             ← loads config.properties, resolves paths

src/main/resources/js/
├── chatgpt_selectors.json         ← selector arrays for all four DOM targets
└── ping.js                        ← ping script (moved from inline Java string)

src/test/java/com/chatstory/
├── AppStateTest.java
├── BridgeMessageTest.java
└── ResourceLoadingTest.java
```

The spike package `com.chatstory.spike` remains until the new structure passes all success criteria, then is deleted as the final step.

---

## Class Specifications

### `UiThread.java`

No dependencies on JCEF or Swing except `SwingUtilities`. Every JCEF callback that touches a Swing component must go through this.

```java
public final class UiThread {
    public static void run(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }
    private UiThread() {}
}
```

---

### `AppState.java`

A thread-safe state machine with no JCEF or Swing dependency.

**State enum — define the full set now:**

```
Starting
LoadingChatGPT
NeedsLogin
Ready
InjectingPrompt    (wired in DC003)
Sending            (wired in DC003)
WaitingForResponse (wired in DC004)
Complete           (wired in DC004)
Error
```

DC002 only wires the startup transitions. The remaining states are defined so DC003/DC004 don't need to expand the enum.

**Listener interface:**

```java
public interface StateListener {
    void onStateChanged(AppState previous, AppState current);
}
```

**Public API:**

```java
void browserLoadStarted(String url)     // transitions to LoadingChatGPT
void browserLoadFinished(String url, boolean seemsLoggedIn)  // → Ready or NeedsLogin
void browserLoadError(String url, String description)        // logs; no state crash
void transition(AppState next)          // for DC003/DC004 direct transitions (validated)
AppState current()
boolean isSendEnabled()                 // true only in Ready and Complete
void addListener(StateListener l)
void removeListener(StateListener l)
```

**Event method rules — tolerant of real browser noise:**

- `browserLoadStarted` from any non-terminal state → `LoadingChatGPT` (idempotent if already there)
- `browserLoadStarted` from `Ready` → `LoadingChatGPT` (user navigated)
- `browserLoadFinished` from `LoadingChatGPT` → `Ready` or `NeedsLogin` based on `seemsLoggedIn`
- `browserLoadFinished` from any other state → log and ignore (no transition, no throw)
- `browserLoadError` → log the description; no state transition; no exception

**`transition(AppState next)` valid paths (DC003/DC004):**

```
Ready            → InjectingPrompt
InjectingPrompt  → Sending | Error
Sending          → WaitingForResponse
WaitingForResponse → Complete | Error
Complete         → Ready
Error            → Ready
```

Invalid `transition()` calls throw `IllegalStateException`. The event methods never throw.

**Thread safety:** state field is `volatile`; listener notification is synchronized.

---

### `BrowserPanel.java`

Thin wrapper that gives the UI layer a stable type without importing JCEF directly.

```java
public class BrowserPanel {
    private final CefBrowser browser;
    public BrowserPanel(CefBrowser browser) { this.browser = browser; }
    public Component getUIComponent() { return browser.getUIComponent(); }
    public CefBrowser getBrowser() { return browser; }
}
```

---

### `BrowserClient.java`

Extends `CefClientAdapter`. Holds references to `AppState` and `DomBridge`. Calls `AppState` event methods on load events; wraps all state updates in `UiThread.run()`.

```java
@Override
public void onLoadStart(CefBrowser browser, CefFrame frame, ...) {
    if (frame.isMain()) UiThread.run(() -> appState.browserLoadStarted(browser.getURL()));
}

@Override
public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
    if (frame.isMain()) {
        boolean seemsLoggedIn = !isLoginPage(browser.getURL());
        UiThread.run(() -> appState.browserLoadFinished(browser.getURL(), seemsLoggedIn));
        domBridge.executePing(browser);
    }
}

@Override
public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode,
                        String errorText, String failedUrl) {
    if (frame.isMain()) UiThread.run(() ->
        appState.browserLoadError(failedUrl, errorCode + ": " + errorText));
}
```

**Login detection heuristic** — best-effort, not a DC002 success criterion:

```java
private boolean isLoginPage(String url) {
    if (url == null) return false;
    return url.contains("/auth/") || url.contains("/login") || url.contains("accounts.google.com");
}
```

This may not catch all login flows. `NeedsLogin` is informational only in DC002. The status label may show `Ready` even when ChatGPT is showing a login page — this is acceptable.

---

### `DomBridge.java`

Lives in `com.chatstory.browser`. Owns the `CefMessageRouter`. Imports from `com.chatstory.bridge` (correct dependency direction).

**Responsibilities:**
- Create and configure the `CefMessageRouter` with `jsQueryFunction = "cefQuery"`
- Register it with `CefClient` via `client.addMessageRouter(router)`
- Maintain a thread-safe `Map<String, BridgeMessageHandler>` for type-routed dispatch
- Parse each incoming request as a `BridgeMessage`; route to the registered handler by `type`
- If no handler is registered for a type, log a warning
- If `BridgeMessage.parse()` throws, log the raw request and the exception; call `callback.failure(-1, ErrorCodes.BRIDGE_MESSAGE_INVALID)`
- Provide `execute(CefBrowser browser, String script)` for one-way JS injection
- Handle `ping` internally with a default registered handler at construction

**Public API:**

```java
public DomBridge(CefClient client)
public void registerHandler(String type, BridgeMessageHandler handler)
public void execute(CefBrowser browser, String script)
void executePing(CefBrowser browser)   // loads ping.js from resources, executes it
```

**`executePing`** loads `ping.js` once (lazy-load, cache the string) and calls `execute()`.

**Handler registration is thread-safe** — use `ConcurrentHashMap` for the handler map.

---

### `BridgeMessage.java`

Parses the JSON envelope sent via `window.cefQuery`.

**Fields:**

| Field | Java type | JSON absent behavior |
|-------|-----------|---------------------|
| `type` | `String` | Required — throw `BridgeMessageException` if missing |
| `requestId` | `long` | Defaults to `0` |
| `ok` | `boolean` | `false` if `type == "error"`, `true` otherwise |
| `text` | `String` | `null` |
| `errorCode` | `String` | `null` |
| `message` | `String` | `null` |

**Static factory:**

```java
public static BridgeMessage parse(String json) throws BridgeMessageException
```

Throws `BridgeMessageException` (checked) if JSON is malformed or `type` is absent.

**`ok` default rule:** If the `"ok"` field is absent, default to `false` when `type` equals `"error"`, and to `true` for all other types. This prevents an error-type message from silently appearing successful.

---

### `BridgeMessageException.java`

Simple checked exception extending `Exception`. No special fields needed.

---

### `BridgeMessageHandler.java`

Functional interface in `com.chatstory.bridge`:

```java
@FunctionalInterface
public interface BridgeMessageHandler {
    void handle(BridgeMessage message, CefQueryCallback callback);
}
```

---

### `ErrorCodes.java`

String constants in `com.chatstory.bridge`. Define the full set now — the DC003/DC004 constants cost nothing and give all future DevCycles a consistent vocabulary.

```java
public final class ErrorCodes {
    // Browser / navigation
    public static final String PAGE_LOAD_FAILED            = "page_load_failed";
    public static final String NOT_ON_CHATGPT_PAGE         = "not_on_chatgpt_page";
    public static final String LOGIN_REQUIRED              = "login_required";
    // Bridge protocol
    public static final String BRIDGE_MESSAGE_INVALID      = "bridge_message_invalid";
    public static final String BRIDGE_HANDLER_FAILED       = "bridge_handler_failed";
    // Injection (DC003)
    public static final String EDITOR_NOT_FOUND            = "editor_not_found";
    public static final String PROMPT_INJECTION_FAILED     = "prompt_injection_failed";
    public static final String SEND_BUTTON_NOT_FOUND       = "send_button_not_found";
    public static final String SEND_BUTTON_DISABLED        = "send_button_disabled";
    public static final String SEND_CLICK_FAILED           = "send_click_failed";
    public static final String USER_MESSAGE_NOT_CONFIRMED  = "user_message_not_confirmed";
    // Extraction (DC004)
    public static final String EXTRACTION_FAILED           = "extraction_failed";
    public static final String TIMEOUT                     = "timeout";
    private ErrorCodes() {}
}
```

---

### `ChatBridge.java` and `ResponseListener.java`

Interface definitions only. No implementation in DC002. Comments mark where DC003 begins.

```java
/** Implemented in DC003. */
public interface ChatBridge {
    void sendPrompt(String prompt, ResponseListener listener);
}

/** Implemented in DC003. */
public interface ResponseListener {
    void onPromptSubmitted(long requestId);
    void onResponsePartial(long requestId, String responseText);   // no-op until DC004
    void onResponseComplete(long requestId, String responseText);
    void onError(long requestId, String errorCode, String message);
}
```

---

### `AppConfig.java`

Read-only value object resolved at construction time. Graceful fallback on every failure — missing file, missing key, and unreadable path all fall back silently with a log message. Startup never fails because of a config problem.

```java
public final class AppConfig {
    public String getTargetUrl()      // from target.chat.url, default "https://chatgpt.com"
    public String getProfilePath()    // %LOCALAPPDATA%\ChatStory\profile or fallback
    public String getConfigFilePath() // for logging only
}
```

**Path resolution:**

| Purpose | Primary | Fallback |
|---------|---------|----------|
| Browser profile | `%LOCALAPPDATA%\ChatStory\profile` | `{user.home}/.chatstory/profile` |
| Config file | `%APPDATA%\ChatStory\config.properties` | `{user.home}/.chatstory/config.properties` |

`%LOCALAPPDATA%` is preferred for the profile because browser caches are large and should not roam.

---

### `AppFrame.java`

Observes `AppState` via `StateListener`. All state-driven UI updates go through `UiThread.run()`.

**Layout:** `BorderLayout` — browser panel in `CENTER`, toolbar in `NORTH`. No `JSplitPane` until DC004 adds the output panel.

**Toolbar:** DevTools button + status label. Status label text per state:

| State | Label text |
|-------|-----------|
| `Starting` | "Starting..." |
| `LoadingChatGPT` | "Loading ChatGPT..." |
| `NeedsLogin` | "Please log in to ChatGPT" |
| `Ready` | "Ready" |
| `Error` | "Error — check console" |
| Others (DC003+) | Set when those states are wired |

Window close: `DO_NOTHING_ON_CLOSE` + `WindowAdapter.windowClosing` → `System.exit(0)`.

---

### `Main.java`

Replaces the spike entry point. Responsibilities:

1. Create `AppConfig` — resolve all paths before any JCEF code runs
2. Build JCEF via `CefAppBuilder`: set `installDir`, `cache_path` from config, progress handler
3. Register JVM shutdown hook for `CefApp.getInstance().dispose()`
4. Create `CefClient`
5. Create `DomBridge(client)` — registers router and ping handler
6. Create `BrowserClient(appState, domBridge)` and add it to client
7. Create `CefBrowser` pointed at `appConfig.getTargetUrl()`
8. Create `AppFrame(appState, browserPanel, browser)` on the EDT via `SwingUtilities.invokeLater`

Print a brief startup banner (simpler than the spike's version):

```
Story Workstation starting...
  Profile : C:\Users\...\AppData\Local\ChatStory\profile
  Target  : https://chatgpt.com
```

---

## Resource Files

### `ping.js`

```javascript
(function() {
    if (typeof window.cefQuery !== 'function') {
        console.warn('[bridge] cefQuery not available on this page');
        return;
    }
    window.cefQuery({
        request: JSON.stringify({ type: 'ping', requestId: 0 }),
        onSuccess: function(r) { console.log('[bridge] ping ok'); },
        onFailure: function(e, m) { console.error('[bridge] ping fail:', e, m); }
    });
})();
```

### `chatgpt_selectors.json`

Candidate selectors — DC003 verifies these via DevTools before using them in real scripts.

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

**Startup transitions:**
- `Starting → LoadingChatGPT` via `browserLoadStarted()`
- `LoadingChatGPT → Ready` via `browserLoadFinished(..., true)`
- `LoadingChatGPT → NeedsLogin` via `browserLoadFinished(..., false)`
- `NeedsLogin → LoadingChatGPT` via `browserLoadStarted()` (user retrying)

**Navigation noise tolerance:**
- Two consecutive `browserLoadStarted()` calls do not throw
- `browserLoadFinished()` while not in `LoadingChatGPT` is a silent no-op
- `browserLoadError()` from any state does not throw

**Direct transitions (DC003+ states):**
- `Ready → InjectingPrompt` via `transition()`
- `InjectingPrompt → Sending` via `transition()`
- `InjectingPrompt → Error` via `transition()`
- `Sending → WaitingForResponse` via `transition()`
- `WaitingForResponse → Complete` via `transition()`
- `WaitingForResponse → Error` via `transition()`
- `Complete → Ready` via `transition()`
- `Error → Ready` via `transition()`
- Invalid transition (e.g. `Starting → Complete`) throws `IllegalStateException`

**Send enablement:**
- `isSendEnabled()` is false in all states except `Ready` and `Complete`

**Listener:**
- Registered listener receives `(previous, current)` on every state change
- Removed listener is not called

### `BridgeMessageTest`

- Complete well-formed message: all fields parse correctly
- `type` only: optional fields are null or default
- Missing `type` throws `BridgeMessageException`
- Malformed JSON throws `BridgeMessageException`
- `requestId` absent → `0`
- `ok` absent + `type != "error"` → `true`
- `ok` absent + `type == "error"` → `false`
- `ok: false` explicit → `false` regardless of type
- `errorCode` and `message` are independent nullable fields

### `ResourceLoadingTest`

- `ping.js` loads from classpath as a non-empty string
- `chatgpt_selectors.json` loads from classpath as a non-empty string
- `chatgpt_selectors.json` parses to a JSON object containing keys: `promptEditor`, `sendButton`, `stopButton`, `assistantMsg`
- Each of those keys has an array value with at least one element

---

## Spike Deletion — Final Step

After all success criteria above are confirmed:

1. Delete the `src/main/java/com/chatstory/spike/` directory
2. Run `gradlew.bat run` — confirm the application still launches correctly
3. Commit the deletion separately:
   ```
   DC-002: Remove DC001 spike package — new scaffold confirmed
   ```

Keeping the deletion as its own commit makes it trivially revertible if an issue surfaces after deletion.

---

## Success Criteria

- `gradlew.bat run` launches the application via the new `com.chatstory.Main`
- Status label reflects `AppState` transitions during startup and page load
- ChatGPT loads, session persists, DevTools button works — no regression from DC001
- `DomBridge` ping fires on each page load; `[Bridge]` message appears in console
- `ping.js` loads from classpath resources
- `chatgpt_selectors.json` loads and contains all four expected keys
- `AppStateTest`, `BridgeMessageTest`, and `ResourceLoadingTest` all pass
- `com.chatstory.spike` package deleted and application still runs after deletion
- `BUILDING.md` updated: main class changed from `com.chatstory.spike.Main` to `com.chatstory.Main`

---

## Notes and Risks

- **JCEF init is the riskiest part of the refactor.** The spike's initialization sequence is known to work. Extract it carefully into `Main.java` — don't rearrange the order of `CefAppBuilder`, `cache_path`, `build()`, and `addMessageRouter()` calls without testing each step.

- **Keep the spike runnable until the very end.** If the new `Main.java` fails to initialize JCEF, you can temporarily change the `mainClass` in `build.gradle.kts` back to the spike to confirm the issue is in the refactor, not in JCEF itself.

- **`AppState` listener notifications must be on the EDT.** `BrowserClient` wraps all state calls in `UiThread.run()` — `AppFrame`'s listener can then safely update Swing components without an additional dispatch.

- **Multiple load events are expected.** DC001 showed 3 ping events per startup. The tolerant event methods in `AppState` handle this. Do not add assertions or guards that assume exactly one load per session.
