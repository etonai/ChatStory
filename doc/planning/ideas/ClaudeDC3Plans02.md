# DC3 Plan: Prompt Injection and Native Input

## Purpose

DevCycle 003 proves the next technical premise: can the native application inject a prompt into ChatGPT's editor and submit it reliably through DOM interaction?

DevCycle 001 proved the embedded browser was viable. DevCycle 002 turned the spike into a maintainable application foundation. DevCycle 003 builds the first real ChatGPT send bridge on top of that foundation.

DevCycle 003 stops after prompt submission is confirmed. Response completion detection and assistant response extraction are deferred to DevCycle 004.

---

## Goal

The app should let the user type a prompt in a native Swing input area, submit it through the established bridge, and confirm that ChatGPT received the prompt as a new user message.

---

## Desired Outcome

At the end of DevCycle 003:

- The application has a native input panel with a multiline text area and Send button.
- A hardcoded injection action proves editor injection before user input is wired.
- Prompt text is safely encoded before being passed into JavaScript.
- JavaScript resources perform editor injection and send-button triggering.
- `ChatBridge.sendPrompt(...)` has a real implementation.
- `ResponseListener.onPromptSubmitted(...)` is called when send is confirmed.
- The app transitions through `InjectingPrompt` and `Sending`.
- Failed injection or send attempts produce specific error codes and user-visible status.
- The prompt appears in ChatGPT as a new user message.

---

## Explicit Non-Goals

DevCycle 003 does not include:

- assistant response completion detection
- assistant response extraction
- native output panel
- story parsing
- XML/tagged response formats
- prompt history
- resend behavior
- MutationObserver logic
- file upload workflows
- full Markdown rendering

DC3 proves sending only. Reading responses belongs in DC4.

---

## Known Limitation

Once `InputPanel` exists, the embedded ChatGPT browser should be treated as display and debug UI, not the primary input path. If the user manually types into the embedded ChatGPT editor and then uses the native Send flow, the injection script will clear and overwrite whatever is in the browser editor. This is acceptable for DC3 and does not need to be fixed here.

---

## Current Foundation

DC2 already provides:

- `AppState` with `InjectingPrompt`, `Sending`, `WaitingForResponse`, `Complete`, and `Error` states defined.
- `UiThread.run(...)` for EDT-safe UI updates.
- `DomBridge` with type-routed `CefMessageRouter` handling.
- `BridgeMessage`, `BridgeMessageHandler`, `ErrorCodes`, `ChatBridge`, and `ResponseListener`.
- `chatgpt_selectors.json` resource loading.
- `ping.js` resource loading.
- `AppFrame` with toolbar, status label, browser panel, and DevTools access.

DC3 extends this foundation rather than bypassing it.

---

## Target Additions

```text
src/main/java/com/chatstory/
  AppFrame.java                  update to include InputPanel

src/main/java/com/chatstory/bridge/
  ChatGptBridge.java             implements ChatBridge
  PromptEncoder.java             JSON-safe prompt argument encoding
  RequestIdGenerator.java        optional small helper

src/main/java/com/chatstory/ui/
  InputPanel.java                multiline input + Send button

src/main/resources/js/
  inject_prompt.js               editor injection script
  trigger_send.js                send-button and confirmation script

src/test/java/com/chatstory/
  PromptEncoderTest.java
  ChatGptBridgeTest.java         only if practical without JCEF
```

Class names can change if implementation discovers a better fit, but the responsibilities should stay separated.

---

## Script Contracts

Define these contracts before writing any injection code.

### `inject_prompt.js`

Defines a global function:

```javascript
window.chatStoryInjectPrompt = function(options) { ... }
```

Input shape:

```json
{
  "requestId": 1,
  "selectors": {
    "promptEditor": ["..."],
    "sendButton": ["..."]
  },
  "text": "prompt text here"
}
```

The script calls `window.cefQuery` directly with the result, matching the `ping.js` pattern.

Success message:

```json
{
  "type": "injectResult",
  "requestId": 1,
  "ok": true,
  "message": "Prompt injected"
}
```

Failure message:

```json
{
  "type": "injectResult",
  "requestId": 1,
  "ok": false,
  "errorCode": "editor_not_found",
  "message": "Prompt editor not found"
}
```

### `trigger_send.js`

Defines a global function:

```javascript
window.chatStoryTriggerSend = function(options) { ... }
```

Input shape:

```json
{
  "requestId": 1,
  "selectors": {
    "sendButton": ["..."],
    "userMsg": ["..."]
  },
  "expectedText": "prompt text here",
  "timeoutMs": 5000
}
```

