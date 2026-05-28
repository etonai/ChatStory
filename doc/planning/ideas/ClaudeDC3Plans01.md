# Codex DC3 Suggestions 01: Prompt Injection and Native Input

## Purpose

This document proposes the scope for DevCycle 003 after the successful completion of DevCycle 002.

DevCycle 001 proved the embedded browser was viable. DevCycle 002 turned the spike into a maintainable application foundation. DevCycle 003 should now prove the next technical premise:

Can the native application inject a prompt into ChatGPT's editor and submit it reliably through DOM interaction?

DevCycle 003 should stop after prompt submission is proven. Response completion detection and assistant response extraction should remain deferred to DevCycle 004.

## Recommended DevCycle

```text
DevCycle 003: Prompt Injection and Native Input
```

## Goal

Build the first real ChatGPT send bridge.

The app should let the user type a prompt in a native Swing input area, submit it through the established bridge, and confirm that ChatGPT received the prompt as a new user message.

## Desired Outcome

At the end of DevCycle 003:

- The application has a native input panel with a multiline text area and Send button.
- A temporary hardcoded injection action proves editor injection before user input is wired.
- Prompt text is safely encoded before being passed into JavaScript.
- JavaScript resources perform editor injection and send-button triggering.
- `ChatBridge.sendPrompt(...)` has a real implementation.
- `ResponseListener.onPromptSubmitted(...)` is called when send is confirmed.
- The app transitions through `InjectingPrompt` and `Sending`.
- Failed injection or send attempts produce specific error codes and user-visible status.
- The prompt appears in ChatGPT as a new user message.

## Explicit Non-Goals

DevCycle 003 should not include:

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

## Current Foundation From DC2

DC2 already provides:

- `AppState` with `InjectingPrompt`, `Sending`, `WaitingForResponse`, `Complete`, and `Error` states defined.
- `UiThread.run(...)` for EDT-safe UI updates.
- `DomBridge` with type-routed `CefMessageRouter` handling.
- `BridgeMessage`, `BridgeMessageHandler`, `ErrorCodes`, `ChatBridge`, and `ResponseListener`.
- `chatgpt_selectors.json` resource loading.
- `ping.js` resource loading.
- `AppFrame` with toolbar, status label, browser panel, and DevTools access.

DC3 should extend this foundation rather than bypass it.

## Suggested Target Additions

```text
src/main/java/com/chatstory/
  AppFrame.java                  update to include InputPanel

src/main/java/com/chatstory/bridge/
  ChatGptBridge.java             implements ChatBridge
  PromptEncoder.java             JSON-safe prompt argument encoding
  RequestIdGenerator.java         optional small helper

src/main/java/com/chatstory/ui/
  InputPanel.java                multiline input + Send button

src/main/resources/js/
  inject_prompt.js               editor injection script
  trigger_send.js                send confirmation script

src/test/java/com/chatstory/
  PromptEncoderTest.java
  ChatGptBridgeTest.java         only if practical without JCEF
```

Class names can change if implementation discovers a better fit, but the responsibilities should stay separated.

## Proposed Phases

### Phase 1: Live DOM Inspection And Selector Confirmation

**Status:** Planning

- [ ] Open the embedded browser and DevTools.
- [ ] Inspect the current ChatGPT editor DOM.
- [ ] Confirm the best prompt editor selector candidates.
- [ ] Confirm the best send button selector candidates.
- [ ] Confirm whether the submitted user message can be detected with a stable selector.
- [ ] Update `chatgpt_selectors.json` with confirmed candidates.
- [ ] Record confirmed selector assumptions in the DevCycle document or a short technical note.

**Technical Notes:**

Do this before writing injection behavior. The existing selector JSON contains candidates, not proven selectors.

Avoid over-investing in perfect selectors. DC3 only needs enough selector reliability to inject and confirm a sent user prompt.

### Phase 2: Prompt Encoding

**Status:** Planning

- [ ] Implement `PromptEncoder`.
- [ ] Encode user text using JSON string serialization as the primary path.
- [ ] Keep base64 encoding as fallback only if JSON argument injection proves unreliable.
- [ ] Add tests for:
  - empty prompt
  - quotes
  - backslashes
  - newlines
  - carriage returns
  - multi-paragraph text
  - non-ASCII text
  - emoji
- [ ] Reject or ignore blank/whitespace-only sends in the UI.

**Technical Notes:**

User text must never be inserted into JavaScript through raw string replacement.

JSON encoding is preferred because script calls remain readable in logs and DevTools.

