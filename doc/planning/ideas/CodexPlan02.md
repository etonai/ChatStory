# Codex Plan 02: Standalone Story Workstation MVP Implementation Plan

## Purpose

This document is the standalone implementation plan for the Story Workstation MVP.

It does not depend on earlier planning or review documents. It incorporates the settled decisions from the planning discussion and should be usable as the source for creating the first active DevCycle document.

## MVP Goal

Build the smallest native Windows desktop application that proves a Java application can host ChatGPT in an embedded Chromium browser and interact with it reliably enough to support later storytelling workflows.

The MVP is successful when the application can:

- Launch a native Java desktop window.
- Host ChatGPT in an embedded Chromium browser.
- Allow the user to log in manually.
- Preserve the logged-in session across restarts.
- Navigate to ChatGPT or an optional target chat.
- Send a prompt into ChatGPT from native UI.
- Detect when the assistant response is complete.
- Extract the latest assistant response.
- Display that response in a native output panel as readable plain text.

## Core Implementation Principles

- Prove the browser bridge before building storytelling features.
- Use JCEF as the only browser framework for the MVP.
- Keep the browser visible during MVP development.
- Prefer DOM interaction over mouse coordinates, screen scraping, or pixel matching.
- Keep all browser selectors centralized and easy to update.
- Treat the browser bridge as asynchronous from the start.
- Keep Swing UI updates on the Event Dispatch Thread.
- Do not commit real ChatGPT project or chat URLs.
- Do not add story parsing, continuity tools, correction workflows, or upload profiles until the bridge is proven.

## Explicit Non-Goals

The MVP will not include:

- Multi-pane semantic input.
- Dialogue/action separation.
- XML or tagged response protocols.
- Story-only parsing.
- Continuity tracking.
- Relationship state tracking.
- Correction workflows.
- Upload profiles.
- Session archives.
- Project or chat selection UI.
- Prompt template management.
- Prompt history.
- Resend workflow.
- Installer packaging.

## Technology Decisions

### Language

- Java 21 or newer.

### Build System

- Gradle is preferred.
- Kotlin DSL is acceptable, but the exact build structure should be chosen during the first implementation cycle.

### Browser Framework

- JCEF is required for the MVP.
- JavaFX WebView is not a practical fallback because it is WebKit-based and does not validate the Chromium browser bridge.

### UI Framework

- Swing for the MVP.
- Swing integrates directly with JCEF browser components and is sufficient for the first version.

### Browser Profile Location

Preferred Windows paths:

- Config: `%APPDATA%\ChatStory\config.properties`
- Browser profile/cache: `%LOCALAPPDATA%\ChatStory\profile`

Fallback:

- If environment variables are unavailable, use a directory under `user.home`.

## Application Architecture

```text
Story Workstation MVP
|
+-- App Launcher
+-- Swing App Frame
+-- JCEF Browser Host
+-- Browser Profile Configuration
+-- DevTools Access
+-- DOM Bridge
|   |
|   +-- command injection through executeJavaScript
|   +-- JS-to-Java results through CefMessageRouter
|   +-- requestId matching
|   +-- structured bridge messages
|
+-- State Model
+-- Native Input Panel
+-- Native Output Panel
+-- Debug/Status Surface
+-- Script Resources
    |
    +-- chatgpt_selectors.json
    +-- inject_prompt.js
    +-- trigger_send.js
    +-- detect_generation.js
    +-- extract_latest_response.js
```

## Browser Bridge Design

The bridge must be asynchronous. JavaScript execution in JCEF does not return values directly to Java.

### Java-Side Interface

```java
interface ChatBridge {
    void sendPrompt(String prompt, ResponseListener listener);
}

interface ResponseListener {
    void onPromptSubmitted(long requestId);
    void onResponsePartial(long requestId, String responseText);
    void onResponseComplete(long requestId, String responseText);
    void onError(long requestId, String errorCode, String message);
}
```

Notes:

- `requestId` is required, not optional.
- Java assigns a new `requestId` before each bridge operation.
- JavaScript includes that same `requestId` in every callback message.
- Java drops stale messages whose `requestId` does not match the active pending operation.
- `onResponsePartial` may be a no-op in the MVP, but it stays in the interface for future streaming output.