The script calls `window.cefQuery` directly with the result.

Success message:

```json
{
  "type": "sendResult",
  "requestId": 1,
  "ok": true,
  "message": "Prompt submitted"
}
```

Failure message:

```json
{
  "type": "sendResult",
  "requestId": 1,
  "ok": false,
  "errorCode": "user_message_not_confirmed",
  "message": "No matching user message appeared after send"
}
```

---

## Proposed Phases

### Phase 1: Live DOM Inspection and Selector Confirmation

**Status:** Planning

- [ ] Open the embedded browser and DevTools.
- [ ] Inspect the current ChatGPT editor DOM.
- [ ] Confirm the best prompt editor selector. Verify whether `[contenteditable='true'][data-id]` is valid — confirm the `data-id` attribute is present.
- [ ] Confirm the best send button selector. Note the exact `data-testid` value at time of inspection.
- [ ] Confirm a stable selector for submitted user message nodes. Add this as `userMsg` in `chatgpt_selectors.json`.
- [ ] Update `chatgpt_selectors.json` with all confirmed selectors.
- [ ] Record confirmed selectors and the inspection date in the DevCycle document.

**Technical Notes:**

Do this before writing any injection code. The existing selector JSON contains candidates, not proven selectors.

Avoid over-investing in perfect selectors. DC3 only needs enough reliability to inject and confirm a sent user prompt.

Selector findings are time-stamped facts, not permanent truths. The inspection date matters.

Use `userMsg` as the key name for user message selectors to match the naming of `assistantMsg`.

---

### Phase 2: Prompt Encoding

**Status:** Planning

- [ ] Implement `PromptEncoder`.
- [ ] Encode user text using JSON string serialization as the primary path. The encoder should produce a JS string literal ready to embed as a function argument — including surrounding quotes and all escape sequences.
- [ ] Keep base64 encoding as a fallback only if JSON argument injection proves unreliable.
- [ ] Add tests for:
  - empty prompt
  - quotes
  - backslashes
  - newlines
  - carriage returns
  - multi-paragraph text
  - non-ASCII text
  - emoji
  - very long prompt (~5,000 characters)
- [ ] Reject or ignore blank/whitespace-only sends in the UI.

**Technical Notes:**

User text must never be inserted into JavaScript through raw string replacement.

JSON encoding is preferred because script calls remain readable in logs and DevTools.

The 5,000-character test does not prove browser-side success, but catches Java-side encoding mistakes and documents behavior near argument size limits.

Phase 2 has no browser dependency and can be completed before Phase 1 inspection results are finalized.

---

### Phase 3: Hardcoded Injection Spike

**Status:** Planning

- [ ] Add a "Test Inject" button to the toolbar or debug area.
- [ ] Add `inject_prompt.js` as a resource using the function definition pattern from the script contract above.
- [ ] Load `inject_prompt.js` through Java resource loading.
- [ ] Call `window.chatStoryInjectPrompt(options)` by passing a JSON options object as the argument. Do not use string template substitution inside the script source.
- [ ] Implement the injection fallback sequence in this order:
  1. `document.execCommand('selectAll')` plus `document.execCommand('insertText', false, text)`.
  2. If send button remains disabled: paste simulation via `ClipboardEvent` with `DataTransfer` containing the text.
  3. If still disabled: dispatch a manual `keydown` / `beforeinput` / `input` / `keyup` event sequence.
- [ ] Verify injection by confirming the send button becomes enabled. Visual text in the editor is not sufficient — the send button becoming enabled is the signal that React's internal state accepted the input.
- [ ] Return a structured `injectResult` bridge message through `cefQuery`.
- [ ] Show a clear error in the status label if injection fails.
- [ ] Record which fallback approach succeeded in the DevCycle completion notes.

**Technical Notes:**

ChatGPT's editor is React-controlled. `document.execCommand('insertText')` triggers native browser input events that React's synthetic event system intercepts. This generally works in embedded Chromium but has broken before in ChatGPT UI rollouts.

Keep the hardcoded Test Inject path after native send is working. Move it behind a debug menu item rather than deleting it — it is a valuable diagnostic for future selector drift and editor behavior changes.

---

### Phase 4: Trigger Send and Confirm User Message

**Status:** Planning