### Phase 3: Hardcoded Injection Spike

**Status:** Planning

- [ ] Add a temporary "Test Inject" button to the toolbar or a small debug area.
- [ ] Add `inject_prompt.js` as a resource.
- [ ] Load `inject_prompt.js` through Java resource loading.
- [ ] Pass selector arrays and the encoded hardcoded test prompt into the script.
- [ ] Try `document.execCommand('selectAll')` plus `document.execCommand('insertText', false, text)` first.
- [ ] If that fails, test synthetic `InputEvent` insertion.
- [ ] Verify injection by checking:
  - editor text matches expected prompt
  - send button becomes enabled
- [ ] Return a structured `injectResult` bridge message through `cefQuery`.
- [ ] Show a clear error if injection fails.

**Technical Notes:**

Visual text in the editor is not enough. The send button becoming enabled is the better signal that ChatGPT's internal editor state accepted the input.

Keep the hardcoded test path until native send is stable. It can move behind a debug affordance rather than being deleted immediately.

### Phase 4: Trigger Send And Confirm User Message

**Status:** Planning

- [ ] Add `trigger_send.js` as a resource.
- [ ] Locate the send button using selector candidates.
- [ ] Confirm the send button exists.
- [ ] Confirm the send button is enabled.
- [ ] Click the send button.
- [ ] Confirm a new user message appears in the DOM within a short timeout.
- [ ] Return a structured `sendResult` bridge message through `cefQuery`.
- [ ] Call `ResponseListener.onPromptSubmitted(requestId)` after send confirmation.

**Technical Notes:**

Send confirmation should not mean "button click was attempted." It should mean a new user message appeared after the click.

If user-message detection is too fragile, DC3 may temporarily accept a weaker confirmation, but this should be recorded as a risk and fixed before response extraction depends on it.

### Phase 5: Implement `ChatBridge`

**Status:** Planning

- [ ] Implement a concrete `ChatBridge`, likely `ChatGptBridge`.
- [ ] Assign a new incrementing `requestId` for each send attempt.
- [ ] Register bridge handlers for `injectResult` and `sendResult`.
- [ ] Drop stale bridge messages whose `requestId` does not match the active request.
- [ ] Transition `AppState`:
  - `Ready` or `Complete` to `InjectingPrompt`
  - `InjectingPrompt` to `Sending`
  - `Sending` to `Ready` after prompt submission is confirmed
  - any failure to `Error`
- [ ] Route failures through `ResponseListener.onError(requestId, errorCode, message)`.
- [ ] Prevent concurrent sends while a request is active.

**Technical Notes:**

DC3 does not transition to `WaitingForResponse`; that belongs to DC4 when the app starts tracking assistant generation. For DC3, successful prompt submission can return the app to `Ready`.

This keeps the state model honest: after send confirmation, the browser may be generating, but the native app is not yet responsible for waiting on or extracting that response.

If the team wants to reserve `WaitingForResponse` immediately after send, document that response handling is still not implemented and the state must recover to `Ready` without extraction. My recommendation is to stay with `Ready` after submitted for DC3.

### Phase 6: Native Input Panel

**Status:** Planning

- [ ] Create `InputPanel` under `com.chatstory.ui`.
- [ ] Include a multiline `JTextArea`.
- [ ] Include a Send button.
- [ ] Place `InputPanel` below the browser in `AppFrame`.
- [ ] Keep the browser as the main visible area.
- [ ] Disable Send unless `AppState.isSendEnabled()` is true and the input has non-blank text.
- [ ] On Send, call `ChatBridge.sendPrompt(...)`.
- [ ] Clear the input only after send is confirmed.
- [ ] Keep status label updates clear during injection and sending.

**Technical Notes:**

Use a simple layout. Do not add prompt history, resend, templates, or semantic panes.

Enter-key behavior can wait unless it is trivial. If added, prefer Ctrl+Enter for send so multiline editing remains comfortable.

### Phase 7: Tests And Manual Validation

**Status:** Planning

- [ ] Add `PromptEncoderTest`.
- [ ] Add tests for any pure Java request ID or message routing helpers.
- [ ] Run existing `AppStateTest`, `BridgeMessageTest`, and `ResourceLoadingTest`.
- [ ] Run `gradlew.bat test`.
- [ ] Manually validate hardcoded injection.
- [ ] Manually validate native prompt send.
- [ ] Manually validate repeated prompt sends.
- [ ] Update `BUILDING.md` only if setup/run behavior changes.

**Technical Notes:**

Do not try to automate the live ChatGPT DOM in unit tests. Keep browser behavior manual for now.

