# ClaudePlan02: Story Workstation MVP Implementation Plan

**Created:** 2026-05-28
**Purpose:** Implementation plan for the Story Workstation MVP. Serves as the source document for creating active DevCycle files.

---

## Goal

Build the smallest possible functional version of the Story Workstation that proves the core browser bridge premise:

Can a native Java desktop application host ChatGPT, preserve a logged-in session across restarts, send user prompts into ChatGPT via DOM interaction, detect when a response is complete, and display the extracted response in a native UI panel?

No storytelling features are included. The sole purpose is to validate that the browser integration layer is reliable enough to build on.

## Desired Outcome

At the end of the MVP:

- The application launches and loads ChatGPT inside an embedded Chromium window
- The user can log in once and remain logged in across application restarts
- The user can type a prompt in a native input box and send it into ChatGPT without touching the browser
- The application detects when the assistant finishes generating
- The latest assistant response appears as plain text in a native output panel separate from the browser UI
- The bridge behaves correctly across repeated sends in the same session

If all of these work reliably, the Story Workstation concept is technically viable and higher-level storytelling features can be built on top.

---

## Technology Stack

### Language
Java 21+

### Build System
Gradle (Kotlin DSL)

### Browser Framework
JCEF (Java Chromium Embedded Framework) — the only browser framework for this MVP.

JavaFX WebView is not a viable alternative. It uses a WebKit engine that does not support the JavaScript APIs ChatGPT requires. If JCEF setup blocks progress, the correct response is to solve the JCEF setup problem, not to switch engines.

The exact JCEF artifact and version must be verified during Phase 0 before any application code is written. The following questions must be answered at that time:

- Which artifact/repository to use (e.g., `dev.datlag:jcef`, official JCEF build distribution, or another)
- Windows x64 and Java 21 compatibility confirmed
- Whether the artifact includes Chromium binaries or requires separate native binary acquisition
- Expected local file layout for native DLLs
- Whether the chosen path is compatible with later `jpackage`-based Windows packaging

### UI Framework
Swing — sufficient for the MVP, no additional runtime dependencies, integrates directly with `CefBrowser.getUIComponent()`.

---

## Project Structure

```
ChatStory/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/
│   │   ├── java/com/chatstory/
│   │   │   ├── Main.java
│   │   │   ├── AppFrame.java
│   │   │   ├── AppState.java              ← status state machine
│   │   │   ├── UiThread.java              ← EDT dispatch helper
│   │   │   ├── browser/
│   │   │   │   ├── BrowserPanel.java
│   │   │   │   ├── BrowserClient.java
│   │   │   │   └── DomBridge.java
│   │   │   ├── bridge/
│   │   │   │   ├── ChatBridge.java        ← interface
│   │   │   │   ├── ResponseListener.java  ← callback interface
│   │   │   │   └── BridgeMessage.java     ← message envelope
│   │   │   └── ui/
│   │   │       ├── InputPanel.java
│   │   │       └── OutputPanel.java
│   │   └── resources/
│   │       └── js/
│   │           ├── chatgpt_selectors.json ← all selectors in one place
│   │           ├── inject_prompt.js
│   │           ├── trigger_send.js
│   │           ├── detect_generation.js
│   │           └── extract_response.js
│   └── test/
│       └── java/com/chatstory/
│           ├── PromptEncodingTest.java
│           ├── BridgeMessageTest.java
│           └── AppStateTest.java
```

---

## Phase 0 — JCEF Feasibility Spike

### Purpose

Phase 0 is a narrow, reversible spike. Its only job is to prove that JCEF can be initialized on this machine, that ChatGPT renders inside an embedded Chromium window, and that a manual login session persists across application restarts. No application structure should be committed to before these three facts are confirmed.

### Tasks

