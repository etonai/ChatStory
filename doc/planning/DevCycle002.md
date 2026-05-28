# DevCycle 002: Application Foundation

**Status:** Planning
**Start Date:** 2026-05-28
**Target Completion:** 2026-06-18
**Focus:** Replace the DC001 spike with a clean, extensible application scaffold while preserving all DC001 behavior.

---

## Goal

DC001 proved the browser bridge premise in a single spike file. DC002 turns that spike into a maintainable application structure. This cycle is a structural refactor — no new ChatGPT automation is added. The application must be functionally equivalent to the spike when DC002 closes.

The spike package `com.chatstory.spike` is kept as a reference until the new scaffold is verified to preserve all DC001 behavior, then removed as a final cleanup step.

## Desired Outcome

At the end of DC002:

- The application is organized into the target package layout (`browser/`, `bridge/`, `config/`)
- `AppState`, `UiThread`, `DomBridge`, and the bridge interfaces are in place
- The status label reflects live `AppState` transitions during startup and navigation without crashing on repeated load events
- `ping.js` and `chatgpt_selectors.json` load from classpath resources
- Config file is read at startup with graceful fallback
- `AppStateTest`, `BridgeMessageTest`, and `ResourceLoadingTest` all pass
- ChatGPT loads, session persists, and DevTools are accessible — no regression from DC001
- `com.chatstory.spike` is deleted

---

## Tasks

### Phase 1: Build Infrastructure and Foundation Classes

**Status:** Planning

**Gradle changes — make these first, before writing any new Java:**
- [ ] Add `implementation("com.google.code.gson:gson:2.11.0")` to `build.gradle.kts` — declare explicitly, do not rely on transitive dependency from jcefmaven
- [ ] Add JUnit 5 to `build.gradle.kts`:
  ```kotlin
  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  tasks.test { useJUnitPlatform() }
  ```

**`UiThread.java` (`com.chatstory`):**
- [ ] Implement `UiThread.run(Runnable)` — dispatches to Swing EDT via `SwingUtilities.invokeLater` if not already on EDT, runs directly if already on EDT

**`AppState.java` (`com.chatstory`):**
- [ ] Define the state enum with all states: `Starting`, `LoadingChatGPT`, `NeedsLogin`, `Ready`, `InjectingPrompt`, `Sending`, `WaitingForResponse`, `Complete`, `Error`
- [ ] Define `StateListener` interface: `onStateChanged(AppState previous, AppState current)`
- [ ] Implement `browserLoadStarted(String url)` — transitions to `LoadingChatGPT` from any non-terminal state (idempotent if already there)
- [ ] Implement `browserLoadFinished(String url, boolean seemsLoggedIn)` — transitions `LoadingChatGPT → Ready` or `LoadingChatGPT → NeedsLogin`; silent no-op if called from any other state
- [ ] Implement `browserLoadError(String url, String description)` — logs `ErrorCodes.PAGE_LOAD_FAILED` with URL and description; no state transition; never throws
- [ ] Implement `transition(AppState next)` — validates the transition is legal and throws `IllegalStateException` if not; used by DC003/DC004 for prompt/response states
- [ ] Implement `current()`, `isSendEnabled()` (true only in `Ready` and `Complete`), `addListener`, `removeListener`
- [ ] Make thread-safe: `volatile` state field; synchronized listener notification

**`bridge/` package (`com.chatstory.bridge`):**
- [ ] Implement `BridgeMessageException` — simple checked exception extending `Exception`
- [ ] Implement `BridgeMessage` with fields `type` (required), `requestId` (default 0), `ok` (see rule below), `text`, `errorCode`, `message`; static factory `BridgeMessage.parse(String json) throws BridgeMessageException`
- [ ] Implement `BridgeMessageHandler` — `@FunctionalInterface` with `void handle(BridgeMessage msg, CefQueryCallback callback)`
- [ ] Implement `ErrorCodes` — `public static final String` constants for the full vocabulary (see Technical Notes)
- [ ] Implement `ChatBridge` interface — `void sendPrompt(String prompt, ResponseListener listener)`; add comment "Implemented in DC003"
- [ ] Implement `ResponseListener` interface — `onPromptSubmitted`, `onResponsePartial`, `onResponseComplete`, `onError` with `requestId` in every callback; add comment "Implemented in DC003"

