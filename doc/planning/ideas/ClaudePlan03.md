# ClaudePlan03: Story Workstation MVP Implementation Plan

**Created:** 2026-05-28
**Purpose:** Implementation plan for the Story Workstation MVP. Serves as the source document for creating active DevCycle files in `doc/planning/`.

---

## Goal

Build the smallest possible functional version of the Story Workstation that proves the core browser bridge premise:

Can a native Java desktop application host ChatGPT, preserve a logged-in session across restarts, send user prompts into ChatGPT via DOM interaction, detect when a response is complete, and display the extracted response in a native UI panel?

No storytelling features are included. The sole purpose is to validate that the browser integration layer is reliable enough to build on. Once the bridge is proven, all higher-level Story Workstation features can be layered on top.

## Desired Outcome

At the end of the MVP:

- The application launches and loads ChatGPT inside an embedded Chromium window
- The user can log in once and remain logged in across application restarts
- The user can type a prompt in a native input box and send it into ChatGPT without touching the browser
- The application detects when the assistant finishes generating
- The latest assistant response appears as plain text in a native output panel separate from the browser UI
- The bridge behaves correctly across repeated sends in the same session

If all of these work reliably, the Story Workstation concept is technically viable.

---

## Process Note

DevCycle documents created from this plan must follow `DevelopmentProcess.md`. Agents may not mark a DevCycle or phase `Verified` without explicit user permission. Implementation work stops at `Work Complete` and waits for the user to approve `Verified`.

---

## Technology Stack

### Language
Java 21+

### Build System
Gradle (Kotlin DSL)

### Browser Framework
JCEF (Java Chromium Embedded Framework) — the only browser framework for this MVP.

JavaFX WebView is not a viable alternative. It uses a WebKit engine that does not support the JavaScript APIs ChatGPT requires. If JCEF setup blocks progress, the correct response is to solve the JCEF setup problem, not to switch engines.

The exact JCEF artifact and version must be verified during DevCycle 001 before any application code is written. The following questions must be answered at that time:

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
├── BUILDING.md                            ← setup and run instructions
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
│           ├── AppStateTest.java
│           └── ResourceLoadingTest.java
```

This structure is a target. DevCycle 001 intentionally does not build it — it produces only the minimum needed to prove JCEF launches. The full structure is assembled in DevCycle 002.

---

## DevCycle 001 — JCEF Browser Viability Spike

### Goal

Prove that JCEF can launch on this machine, that ChatGPT renders correctly inside an embedded Chromium window, that the intended login flow completes, and that a logged-in session survives application restarts. This DevCycle is a narrow, reversible spike. No application architecture should be committed to before these facts are confirmed.

### Tasks

- [ ] Research and confirm exact JCEF artifact, version, and native binary acquisition method
- [ ] Verify Windows x64 and Java 21 compatibility
- [ ] Confirm whether the artifact includes Chromium binaries or requires separate native binary acquisition
- [ ] Confirm whether the chosen JCEF integration approach is compatible with later `jpackage` packaging
- [ ] Write the minimum Gradle project and Java entry point needed to open a JCEF Swing window — nothing more
- [ ] Navigate to `https://chatgpt.com` and confirm it renders correctly
- [ ] Add a way to open JCEF DevTools and confirm they are accessible
- [ ] Complete a manual login using the actual intended authentication method (test the method the user will use day-to-day, e.g. Google SSO — a different method may not validate the real login flow)
- [ ] Configure a persistent profile directory and confirm login survives application restart
- [ ] Wire a minimal `CefMessageRouter` ping — send `{ "type": "ping" }` from JS, receive and log it in Java — to confirm the message channel works before the full bridge is built
- [ ] Document all setup findings in `BUILDING.md`: native file layout, required JVM flags, and the repeatable launch command

### Success Criteria

- ChatGPT loads and operates normally inside the application window
- Manual login completes without errors using the intended authentication method
- Session survives an application restart
- The `CefMessageRouter` ping round-trip works
- A developer can repeat the launch from the documented command in `BUILDING.md`

### Non-Goals for DevCycle 001

- Native prompt input
- Prompt injection
- Response extraction
- Final application package structure
- State machine implementation
- Any UI beyond the bare window needed to prove the browser