- [ ] Research and confirm exact JCEF artifact, version, and native binary acquisition method
- [ ] Verify Windows x64 and Java 21 compatibility
- [ ] Document required native file layout and launch command
- [ ] Write the minimum Gradle project and Java entry point to open a JCEF Swing window
- [ ] Navigate to `https://chatgpt.com` and confirm it renders correctly
- [ ] Open JCEF DevTools and confirm they are accessible
- [ ] Complete a manual login using the intended authentication method (e.g., Google SSO or email/password — test the method the user will actually use)
- [ ] Configure a persistent profile directory and confirm login survives application restart
- [ ] Wire a minimal `CefMessageRouter` "ping" — send `{ "type": "ping" }` from JS, receive it in Java — to confirm the message channel works before the full bridge is built
- [ ] Document all setup findings in `BUILDING.md`

### Success Criteria

- ChatGPT loads and operates normally inside the application
- Manual login completes without errors
- Session survives an application restart
- The `CefMessageRouter` ping round-trip works
- A developer can repeat the launch from a documented command

### Non-Goals for Phase 0

- Native prompt input
- Prompt injection
- Response extraction
- Final package structure
- Any UI beyond the bare window needed to prove the browser

---

## DevCycle 001 — Browser Scaffold, Persistent Session, and Target Navigation

### Goal

Build the proper application scaffold on top of the Phase 0 spike. Establish the full browser infrastructure, the `CefMessageRouter` bridge foundation, and reliable navigation to a target chat URL. This DevCycle ends with a working embedded browser that the user can interact with normally and that navigates automatically to the intended ChatGPT conversation.

### Tasks

- [ ] Formalize the Gradle build from the Phase 0 spike into the target project structure
- [ ] Implement `BrowserPanel` — wraps `CefBrowser`, exposes `getUIComponent()`
- [ ] Implement `BrowserClient` — extends `CefClientAdapter`, handles page load events
- [ ] Implement `AppFrame` — Swing `JFrame` containing the browser panel and a status label
- [ ] Implement `UiThread` helper (see Bridge Design section)
- [ ] Implement `AppState` state machine (see Status State Model section)
- [ ] Implement `DomBridge` — registers and owns the `CefMessageRouter`; routes incoming messages by type
- [ ] Define `ChatBridge` interface and `ResponseListener` interface (see Bridge Design section)
- [ ] Define `BridgeMessage` envelope and parsing (see Bridge Design section)
- [ ] Configure browser profile path as `%LOCALAPPDATA%\ChatStory\profile` on Windows, falling back to `{user.home}/.chatstory/profile` if `LOCALAPPDATA` is unavailable
- [ ] Add optional config file at `%APPDATA%\ChatStory\config.properties` with a `target.chat.url` key; use `https://chatgpt.com` as the default if the file or key is absent
- [ ] Register a JVM shutdown hook to call `CefApp.getInstance().dispose()`
- [ ] Add a "DevTools" menu item or button that calls `browser.showDevTools(...)` — keep this accessible throughout MVP development
- [ ] Navigate to the configured target URL after the browser finishes loading
- [ ] Status label reflects `AppState` transitions during startup and navigation

### Success Criteria

- Application launches and loads the target ChatGPT page
- Session persists across restarts
- `AppState` transitions are visible in the status label
- DevTools are one click away
- `DomBridge` and `CefMessageRouter` are initialized and the ping round-trip works

---

## DevCycle 002 — Injection Spike and Native Input

### Goal

Prove that a prompt can be injected into ChatGPT's editor and successfully submitted, then wire up the native `InputPanel`. The injection method must be proven with a hardcoded test string before `InputPanel` is built — do not wire up user-facing input until the injection mechanism is confirmed.

### Tasks

**Injection Spike (complete before InputPanel work):**
- [ ] Load `chatgpt_selectors.json` and pass selector arrays as JSON arguments to scripts
- [ ] Implement `inject_prompt.js` — find the editor, inject a hardcoded test string, report result via `cefQuery`
- [ ] Test `execCommand('selectAll') + execCommand('insertText', ...)` as the primary injection strategy
- [ ] If primary strategy fails: test dispatching a synthetic `InputEvent` with `inputType: 'insertText'`
- [ ] Confirm injection strategy by checking that the ChatGPT send button becomes enabled (not just that text appears visually)
- [ ] Document confirmed injection method in `BUILDING.md`
- [ ] Implement `trigger_send.js` — find send button (multiple selector candidates, check enabled state before clicking), click, confirm a new user message appears in the DOM
- [ ] Implement structured failure codes: `editor_not_found`, `send_button_disabled`, `send_button_not_found`, `user_message_not_confirmed`
- [ ] Add retry (up to 3 attempts, 300ms apart) for `editor_not_found` before reporting failure