**`AppConfig.java` (`com.chatstory.config`):**
- [ ] Resolve profile path (`%LOCALAPPDATA%\ChatStory\profile`, fallback `{user.home}/.chatstory/profile`)
- [ ] Resolve config file path (`%APPDATA%\ChatStory\config.properties`, fallback `{user.home}/.chatstory/config.properties`)
- [ ] Create application directories (`%LOCALAPPDATA%\ChatStory` and `%APPDATA%\ChatStory`) at construction time — if creation fails, log and continue with fallback
- [ ] Load `target.chat.url` from config file if it exists; default to `https://chatgpt.com` if file is absent, key is missing, or any read error occurs; log the fallback when it is used
- [ ] Expose via read-only getters: `getTargetUrl()`, `getProfilePath()`, `getConfigFilePath()`; startup never fails because of a config problem

**Technical Notes:**

`AppState` valid `transition()` paths (DC003/DC004):
```
Ready            → InjectingPrompt
InjectingPrompt  → Sending | Error
Sending          → WaitingForResponse
WaitingForResponse → Complete | Error
Complete         → Ready
Error            → Ready
```

`BridgeMessage.ok` default rule: if the `"ok"` field is absent in the JSON, default to `false` when `type == "error"`, and to `true` for all other types. This prevents error messages from silently appearing successful when `ok` is omitted.

`ErrorCodes` full set — define all now so DC003/DC004 have a consistent vocabulary:
```java
// Browser / navigation
PAGE_LOAD_FAILED, NOT_ON_CHATGPT_PAGE, LOGIN_REQUIRED
// Bridge protocol
BRIDGE_MESSAGE_INVALID, BRIDGE_HANDLER_FAILED
// Injection (used in DC003)
EDITOR_NOT_FOUND, PROMPT_INJECTION_FAILED, SEND_BUTTON_NOT_FOUND,
SEND_BUTTON_DISABLED, SEND_CLICK_FAILED, USER_MESSAGE_NOT_CONFIRMED
// Extraction (used in DC004)
EXTRACTION_FAILED, TIMEOUT
```

---

### Phase 2: Browser Layer, Resource Files, and Application Assembly

**Status:** Planning

**Resource files:**
- [ ] Create `src/main/resources/js/ping.js` — self-invoking function that calls `window.cefQuery` with `{ type: 'ping', requestId: 0 }`; guards for missing `window.cefQuery` with a console warning
- [ ] Create `src/main/resources/js/chatgpt_selectors.json` with selector arrays for `promptEditor`, `sendButton`, `stopButton`, and `assistantMsg` (see Technical Notes for values)

**`browser/` package (`com.chatstory.browser`):**
- [ ] Implement `BrowserPanel` — wraps `CefBrowser`; exposes `getUIComponent()` and `getBrowser()`; gives the UI layer a stable type that doesn't require importing JCEF directly
- [ ] Implement `DomBridge` — creates and registers `CefMessageRouter` (query function `"cefQuery"`); dispatches by type using a `ConcurrentHashMap<String, BridgeMessageHandler>`; exposes `registerHandler(String type, BridgeMessageHandler)`, `execute(CefBrowser, String script)`, and `executePing(CefBrowser)`
- [ ] Implement `BrowserClient` — extends `CefClientAdapter`; calls `appState.browserLoadStarted/Finished/Error()` from `onLoadStart/End/Error`; calls `domBridge.executePing(browser)` on main-frame `onLoadEnd`; wraps all `AppState` calls in `UiThread.run()`

**Application assembly:**
- [ ] Implement `AppFrame` — `JFrame` with `BorderLayout`; browser panel in `CENTER`, toolbar in `NORTH`; toolbar contains DevTools button and status label; registers a `StateListener` on `AppState` to update the label; window close calls `System.exit(0)` via `WindowAdapter`
- [ ] Implement `Main.java` (`com.chatstory.Main`) — creates `AppConfig`, builds JCEF, registers shutdown hook, creates `DomBridge` and `BrowserClient`, creates `CefBrowser` at the configured URL, opens `AppFrame` on the EDT