### JS-to-Java Communication

Use `CefMessageRouter`.

JavaScript sends results through `window.cefQuery(...)` using a structured JSON envelope.

Example:

```json
{
  "type": "responseComplete",
  "requestId": 12,
  "ok": true,
  "text": "assistant response",
  "errorCode": null,
  "message": null
}
```

Suggested message types:

- `ping`
- `injectResult`
- `sendResult`
- `generationStatus`
- `responsePartial`
- `responseComplete`
- `error`

### Java-to-JavaScript Commands

Use `CefBrowser.executeJavaScript(...)` to inject commands only.

Do not rely on `executeJavaScript(...)` returning a value.

User-provided prompt text must never be inserted through raw string replacement. Use centralized safe encoding.

Preferred order:

1. JSON serialization for prompt arguments, because it remains readable in DevTools and logs.
2. Base64 encoding if JSON argument injection proves unreliable.

Both approaches must be tested with:

- Empty strings.
- Quotes.
- Backslashes.
- Newlines.
- Long prompts.
- Non-ASCII text.

## State Model

The app state model is the source of truth for UI enablement and status display.

Initial states:

```text
Starting
LoadingChatGPT
NeedsLogin
Ready
InjectingPrompt
Sending
WaitingForResponse
Complete
Error
```

Rules:

- Send is enabled only in `Ready` and `Complete`.
- Browser load events update page-related states.
- Bridge operations update prompt/response states.
- `Complete` may transition back to `Ready` when the UI has displayed the response.
- `Error` shows a clear message and allows user recovery.
- `isReady()` or `isGenerating()` helper methods, if added, must derive from this state model rather than maintain separate flags.

## Swing Threading Rule

JCEF callbacks do not run on the Swing Event Dispatch Thread.

Every Swing update caused by JCEF must be wrapped through a helper like this:

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

This applies to:

- Output panel updates.
- Status label updates.
- Button enablement.
- Error messages.
- Debug status text.
- Load event handling that touches UI.

## Selector Strategy

Selectors should be centralized in one resource file:

```text
src/main/resources/js/chatgpt_selectors.json
```

Example shape:

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
  "assistantMessage": [
    "[data-message-author-role='assistant']"
  ]
}
```

Java loads this file and passes the relevant selector arrays into JavaScript commands as JSON arguments.

Benefits:

- Selector changes are isolated.
- Scripts can try multiple selector candidates in order.
- The plan avoids scattering selectors across Java constants and multiple scripts.

## Prompt Injection Strategy

Assume ChatGPT uses a `contenteditable` editor, not a plain textarea.

The first prompt-injection work should be a spike with a hardcoded test string and a simple "Test Inject" button. This proves the editor interaction before building the full native input panel.

Injection strategies to test, in order:

1. Focus editor and use `document.execCommand('insertText', false, text)`.
2. Dispatch a synthetic `InputEvent` with `inputType: 'insertText'`.
3. Try clipboard/paste simulation if needed.

Verification must check more than visual text:

- The editor contains the exact intended prompt text.
- The send button becomes enabled.
- After clicking Send, a new user message appears with the exact intended text.

## Send Strategy

The send script should:

- Locate the send button using selector candidates.
- Confirm the button exists.
- Confirm the button is enabled.
- Click the button.
- Confirm a new user message appears within a short timeout.

Structured failure codes should include:

- `editor_not_found`
- `prompt_injection_failed`
- `send_button_not_found`
- `send_button_disabled`
- `send_click_failed`
- `user_message_not_confirmed`

## Completion Detection Strategy

Use polling for the MVP. MutationObserver can be considered later.

Initial defaults:

- Poll interval: 500 ms.
- Text stability window: 600 to 800 ms.
- Maximum wait timeout: 180 seconds.

Completion should combine signals:

- A new assistant message exists after the prompt send.
- Generation controls such as a stop button are absent.
- The assistant message text has remained stable for the stability window.

If timeout occurs:

- Move to `Error` or a "possibly partial" error state.
- Preserve any extracted text for debugging.
- Show a clear message to the user.

## Response Extraction Strategy

The extraction script should:

- Track the user send operation by `requestId`.
- Identify the new assistant message created after that send.
- Prefer an inner content node rather than the whole assistant message container.
- Extract readable plain text.
- Preserve line breaks well enough for story text to remain readable.
- Avoid including UI controls, labels, copy buttons, or unrelated page text.

Full Markdown preservation is not required for the MVP.

## Debugging Requirements

During MVP development, the embedded browser is the source of truth.

The app should include:

- A visible embedded browser.
- Easy access to JCEF DevTools.
- Basic status display.
- Logging for bridge operations and failures.

Phase 0 may use console logging first. A small debug status bar can be added once the browser is launching reliably.

Useful debug fields:

- Current app state.
- Last bridge operation.
- Last request ID.
- Last error code.
- Last extracted text length.

## Configuration

For the first browser spike, a Java constant may be used:

```text
https://chatgpt.com/
```

Do not commit real project or chat URLs.

After the browser and login flow work, optional config can be added:

```properties
target.chat.url=https://chatgpt.com/
```

Suggested path:

```text
%APPDATA%\ChatStory\config.properties
```

## Development Phases

### Phase 0: JCEF Feasibility Spike

Goal:

Prove that JCEF can launch on this machine, render ChatGPT, support manual login, preserve session data, and expose DevTools.

Tasks:

- Research and choose the exact JCEF distribution and version.
- Verify Windows x64 and Java 21 compatibility.
- Confirm how native binaries are acquired and loaded.
- Create the smallest runnable Java/Swing/JCEF prototype.
- Load `https://chatgpt.com`.
- Add a simple way to open DevTools.
- Configure browser profile/cache storage.
- Attempt the user's real login workflow.
- Restart the app and confirm the session persists.
- Document the run command and native binary setup.