---

## DevCycle 002 — Application Foundation

### Goal

Turn the DevCycle 001 spike into a small, maintainable application foundation. Establish the project structure, the `AppState` model, the `UiThread` convention, the full `CefMessageRouter` bridge infrastructure, the selector loading system, and reliable navigation to the target chat URL. This DevCycle ends with a properly scaffolded application that other engineers can extend.

### Tasks

- [ ] Formalize the Gradle build from the DevCycle 001 spike into the target project structure
- [ ] Implement `Main.java` — application entry point, JCEF init, shutdown hook
- [ ] Implement `AppFrame` — Swing `JFrame` containing the browser panel and a status label
- [ ] Implement `BrowserPanel` — wraps `CefBrowser`, exposes `getUIComponent()`
- [ ] Implement `BrowserClient` — extends `CefClientAdapter`, handles page load events, updates `AppState`
- [ ] Implement `UiThread` helper (see Bridge Design section)
- [ ] Implement `AppState` state machine (see Status State Model section)
- [ ] Implement `DomBridge` — registers and owns the `CefMessageRouter`; routes incoming messages by type to registered handlers
- [ ] Define `ChatBridge` interface, `ResponseListener` interface, and `BridgeMessage` (see Bridge Design section)
- [ ] Load `chatgpt_selectors.json` at startup and make it available to all scripts (see Selector Strategy section)
- [ ] Configure browser profile path as `%LOCALAPPDATA%\ChatStory\profile` on Windows, falling back to `{user.home}/.chatstory/profile` if `LOCALAPPDATA` is unavailable
- [ ] Add optional config file at `%APPDATA%\ChatStory\config.properties` with a `target.chat.url` key; default to `https://chatgpt.com` if the file or key is absent — do not commit a real chat URL to source control
- [ ] Register a JVM shutdown hook to call `CefApp.getInstance().dispose()`
- [ ] Add a "DevTools" menu item or button that calls `browser.showDevTools(...)` — keep this accessible throughout all MVP DevCycles
- [ ] Navigate to the configured target URL after the browser finishes loading
- [ ] Status label reflects `AppState` transitions during startup and navigation
- [ ] Write unit tests for `AppState` transitions and `BridgeMessage` parsing

### Success Criteria

- Application launches and loads the target ChatGPT page via a repeatable Gradle command
- Session persists across restarts
- `AppState` transitions are visible in the status label
- DevTools are one click away
- `DomBridge` and `CefMessageRouter` are initialized and the ping round-trip works
- `chatgpt_selectors.json` loads without error on startup
- `AppState` and `BridgeMessage` unit tests pass

---

## DevCycle 003 — Prompt Injection and Native Input

### Goal

Prove that a prompt can be injected into ChatGPT's editor and successfully submitted, then wire up the native `InputPanel`. The injection mechanism must be proven with a hardcoded test string before `InputPanel` is built. Do not wire up user-facing input until the injection approach is confirmed.

### Tasks

**Injection Spike — complete before any InputPanel work:**
- [ ] Add a temporary "Test Inject" button to `AppFrame`
- [ ] Implement `inject_prompt.js` — uses selector arrays from `chatgpt_selectors.json`; finds the editor, injects a hardcoded test string, reports result via `cefQuery`
- [ ] Test `execCommand('selectAll') + execCommand('insertText', ...)` as the primary injection strategy
- [ ] If primary strategy fails: test dispatching a synthetic `InputEvent` with `inputType: 'insertText'`
- [ ] Confirm injection success by verifying the ChatGPT send button becomes enabled — visual text appearing is not sufficient
- [ ] Document the confirmed injection method in `BUILDING.md`
- [ ] Implement `trigger_send.js` — uses selector arrays from `chatgpt_selectors.json`; checks button exists and is enabled before clicking; confirms a new user message appears in the DOM afterward
- [ ] Implement structured failure codes: `editor_not_found`, `prompt_injection_failed`, `send_button_not_found`, `send_button_disabled`, `send_click_failed`, `user_message_not_confirmed`
- [ ] Add retry (up to 3 attempts, 300ms apart) for `editor_not_found` before reporting failure
- [ ] Remove "Test Inject" button once injection is confirmed