**Native Input (after injection is confirmed):**
- [ ] Implement `InputPanel` — multi-line `JTextArea` + "Send" `JButton`
- [ ] Add `InputPanel` to `AppFrame` below the browser panel (fixed height ~120px)
- [ ] Wire Send button: encode prompt text (see Prompt Encoding section), execute injection scripts, update `AppState`
- [ ] Disable Send button in all states except `Ready` and `Complete`
- [ ] Clear `InputPanel` after successful send

### Success Criteria

- Injection method confirmed and documented
- User types a prompt in the native input, presses Send, and the prompt appears correctly in ChatGPT and is submitted
- Injection failure produces a visible, specific error message
- Send button correctly enables and disables with application state

---

## DevCycle 003 — Completion Detection and Response Extraction

### Goal

Detect when ChatGPT finishes generating, extract the latest assistant response, and display it in the native output panel.

### Tasks

**Completion Detection:**
- [ ] Record the assistant message count (or a reference to the last assistant message node) immediately before sending
- [ ] Implement `detect_generation.js` — returns whether generation is in progress (checks for stop button presence using multiple selectors)
- [ ] Poll every 500ms after send using `detect_generation.js`
- [ ] Treat generation as complete only when: no stop button is present AND a new assistant message exists that was not present before the send AND the text content of that message has not changed for the stability window (default 1500ms; tune down after real-world testing)
- [ ] Apply a 180-second maximum timeout — on timeout, report partial response state with whatever text is available
- [ ] On any error during detection, report `Error` state with a specific message

**Response Extraction:**
- [ ] Implement `extract_response.js` — targets the inner content node of the new assistant message (confirmed by DevTools inspection, not assumed); avoids including UI chrome such as copy buttons or citation markers
- [ ] Return extracted text through `cefQuery` with a `requestId` matching the pending send operation (see Bridge Design section)
- [ ] Preserve line breaks well enough for multi-paragraph story text to remain readable (no full Markdown rendering required)
- [ ] Implement `OutputPanel` — scrollable `JTextArea`, auto-scrolls to bottom when new content arrives
- [ ] Add `OutputPanel` to `AppFrame` using `JSplitPane` between browser and `InputPanel` so the browser can be expanded for debugging
- [ ] Wire extracted response into `OutputPanel` via `UiThread.run()`
- [ ] Clear `OutputPanel` when a new prompt is sent
- [ ] Add a "Copy" button to `OutputPanel` (copies full response text to clipboard)

### Success Criteria

- After sending a prompt and waiting for completion, the latest assistant response appears in the native output panel
- The output panel updates correctly on repeated sends without capturing stale responses
- Partial timeout state shows available text with a visible indicator

---

## DevCycle 004 — MVP Hardening

### Goal

Make the MVP reliable enough to evaluate for extended use. This DevCycle is about stability, observability, and documentation — not new features.

### Tasks

- [ ] Add a debug status bar below the output panel showing: last bridge operation, last error, last extracted response length
- [ ] Add minimal logging for all bridge operations (operation name, requestId, outcome, duration)
- [ ] Verify and document all confirmed selectors — update `chatgpt_selectors.json` to match live ChatGPT DOM
- [ ] Write and execute the Manual Validation Plan (see section below); record results
- [ ] Document known limitations and any selector assumptions
- [ ] Verify `jpackage` compatibility of the chosen JCEF distribution (confirm the dependency approach can produce a distributable Windows build in a future cycle)

### Deferred to post-MVP
- Prompt history
- Resend button
- Editable prompt buffer
- MutationObserver-based completion detection

### Success Criteria

- All five manual validation tests pass reliably
- Common failure modes produce specific, actionable messages
- Build and run documentation is complete and verified by a fresh setup

---

## Bridge Design

### Interfaces