**Technical Notes:**

`DomBridge` message routing — in the `CefMessageRouterHandlerAdapter.onQuery` implementation:
1. Parse incoming request as `BridgeMessage` — if `BridgeMessage.parse()` throws `BridgeMessageException`, log the raw request and exception, call `callback.failure(-1, ErrorCodes.BRIDGE_MESSAGE_INVALID)`, return true
2. Look up the registered handler by `message.getType()` — if none found, log a warning, call `callback.success("")`, return true
3. Invoke the handler — if the handler throws a runtime exception, catch it, log it, call `callback.failure(-1, ErrorCodes.BRIDGE_HANDLER_FAILED)`, return true

`DomBridge` registers a default ping handler at construction:
```java
registerHandler("ping", (msg, cb) -> {
    System.out.println("[Bridge] ping from: " + /* log URL via frame */ "...");
    cb.success("ok");
});
```
Include the source URL in the log so repeated pings from redirect sequences are easy to trace. Multiple pings per startup are expected and normal.

`BrowserClient` login detection heuristic (best-effort, not a success criterion):
```java
private boolean isLoginPage(String url) {
    if (url == null) return false;
    return url.contains("/auth/") || url.contains("/login")
        || url.contains("accounts.google.com");
}
```
`NeedsLogin` is informational in DC002. The status label may show `Ready` even when ChatGPT is showing a login page — this is acceptable.

`AppFrame` status label text per state:

| State | Label |
|-------|-------|
| `Starting` | "Starting..." |
| `LoadingChatGPT` | "Loading ChatGPT..." |
| `NeedsLogin` | "Please log in to ChatGPT" |
| `Ready` | "Ready" |
| `Error` | "Error — check console" |
| Others | Set when those states are wired in DC003/DC004 |

`AppFrame`'s `StateListener` must use `UiThread.run(...)` before touching any Swing component, even though `BrowserClient` currently dispatches on the EDT. Future DC003/DC004 transitions may originate from bridge callbacks or worker threads; defensive dispatch here keeps the listener safe regardless of the call source.

`Main.java` initialization order matters — follow the spike's sequence:
1. `AppConfig` (paths only, no JCEF)
2. `CefAppBuilder` with `installDir`, `cache_path = appConfig.getProfilePath()`
3. `builder.build()` (blocking, may download natives)
4. Shutdown hook
5. `cefApp.createClient()`
6. `new DomBridge(client)` — registers router before browser is created
7. `new BrowserClient(appState, domBridge)` — add load handler before browser is created
8. `client.createBrowser(targetUrl, false, false)`
9. `SwingUtilities.invokeLater(...)` — open `AppFrame`

Do not rearrange this sequence. The DC001 spike proved it works; changing the order risks silent JCEF initialization failures.

`chatgpt_selectors.json` initial values (DC003 verifies these via DevTools before using them in real scripts):
```json
{
    "promptEditor":  ["[contenteditable='true'][data-id]", "div[contenteditable='true']"],
    "sendButton":    ["button[data-testid='send-button']", "button[aria-label='Send message']"],
    "stopButton":    ["button[aria-label='Stop generating']", "[data-testid='stop-button']"],
    "assistantMsg":  ["[data-message-author-role='assistant']"]
}
```

---

### Phase 3: Tests, Verification, and Cleanup

**Status:** Planning

**Unit tests:**
- [ ] Write `AppStateTest` — see test cases in Technical Notes below
- [ ] Write `BridgeMessageTest` — see test cases in Technical Notes below
- [ ] Write `ResourceLoadingTest` — see test cases in Technical Notes below
- [ ] Run `gradlew.bat test` and confirm all tests pass

**Manual verification:**
- [ ] Run `gradlew.bat run` — confirm application launches via `com.chatstory.Main`
- [ ] Confirm status label transitions are visible in the toolbar during startup
- [ ] Confirm ChatGPT loads and session persists (no re-login required)
- [ ] Confirm DevTools button opens Chromium DevTools
- [ ] Confirm `[Bridge] ping` log lines appear in the console on page load