**Native Input — after injection is confirmed:**
- [ ] Implement `InputPanel` — multi-line `JTextArea` + "Send" `JButton`
- [ ] Add `InputPanel` to `AppFrame` below the browser panel (fixed height ~120px)
- [ ] Wire Send button: encode prompt text (see Prompt Encoding section), execute injection and send scripts, update `AppState`
- [ ] Disable Send button in all states except `Ready` and `Complete`
- [ ] Clear `InputPanel` after successful send
- [ ] Write unit tests for prompt encoding edge cases

### Success Criteria

- Injection method confirmed and documented
- User types a prompt in the native input, presses Send, and the prompt appears correctly in ChatGPT and is submitted
- Injection or send failure produces a visible, specific error message
- Send button correctly enables and disables with application state
- Prompt encoding unit tests pass

---

## DevCycle 004 — Completion Detection and Response Extraction

### Goal

Detect when ChatGPT finishes generating, extract the latest assistant response, and display it in the native output panel.

### Tasks

**Completion Detection:**
- [ ] Record the assistant message count (or a reference to the last assistant message node) immediately before sending — store it in the active `requestId` operation context
- [ ] Implement `detect_generation.js` — returns current generation status using multiple stop-button selector candidates
- [ ] Poll every 500ms after send using `detect_generation.js`
- [ ] Treat generation as complete only when all three signals are satisfied: no stop button present AND a new assistant message exists that was not present before the send AND the text content of that message has not changed for the stability window
- [ ] Stability window default: 1500ms — store as a named constant, tune down to 600–800ms during DevCycle 005 if the delay is noticeable in practice
- [ ] Apply a 180-second maximum timeout — on timeout, transition to `Error` with whatever text is available and display a "response may be partial" indicator
- [ ] On any detection error, transition to `Error` state with a specific message

**Response Extraction:**
- [ ] Implement `extract_response.js` — target the inner content node of the new assistant message (confirm the correct node by DevTools inspection before writing the selector); avoid including UI chrome such as copy buttons or citation markers
- [ ] Return extracted text through `cefQuery` with the `requestId` matching the pending send operation — discard any result whose `requestId` does not match
- [ ] Preserve line breaks well enough for multi-paragraph story text to remain readable; full Markdown rendering is not required
- [ ] Implement `OutputPanel` — scrollable `JTextArea`, auto-scrolls to bottom when new content arrives
- [ ] Add `OutputPanel` to `AppFrame` using `JSplitPane` between browser panel and `InputPanel` so the browser can be expanded for debugging
- [ ] Wire extracted response into `OutputPanel` via `UiThread.run()`
- [ ] Clear `OutputPanel` when a new prompt is sent
- [ ] Add a "Copy" button to `OutputPanel` that copies the full response text to clipboard

### Success Criteria

- After sending a prompt and waiting for completion, the latest assistant response appears in the native output panel
- Output panel updates correctly on repeated sends without capturing stale responses
- Timeout state shows available text with a visible partial indicator
- `requestId` mismatch results in silent discard, not a crash or wrong output

---

## DevCycle 005 — MVP Hardening

### Goal

Make the MVP reliable enough for extended local evaluation. This DevCycle is about stability, observability, and documentation — not new features.

### Tasks

- [ ] Add a debug status bar showing: current `AppState`, last bridge operation, last `requestId`, last error code, last extracted response length
- [ ] Add minimal logging for all bridge operations: operation name, `requestId`, outcome, duration
- [ ] Tune the stability window constant based on observed behavior (target: no noticeable lag on short responses; no premature extraction on long ones)
- [ ] Verify all selectors against the live ChatGPT DOM using DevTools — update `chatgpt_selectors.json` to confirmed values
- [ ] Document all confirmed selectors and all known selector assumptions
- [ ] Document known limitations: selector fragility, response partial risk, session edge cases
- [ ] Write and execute the full Manual Validation Plan; record results in the DevCycle 005 document
- [ ] Verify `jpackage` compatibility: confirm the chosen JCEF distribution can produce a distributable Windows build in a future cycle
- [ ] Update `BUILDING.md` with any setup changes discovered during DevCycle 003 and 004
- [ ] Write `ResourceLoadingTest` — confirms all JS files and `chatgpt_selectors.json` exist and load without error