```java
interface ChatBridge {
    void sendPrompt(String prompt, ResponseListener listener);
    boolean isReady();      // delegates to AppState
    boolean isGenerating(); // delegates to AppState
}

interface ResponseListener {
    void onPromptSubmitted();
    void onResponsePartial(String responseText); // no-op in MVP; defined for future streaming
    void onResponseComplete(String responseText);
    void onError(String message);
}
```

### Message Envelope

All JS-to-Java messages pass through `CefMessageRouter` as JSON. Java parses them in the `DomBridge` handler and routes by type.

```json
{
    "type":      "injectResult | sendResult | generationStatus | extractResult | ping | error",
    "requestId": 42,
    "ok":        true,
    "text":      "...",
    "error":     null
}
```

`requestId` is critical. The Java side assigns an incrementing integer before each send operation. Any response whose `requestId` does not match the current pending operation is silently discarded. This prevents race conditions when a user sends a new prompt before the previous response is fully processed.

Error codes returned in the `error` field:
- `editor_not_found`
- `send_button_disabled`
- `send_button_not_found`
- `user_message_not_confirmed`
- `extraction_failed`
- `timeout`

### EDT Safety

```java
final class UiThread {
    static void run(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}
```

Every Swing component update triggered by a JCEF callback, bridge message, or background thread must go through `UiThread.run(...)`. JCEF callbacks execute on CEF-internal threads. Updating Swing components directly from those threads causes intermittent rendering corruption.

---

## Selector Strategy

All CSS selectors live in one file: `src/main/resources/js/chatgpt_selectors.json`.

```json
{
    "promptEditor":  [
        "[contenteditable='true'][data-id]",
        "div[contenteditable='true']"
    ],
    "sendButton":    [
        "button[data-testid='send-button']",
        "button[aria-label='Send message']"
    ],
    "stopButton":    [
        "button[aria-label='Stop generating']",
        "[data-testid='stop-button']"
    ],
    "assistantMsg":  [
        "[data-message-author-role='assistant']"
    ]
}
```

Java loads this file at startup and passes the arrays as JSON arguments to each script. Scripts try each candidate in order and use the first that resolves. Selector updates require changing this one file and no Java recompilation.

Selectors should be treated as unstable. Before Phase 2 work begins, verify each entry by inspecting the live ChatGPT DOM in the embedded browser via the DevTools button.

---

## Status State Model

```
Starting
  └─> LoadingChatGPT
        └─> NeedsLogin      (manual login required — detected or inferred)
              └─> Ready
LoadingChatGPT ──────────> Ready   (if already logged in)
Ready
  └─> InjectingPrompt
        ├─> Error            (injection failed)
        └─> Sending
              └─> WaitingForResponse
                    ├─> Complete
                    │     └─> Ready   (after user initiates next send)
                    └─> Error
Error
  └─> Ready                  (via Retry action)
```

**Send button** is enabled only in `Ready` and `Complete` states.

**`NeedsLogin` detection:** For MVP, this state can be entered manually or through a simple heuristic (e.g., the target chat URL redirected to a login page). Automated detection via DOM selector is deferred to avoid fragility.

The state machine is a standalone class (`AppState.java`) with no JCEF or Swing dependency, making it straightforward to unit test.

---

## Prompt Encoding

User-supplied text must never be inserted through raw string substitution into a JavaScript source string. Quotes, backslashes, newlines, and non-ASCII characters will cause syntax errors or unintended behavior.

**Primary approach — JSON encoding:**
```java
String json = new Gson().toJson(promptText); // produces a quoted, escaped JSON string
String script = "injectPrompt(" + json + ");";
browser.executeJavaScript(script, "", 0);
```

This keeps the script call readable in DevTools logs.

**Fallback — Base64 encoding** (if JSON injection hits quoting issues in a specific Chromium version):
```java
String b64 = Base64.getEncoder().encodeToString(promptText.getBytes(StandardCharsets.UTF_8));
String script = "injectPrompt(atob('" + b64 + "'));";
```

The chosen approach must be tested against:
- Empty prompt
- Single and double quotes
- Backslashes
- Newlines and carriage returns
- Long text (several paragraphs)
- Non-ASCII and emoji characters

---

## Completion Detection Parameters