Success criteria:

- JCEF launches successfully.
- ChatGPT renders in the embedded browser.
- DevTools can open.
- Manual login succeeds.
- Login persists after restart.
- Build/run setup is documented.

Non-goals:

- Native prompt input.
- Prompt injection.
- Response extraction.
- Final package structure.
- Quality-of-life UI.

### Phase 1: Browser Scaffold And State Foundation

Goal:

Turn the spike into a small maintainable application foundation.

Tasks:

- Establish the Gradle project structure.
- Create the application entry point.
- Create the Swing app frame.
- Create the browser host component.
- Add the app state model.
- Add the `UiThread.run(...)` helper.
- Add basic status display.
- Establish `CefMessageRouter`.
- Verify a minimal JS-to-Java `ping` message.
- Add `chatgpt_selectors.json` resource loading.

Success criteria:

- App launches through a repeatable Gradle command.
- Browser host code is separated from UI code.
- State transitions can be represented cleanly.
- JS can send a structured message back to Java.
- Selectors load from a centralized JSON resource.

### Phase 2: Prompt Injection And Native Input

Goal:

Prove that native input can place text into ChatGPT and submit it.

Tasks:

- Add a hardcoded "Test Inject" button.
- Test contenteditable injection strategies.
- Choose and document the working injection method.
- Verify send button enablement after injection.
- Verify a new user message appears after send.
- Add the native input panel.
- Wire input panel send behavior through `ChatBridge`.
- Use safe prompt encoding.
- Add structured error reporting for injection and send failures.

Success criteria:

- A hardcoded test prompt can be injected and sent.
- A native typed prompt can be injected and sent.
- Send failures produce clear error codes.
- Send is disabled while another send is active.

### Phase 3: Completion Detection And Response Extraction

Goal:

Detect assistant response completion and display the extracted latest response in native UI.

Tasks:

- Track `requestId` for send operations.
- Record message count or message identity before send.
- Poll for new assistant message and generation status.
- Apply text stability detection.
- Add 180-second timeout behavior.
- Extract readable plain text from the new assistant message.
- Add output panel.
- Display completed response through `ResponseListener`.
- Preserve line breaks.
- Add repeated-send validation.

Success criteria:

- The app waits until a response appears complete.
- The latest assistant response appears in the native output panel.
- Repeated sends capture the correct new response.
- Timeout behavior is visible and recoverable.

### Phase 4: MVP Hardening And Documentation