### Deferred to post-MVP

- Prompt history
- Resend button
- Editable prompt buffer
- MutationObserver-based completion detection
- Full Markdown rendering

### Success Criteria

- All manual validation tests pass reliably
- Common failure modes produce specific, actionable messages
- Selector assumptions are documented
- Build and run documentation is complete and verified by a fresh setup
- All unit tests pass

---

## Bridge Design

### Interfaces

```java
interface ChatBridge {
    void sendPrompt(String prompt, ResponseListener listener);
}

interface ResponseListener {
    void onPromptSubmitted(long requestId);
    void onResponsePartial(long requestId, String responseText);  // no-op in MVP; defined for future streaming
    void onResponseComplete(long requestId, String responseText);
    void onError(long requestId, String errorCode, String message);
}
```

`requestId` appears in every callback. This allows a single listener instance to distinguish multiple operations and discard stale results when a new prompt is sent before the previous response completes.

`onError` separates `errorCode` (machine-readable, matched against error code constants) from `message` (human-readable, displayed in the UI).

`AppState` is the source of truth for application status. `ChatBridge` implementations query `AppState` directly rather than maintaining their own ready/generating flags.

### Message Envelope

All JS-to-Java messages pass through `CefMessageRouter` as JSON. `DomBridge` parses each message and routes it by type.

```json
{
    "type":      "injectResult | sendResult | generationStatus | extractResult | ping | error",
    "requestId": 42,
    "ok":        true,
    "text":      "...",
    "errorCode": null,
    "message":   null
}
```

The Java side assigns an incrementing integer `requestId` before each send operation. Any incoming message whose `requestId` does not match the current pending operation is silently discarded.

Error codes used in the `errorCode` field:
- `editor_not_found`
- `prompt_injection_failed`
- `send_button_not_found`
- `send_button_disabled`
- `send_click_failed`
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

Every Swing component update triggered by a JCEF callback, bridge message, or background thread must go through `UiThread.run(...)`. JCEF callbacks execute on CEF-internal threads. Updating Swing components directly from those threads causes intermittent rendering corruption that is hard to reproduce.

This applies to: output panel updates, status label updates, button enablement, error messages, debug status text, and load event handling that touches any UI component.

---

## Selector Strategy

All CSS selectors live in one file: `src/main/resources/js/chatgpt_selectors.json`.

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

Java loads this file at startup and passes the relevant selector arrays as JSON arguments to each script. Scripts try each candidate in order and use the first that resolves. Selector updates require changing this one file with no Java recompilation.

Selectors are unstable — OpenAI updates the ChatGPT frontend regularly. Before DevCycle 003 work begins, verify each entry by inspecting the live ChatGPT DOM in the embedded browser via the DevTools button. Record confirmed selectors in `BUILDING.md`.

---

## Status State Model

```
Starting
  └─> LoadingChatGPT
        ├─> NeedsLogin      (login required — detected via redirect heuristic or manual observation)
        │     └─> Ready
        └─> Ready           (already logged in)

Ready
  └─> InjectingPrompt
        ├─> Error           (injection failed)
        └─> Sending
              └─> WaitingForResponse
                    ├─> Complete
                    │     └─> Ready   (on next send)
                    └─> Error

Error
  └─> Ready                 (via Retry action)
```

**Send button** is enabled only in `Ready` and `Complete` states.

**`NeedsLogin` detection:** For the MVP this state may be entered manually or through a simple URL redirect heuristic. Automated DOM-based login detection is deferred to avoid fragility.

`AppState` is a standalone class with no JCEF or Swing dependency, making it straightforward to unit test in isolation.

---

## Prompt Encoding

User-supplied text must never be inserted through raw string substitution into a JavaScript source string. Quotes, backslashes, newlines, and non-ASCII characters will produce syntax errors or unintended behavior.

**Primary approach — JSON encoding:**
```java
String json = new Gson().toJson(promptText); // produces a quoted, escaped JSON string
String script = "injectPrompt(" + json + ");";
browser.executeJavaScript(script, "", 0);
```

JSON encoding keeps the script call readable in DevTools and logs.

**Fallback — Base64 encoding** (use if JSON argument injection proves unreliable in the target Chromium version):
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