| Parameter | Default | Notes |
|-----------|---------|-------|
| Poll interval | 500 ms | How often to check generation state |
| Stability window | 1500 ms | How long response text must remain unchanged before extraction |
| Maximum timeout | 180 s | Time before reporting timeout/partial state |

The stability window defaults to 1500ms for MVP reliability. If it feels slow in practice, reduce to 600–800ms in DevCycle 004 tuning. The polling interval and stability window should be constants in one location so they can be adjusted without touching logic code.

---

## Windows Path Conventions

| Purpose | Path |
|---------|------|
| Browser profile / cache | `%LOCALAPPDATA%\ChatStory\profile` |
| User config file | `%APPDATA%\ChatStory\config.properties` |

`%LOCALAPPDATA%` is preferred for the browser profile because browser caches grow large and should not roam across machines.

Java resolution:
```java
String localAppData = System.getenv("LOCALAPPDATA");
String appData      = System.getenv("APPDATA");
// Fall back to user.home if either variable is unavailable
```

The target chat URL is stored in config under `target.chat.url`. Do not commit a real chat URL to source control. The default value in source is `https://chatgpt.com`.

---

## Unit-Testable Boundaries

These components have no JCEF or Swing dependency and must have unit tests:

| Component | What to test |
|-----------|-------------|
| `PromptEncoding` | JSON and base64 encoding against all edge cases listed above |
| `BridgeMessage` | Parsing of all message types; missing fields; malformed JSON |
| `AppState` | All valid transitions; invalid transition attempts; Send button enable rules |
| Script resource loading | All JS and JSON resource files exist and can be read as strings |

---

## Manual Validation Plan

These tests must all pass before the MVP is considered complete. Results should be recorded in DevCycle 004.

### Test 1 — Browser Loads

1. Launch the application
2. Confirm ChatGPT appears in the embedded browser window
3. Confirm DevTools are accessible

Pass: ChatGPT is functional inside the application.

### Test 2 — Login Persists

1. Log in using the intended authentication method
2. Close the application
3. Relaunch

Pass: The user is still logged in after restart.

### Test 3 — Prompt Sends

1. Type a short prompt in the native input
2. Click Send

Pass: The prompt appears correctly in ChatGPT and is submitted. The application status updates through `Sending` and `WaitingForResponse`.

### Test 4 — Response Extracts

1. Send a short prompt
2. Wait for generation to complete

Pass: The latest assistant response appears in the native output panel as plain text. Status reaches `Complete`.

### Test 5 — Repeated Sends

1. Send three short prompts in the same session, waiting for each to complete
2. Observe each extracted response

Pass: Each response is the correct response to its prompt. No stale or crossed responses. The output panel clears and repopulates correctly for each send.

### Test 6 — Failure Recovery

1. Trigger a known failure (e.g., attempt to send before the page is fully loaded)

Pass: The application shows a specific error message and returns to a recoverable state. It does not hang or crash.

---

## Explicit Non-Goals for the MVP

These features must not be added until the browser bridge is stable:

- Multi-pane semantic input (dialogue/action/instruction panes)
- XML or tagged response protocols
- Story-only response extraction
- Continuity or relationship tracking
- Correction workflows
- File upload profiles
- Session archival
- Project or chat selection UI
- Prompt templates
- Canon database
- MutationObserver-based completion detection
- Prompt history or resend
- Full Markdown rendering in the output panel
- Windows installer or packaged distribution

---

## Open Questions

These must be resolved before or during Phase 0:

1. **Which exact JCEF artifact and version?** Research during Phase 0. Verify Windows x64, Java 21, native binary inclusion, and `jpackage` compatibility before any code is written.

2. **Which login method to test in Phase 0?** Use the authentication method the user will actually use day-to-day (e.g., Google SSO). A different method may not validate the real login flow.

3. **Should `BUILDING.md` be a standalone file or a section of `README.md`?** Use `README.md` unless JCEF native binary setup requires more than a few steps. Decide during Phase 0 based on complexity.

---

## Packaging Note

Packaging is not MVP work and is deferred. However, the JCEF artifact and integration approach chosen in Phase 0 must be compatible with a `jpackage`-based Windows installer in a future cycle. Verify this compatibility during Phase 0 before committing to a dependency path that works only inside an IDE.