Goal:

Make the MVP reliable enough for local evaluation and future planning.

Tasks:

- Improve status and error messages.
- Add minimal bridge operation logging.
- Add retry behavior for temporary DOM lookup failures.
- Add a copy response button if trivial.
- Document confirmed selectors.
- Document known limitations.
- Document manual validation steps.
- Add unit tests for non-browser logic.
- Update README or create `BUILDING.md` with setup instructions.

Success criteria:

- A user can run the app, log in, send prompts, and view responses with understandable status feedback.
- Setup steps are documented.
- Known selector and JCEF limitations are recorded.
- Non-browser logic has focused tests where practical.

## Suggested DevCycle Breakdown

### DevCycle 001: JCEF Browser Viability

Includes:

- Phase 0.
- Minimal build/run documentation.

### DevCycle 002: Application Foundation

Includes:

- Phase 1.
- State model.
- EDT helper.
- Message router ping.
- Selector JSON loading.

### DevCycle 003: Prompt Send Bridge

Includes:

- Phase 2.
- Hardcoded injection spike.
- Native input panel.
- Safe prompt encoding.
- Send confirmation.

### DevCycle 004: Response Bridge

Includes:

- Phase 3.
- Completion polling.
- Response extraction.
- Output panel.
- Repeated-send validation.

### DevCycle 005: MVP Hardening

Includes:

- Phase 4.
- Status polish.
- Logging.
- Retry behavior.
- Documentation.
- Focused tests.

## Manual Validation Plan

### Test 1: Browser Launch

Steps:

1. Launch the app.
2. Confirm the JCEF browser appears.
3. Confirm ChatGPT loads.

Pass condition:

- ChatGPT is usable in the embedded browser.

### Test 2: DevTools

Steps:

1. Launch the app.
2. Open DevTools from the app.

Pass condition:

- DevTools opens and can inspect the ChatGPT page.

### Test 3: Login Persistence

Steps:

1. Log in manually.
2. Close the app.
3. Relaunch the app.

Pass condition:

- The user remains logged in.

### Test 4: Hardcoded Injection

Steps:

1. Open ChatGPT in the embedded browser.
2. Click the hardcoded Test Inject button.

Pass condition:

- The test prompt appears in the editor, enables Send, and can be submitted.

### Test 5: Native Prompt Send

Steps:

1. Type a short prompt in the native input.
2. Click Send.

Pass condition:

- The prompt is submitted to ChatGPT and appears as a new user message.

### Test 6: Response Extraction

Steps:

1. Send a short prompt.
2. Wait for response completion.

Pass condition:

- The native output panel shows the latest assistant response.

### Test 7: Repeated Sends

Steps:

1. Send three prompts in the same chat.
2. Compare browser output with native output after each response.

Pass condition:

- The native output panel always shows the response for the latest prompt, not an older response.

## Unit-Testable Areas

The ChatGPT browser bridge itself requires manual validation, but several parts can be tested without JCEF:

- Prompt JSON encoding.
- Optional prompt base64 encoding.
- Bridge message parsing.
- Request ID matching and stale response discard.
- State transitions.
- Send button enablement rules.
- Resource loading for JavaScript and selector JSON.
- Error code mapping.

## Open Questions Before DevCycle 001

1. Which exact JCEF distribution and version will be used?
2. Will native binaries be managed by Gradle, manually downloaded, or handled by a setup script?
3. What is the simplest repeatable Windows launch command?
4. Which login method should be used for the representative Phase 0 login test?
5. Should Phase 0 include a minimal `CefMessageRouter` ping, or should that wait for Phase 1?
6. Should setup notes go directly into `README.md`, or into a new `BUILDING.md`?

## Completion Definition

The MVP is complete when:

- JCEF launches reliably on the target Windows development machine.
- ChatGPT loads and supports manual login.
- Login persists across restarts.
- The app can send native prompts to ChatGPT.
- The app can detect assistant response completion.
- The app can extract and display the latest assistant response.
- Repeated sends are validated.
- Known setup, selector, and browser bridge limitations are documented.

The MVP should stop at `Work Complete` in DevCycle status until the user explicitly approves `Verified`.