## Error Codes

DC2 already defined the error vocabulary. DC3 should actively use these:

- `EDITOR_NOT_FOUND`
- `PROMPT_INJECTION_FAILED`
- `SEND_BUTTON_NOT_FOUND`
- `SEND_BUTTON_DISABLED`
- `SEND_CLICK_FAILED`
- `USER_MESSAGE_NOT_CONFIRMED`
- `BRIDGE_HANDLER_FAILED`
- `BRIDGE_MESSAGE_INVALID`

If a new send-specific failure appears, add it to `ErrorCodes` deliberately and test any parsing/handling that depends on it.

## Bridge Message Types

Suggested DC3 message types:

```text
injectResult
sendResult
error
```

Suggested `injectResult` shape:

```json
{
  "type": "injectResult",
  "requestId": 1,
  "ok": true,
  "text": null,
  "errorCode": null,
  "message": "Prompt injected"
}
```

Suggested `sendResult` shape:

```json
{
  "type": "sendResult",
  "requestId": 1,
  "ok": true,
  "text": null,
  "errorCode": null,
  "message": "Prompt submitted"
}
```

For errors:

```json
{
  "type": "error",
  "requestId": 1,
  "ok": false,
  "errorCode": "send_button_disabled",
  "message": "Send button was found but disabled"
}
```

## Manual Validation Plan

### Test 1: Existing Browser Behavior Still Works

Steps:

1. Run `gradlew.bat run`.
2. Confirm ChatGPT loads.
3. Confirm existing session is still active.
4. Confirm DevTools opens.

Pass condition:

- No regression from DC2.

### Test 2: Hardcoded Injection

Steps:

1. Open a ChatGPT conversation.
2. Click the temporary Test Inject button.

Pass condition:

- The hardcoded test prompt appears in the ChatGPT editor and enables the send button.

### Test 3: Hardcoded Send

Steps:

1. Use Test Inject.
2. Trigger send through the DC3 send script.

Pass condition:

- ChatGPT shows a new user message with the expected test prompt.

### Test 4: Native Prompt Send

Steps:

1. Type a short prompt into the native input panel.
2. Click Send.

Pass condition:

- The prompt appears in ChatGPT as a new user message.
- The native input clears after confirmation.
- The app returns to a send-ready state.

### Test 5: Prompt Encoding Edge Cases

Steps:

1. Send prompts containing quotes, backslashes, newlines, and non-ASCII text.

Pass condition:

- The submitted ChatGPT user message exactly matches the native input text.

### Test 6: Repeated Sends

Steps:

1. Send three short prompts in sequence.

Pass condition:

- Each prompt is submitted once.
- The UI does not allow overlapping sends.
- Stale bridge messages do not affect the current request.

### Test 7: Failure Recovery

Steps:

1. Attempt to send while not on a usable ChatGPT page or before the editor is available.

Pass condition:

- The app reports a specific error and can recover to `Ready`.

## Suggested Success Criteria

DevCycle 003 should be considered `Work Complete` when:

- `gradlew.bat run` launches the app with no DC2 regressions.
- DevTools remain accessible.
- `inject_prompt.js` and `trigger_send.js` load from resources.
- Confirmed selectors are recorded or noted.
- Hardcoded Test Inject works.
- Hardcoded send confirms a new user message.
- Native `InputPanel` can send a user-entered prompt.
- Prompt encoding edge cases are tested.
- Send button enablement follows `AppState`.
- Concurrent sends are prevented.
- Send failures produce specific error codes.
- Existing and new unit tests pass.
- Response extraction remains deferred to DC4.

Passing these criteria allows the DevCycle to be marked `Work Complete`; only the user may approve `Verified`.

## Risks

- ChatGPT editor selectors may have changed since the earlier planning docs.
- The editor may visually accept text while ChatGPT internal state does not.
- `document.execCommand(...)` may work now but remains a brittle Chromium/editor interaction.
- Send-button selector or enablement behavior may vary by ChatGPT UI rollout.
- Confirming a new user message may require a selector not yet present in `chatgpt_selectors.json`.
- Bridge messages can arrive after a request is stale; request ID matching must be strict.
- Returning to `Ready` after send is a temporary DC3 behavior; DC4 will introduce response waiting and extraction.

## Recommendation For DevCycle 004

If DC3 succeeds, DevCycle 004 should be:

```text
DevCycle 004: Completion Detection and Response Extraction
```

That cycle should build on the confirmed user-message send point, track the new assistant response, detect generation completion, and populate a native output panel.