- [ ] Add `trigger_send.js` as a resource using the function definition pattern from the script contract above.
- [ ] Load `trigger_send.js` through Java resource loading.
- [ ] Call `window.chatStoryTriggerSend(options)` with the options object including `timeoutMs: 5000`.
- [ ] In the script: locate the send button, confirm it exists, confirm it is enabled, click it.
- [ ] After the click, poll for a new user message node using the `userMsg` selector within the timeout window.
- [ ] For user-message confirmation: compare the new message text exactly against the expected text. If exact match fails, log both values. Allow normalization for line endings only if needed and only when documented. Do not accept broad fuzzy matching.
- [ ] Return a structured `sendResult` bridge message through `cefQuery`.
- [ ] Call `ResponseListener.onPromptSubmitted(requestId)` after send confirmation.

**Technical Notes:**

Send confirmation does not mean "button click was attempted." It means a new user message appeared in the DOM after the click.

If user-message detection is too fragile, DC3 may temporarily accept a weaker confirmation (button-click-only), but this must be recorded as a risk and fixed before response extraction depends on it.

Define timeout constants in Java, not magic numbers:

```java
static final int USER_MESSAGE_CONFIRM_TIMEOUT_MS = 5000;
static final int SEND_OPERATION_TIMEOUT_MS = 10000;
```

`SEND_OPERATION_TIMEOUT_MS` guards the overall operation from being stuck in `Sending` if the bridge callback never arrives.

---

### Phase 5: Implement ChatBridge

**Status:** Planning

- [ ] Implement `ChatGptBridge` as the concrete `ChatBridge`.
- [ ] Assign a new incrementing `requestId` for each send attempt.
- [ ] Register bridge handlers for `injectResult` and `sendResult`.
- [ ] For stale bridge messages (requestId does not match active request): drop the message so it does not affect UI state; log the stale requestId and the current requestId at warn level. Do not silently discard.
- [ ] Transition `AppState`:
  - `Ready` or `Complete` → `InjectingPrompt`
  - `InjectingPrompt` → `Sending`
  - `Sending` → `Ready` after prompt submission is confirmed
  - any failure → `Error`
- [ ] Route failures through `ResponseListener.onError(requestId, errorCode, message)`.
- [ ] Enforce concurrent send prevention inside `ChatGptBridge.sendPrompt()`: if `AppState.current()` is not `Ready` or `Complete`, reject the call immediately and invoke `listener.onError(...)`. Do not rely solely on button state.

**Technical Notes:**

DC3 does not transition to `WaitingForResponse`. That belongs to DC4 when the app starts tracking assistant generation. For DC3, successful prompt submission returns the app to `Ready`. This is temporary DC3 behavior; DC4 will introduce response waiting and extraction and will change the post-send transition.

`Sending` → `Ready` must be guarded by `SEND_OPERATION_TIMEOUT_MS`. If the bridge callback does not arrive within the timeout, fail with a specific error rather than leaving the app stuck in `Sending`.

---

### Phase 6: Native Input Panel

**Status:** Planning

- [ ] Create `InputPanel` under `com.chatstory.ui`.
- [ ] Include a multiline `JTextArea`.
- [ ] Include a Send button.
- [ ] Place `InputPanel` below the browser in `AppFrame`.
- [ ] Keep the browser as the main visible area.
- [ ] Register an `AppState.StateListener` in `InputPanel` to update Send button enablement reactively. Do not poll `AppState.isSendEnabled()` only at send time.
- [ ] Send button enabled condition: `appState.isSendEnabled() && inputText.trim().length() > 0`. All updates through `UiThread.run(...)`.
- [ ] On Send, call `ChatBridge.sendPrompt(...)`.
- [ ] Clear the input only after send is confirmed.
- [ ] Keep status label updates clear during injection and sending.
- [ ] Prefer Ctrl+Enter for send so multiline editing remains comfortable. Plain Enter inserts a newline.

**Technical Notes:**

Use a simple layout. Do not add prompt history, resend, templates, or semantic panes.

The `StateListener` pattern should be consistent with how `AppFrame`'s status label already works.

---

### Phase 7: Tests and Manual Validation

**Status:** Planning

- [ ] Add `PromptEncoderTest` with the full list from Phase 2, including the 5,000-character case.
- [ ] Add tests for any pure Java request ID or message routing helpers.
- [ ] Run existing `AppStateTest`, `BridgeMessageTest`, and `ResourceLoadingTest`.
- [ ] Run `gradlew.bat test` and confirm all tests pass.
- [ ] Manually validate: hardcoded injection, native prompt send, repeated sends.
- [ ] Update `BUILDING.md` only if setup or run behavior changes.

**Technical Notes:**

Do not try to automate the live ChatGPT DOM in unit tests. Keep browser behavior manual.

---

## Error Codes