**Cleanup:**
- [ ] Update `BUILDING.md`: change main class reference from `com.chatstory.spike.Main` to `com.chatstory.Main`; keep all other setup and run documentation accurate
- [ ] Delete `src/main/java/com/chatstory/spike/` directory
- [ ] Run `gradlew.bat run` again to confirm nothing broke after deletion
- [ ] Consider committing the spike deletion separately from the scaffold work so it is easy to review or revert if needed

**Technical Notes:**

`AppStateTest` cases:

*Startup transitions:*
- `Starting → LoadingChatGPT` via `browserLoadStarted()`
- `LoadingChatGPT → Ready` via `browserLoadFinished(url, true)`
- `LoadingChatGPT → NeedsLogin` via `browserLoadFinished(url, false)`
- `NeedsLogin → LoadingChatGPT` via `browserLoadStarted()`

*Navigation noise tolerance:*
- Two consecutive `browserLoadStarted()` calls do not throw
- `browserLoadFinished()` while in `Ready` is a silent no-op
- `browserLoadError()` from any state does not throw

*Direct transitions (DC003/DC004 states):*
- `Ready → InjectingPrompt`, `InjectingPrompt → Sending`, `InjectingPrompt → Error`
- `Sending → WaitingForResponse`, `WaitingForResponse → Complete`, `WaitingForResponse → Error`
- `Complete → Ready`, `Error → Ready`
- Invalid transition (e.g. `Starting → Complete`) throws `IllegalStateException`

*Send enablement:*
- `isSendEnabled()` is false in all states except `Ready` and `Complete`

*Listener:*
- Registered listener receives `(previous, current)` on every transition
- Removed listener is not called after removal

`BridgeMessageTest` cases:
- Complete well-formed message: all fields parse correctly
- `type` only — optional fields are null or default
- Missing `type` throws `BridgeMessageException`
- Malformed JSON throws `BridgeMessageException`
- `requestId` absent → `0`
- `ok` absent + `type != "error"` → `true`
- `ok` absent + `type == "error"` → `false`
- `ok: false` explicit → `false` regardless of type
- `errorCode` and `message` are independent nullable fields

`ResourceLoadingTest` cases:
- `ping.js` loads from classpath as a non-empty string
- `chatgpt_selectors.json` loads from classpath as a non-empty string
- `chatgpt_selectors.json` contains keys: `promptEditor`, `sendButton`, `stopButton`, `assistantMsg`
- Each key maps to a JSON array with at least one element
- Each element in each array is a string

---

## Notes and Risks

- **JCEF init sequence is sensitive.** The spike's initialization order is known to work. Do not rearrange the sequence in `Main.java`. If the new `Main.java` fails to launch, temporarily switch `mainClass` in `build.gradle.kts` back to `com.chatstory.spike.Main` to confirm the problem is in the refactor, not in JCEF itself.

- **Multiple load events are normal.** DC001 showed 3 ping events per startup. `AppState` event methods are tolerant of this by design. Do not add guards that assume exactly one load per session.

- **Ping failure during a redirect is not an error.** If `window.cefQuery` is unavailable on a particular navigation step, the ping script logs a warning and exits. A single missed ping must not put the app in `Error` state. DC002 success requires only that the ping works under normal page-load conditions.

- **`BridgeMessageException` must not escape `DomBridge`.** `DomBridge` catches it immediately and converts it to `callback.failure(...)`. Do not let the checked exception propagate into `BrowserClient` or `AppFrame`.

- **Verify Gson version after build.** After adding the explicit Gson dependency, run `gradlew.bat dependencies --configuration runtimeClasspath` and confirm `com.google.code.gson:gson:2.11.0` is present and not being overridden by a different version from jcefmaven.

---

## Completion Summary

*Fill in when DC002 closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** —
**Phases Completed:** —
**Work Deferred:** —

**Accomplishments:**
-

**Metrics:**
- Files created: —
- Files deleted: —
- Tests written: —
- Tests passing: —

**Lessons / Notes:**
*Surprises, decisions made, things that worked well or didn't — especially anything that should inform DC003 planning.*