All three values should live as named constants in one location so they can be tuned without touching logic code. The stability window starts at 1500ms to avoid premature extraction; tune down during DevCycle 005 if responses feel sluggish in real use.

---

## Windows Path Conventions

| Purpose | Path |
|---------|------|
| Browser profile / cache | `%LOCALAPPDATA%\ChatStory\profile` |
| User config file | `%APPDATA%\ChatStory\config.properties` |

`%LOCALAPPDATA%` is used for the browser profile because browser caches grow large and should not roam across machines. `%APPDATA%` is used for user config because it is the Windows convention for roaming per-user application settings.

```java
String localAppData = System.getenv("LOCALAPPDATA");
String appData      = System.getenv("APPDATA");
// Fall back to user.home subdirectory if either variable is unavailable
```

The target chat URL is stored under `target.chat.url` in the config file. Do not commit a real chat URL to source control. The default value in source is `https://chatgpt.com`.

---

## Unit-Testable Boundaries

These components have no JCEF or Swing dependency and must have unit tests:

| Component | What to test |
|-----------|-------------|
| `PromptEncoding` | JSON and base64 encoding against all edge cases listed above |
| `BridgeMessage` | Parsing of all message types; missing fields; malformed JSON; error code mapping |
| `AppState` | All valid transitions; invalid transition attempts; Send button enable rules |
| `ResourceLoadingTest` | All JS files and `chatgpt_selectors.json` exist and can be read as strings without error |

---

## Manual Validation Plan

These tests must all pass before the MVP is considered complete. Results should be recorded in the DevCycle 005 document.

### Test 1 — Browser Loads

1. Launch the application
2. Confirm ChatGPT appears in the embedded browser window

Pass: ChatGPT is functional inside the application.

### Test 2 — DevTools Accessible

1. Launch the application
2. Open DevTools using the DevTools button or menu item

Pass: DevTools opens and can inspect the live ChatGPT DOM.

### Test 3 — Login Persists

1. Log in using the intended authentication method
2. Close the application
3. Relaunch

Pass: The user is still logged in after restart.

### Test 4 — Hardcoded Injection Works

1. Navigate to a ChatGPT conversation in the embedded browser
2. Click the "Test Inject" button (present during DevCycle 003 only)

Pass: The test string appears in the ChatGPT editor, the send button becomes enabled, and the text can be submitted.

### Test 5 — Native Prompt Sends

1. Type a short prompt in the native input box
2. Click Send

Pass: The prompt appears correctly in ChatGPT as a new user message. Application status updates through `Sending` and `WaitingForResponse`.

### Test 6 — Response Extracts

1. Send a short prompt
2. Wait for generation to complete

Pass: The latest assistant response appears in the native output panel as plain text. Status reaches `Complete`.

### Test 7 — Repeated Sends

1. Send three short prompts in the same session, waiting for each to complete
2. Compare the native output panel against the browser after each response

Pass: Each extraction matches the correct response for that prompt. No stale or crossed responses. The output panel clears and repopulates correctly for each send.

### Test 8 — Failure Recovery

1. Trigger a known failure (e.g., attempt to send before the page is fully loaded)

Pass: The application shows a specific error message and transitions to a recoverable state. It does not hang or crash.

---

## Explicit Non-Goals for the MVP

These must not be added until the browser bridge is proven stable:

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

These must be resolved during DevCycle 001:

1. **Which exact JCEF artifact and version?** Verify Windows x64, Java 21, native binary inclusion, and `jpackage` compatibility before writing any application code.

2. **Which login method to test in DevCycle 001?** Use the authentication method the user will actually use day-to-day. A different method may not validate the real login flow.

3. **Should `BUILDING.md` be a standalone file or a section of `README.md`?** Decide during DevCycle 001 based on how many steps JCEF native binary setup requires. Use `README.md` if setup is simple; create a dedicated `BUILDING.md` if it requires several steps.

---

## Packaging Note

Packaging is not MVP work and is deferred. However, the JCEF artifact and integration approach chosen in DevCycle 001 must be compatible with a `jpackage`-based Windows installer in a future cycle. Verify this compatibility during DevCycle 001 before committing to an approach that works only inside an IDE.