All of these are already defined in `ErrorCodes` from DC2. DC3 actively uses them:

- `EDITOR_NOT_FOUND`
- `PROMPT_INJECTION_FAILED`
- `SEND_BUTTON_NOT_FOUND`
- `SEND_BUTTON_DISABLED`
- `SEND_CLICK_FAILED`
- `USER_MESSAGE_NOT_CONFIRMED`
- `BRIDGE_HANDLER_FAILED`
- `BRIDGE_MESSAGE_INVALID`

If a new failure mode appears during implementation, add it to `ErrorCodes` deliberately and update any parsing or handling that depends on it.

---

## Bridge Message Types

DC3 message types:

```text
injectResult
sendResult
error
```

Full shapes are defined in the Script Contracts section above.

---

## Timeout Constants

```java
static final int USER_MESSAGE_CONFIRM_TIMEOUT_MS = 5000;
static final int SEND_OPERATION_TIMEOUT_MS = 10000;
```

---

## Manual Validation Plan

### Test 1: No Regression From DC2

Steps:
1. Run `gradlew.bat run`.
2. Confirm ChatGPT loads and session is active.
3. Confirm DevTools opens.

Pass: No DC2 regression.

### Test 2: Hardcoded Injection

Steps:
1. Open a ChatGPT conversation.
2. Click the Test Inject button.

Pass: The hardcoded test prompt appears in the ChatGPT editor and the send button becomes enabled.

### Test 3: Hardcoded Send

Steps:
1. Use Test Inject.
2. Trigger send through the send script.

Pass: ChatGPT shows a new user message with the expected test prompt.

### Test 4: Native Prompt Send

Steps:
1. Type a short prompt into the native input panel.
2. Click Send.

Pass:
- The prompt appears in ChatGPT as a new user message.
- The native input clears after confirmation.
- The app returns to a send-ready state.

### Test 5: Prompt Encoding Edge Cases

Steps:
1. Send prompts containing quotes, backslashes, newlines, non-ASCII characters, and emoji.

Pass: The submitted ChatGPT user message exactly matches the native input text.

### Test 6: Repeated Sends

Steps:
1. Send three short prompts in sequence.

Pass:
- Each prompt is submitted once.
- The UI does not allow overlapping sends.
- Stale bridge messages do not affect the current request.

### Test 7: Failure Recovery

Steps:
1. Attempt to send while not on a usable ChatGPT page or before the editor is available.

Pass: The app reports a specific error and can recover to `Ready`.

---

## Success Criteria

DevCycle 003 is `Work Complete` when:

- `gradlew.bat run` launches with no DC2 regressions.
- DevTools remain accessible.
- `inject_prompt.js` and `trigger_send.js` load from resources.
- Confirmed selectors are recorded with inspection date.
- Hardcoded Test Inject works.
- Hardcoded send confirms a new user message appeared in the DOM.
- Native `InputPanel` can send a user-entered prompt.
- Prompt encoding edge cases are tested.
- Send button enablement follows `AppState` reactively.
- Concurrent sends are prevented at the bridge layer.
- Send failures produce specific error codes.
- Stale request ID logging is in place.
- All existing and new unit tests pass.
- Response extraction remains deferred to DC4.

Only the user may approve `Verified`.

---

## Risks

- ChatGPT editor selectors may have changed since planning. Phase 1 inspection gates the rest.
- The editor may visually accept text while ChatGPT's React internal state does not. The send button becoming enabled is the correct signal.
- `document.execCommand(...)` may work now but remains a brittle Chromium/editor interaction. The three-step fallback sequence in Phase 3 manages this risk.
- Send-button selector or enablement behavior may vary by ChatGPT UI rollout.
- User-message confirmation may require a selector that proves fragile. If so, document the weaker fallback explicitly.
- Bridge messages can arrive after a request is stale. RequestId matching must be strict.
- Returning to `Ready` after send is temporary DC3 behavior. DC4 will introduce response waiting and change the post-send transition.

---

## Recommendation For DevCycle 004

If DC3 succeeds, DevCycle 004 should be:

```text
DevCycle 004: Completion Detection and Response Extraction
```

DC4 should build on the confirmed user-message send point, track the new assistant response, detect generation completion, and populate a native output panel.

---

## Implementation Note: Known Bug

During DC3 manual validation, prompt injection did not enable the ChatGPT send button.

This means the current injection path can place or attempt to place text into the editor, but ChatGPT's internal editor state is not being updated in the way required for the send button to become active. This should be treated as a DC3 bug and resolved before relying on native prompt sending or moving on to response extraction in DC4.
