# DevCycle 003: Prompt Injection and Native Input

**Status:** Verified
**Start Date:** 2026-05-28
**Target Completion:** 2026-06-25
**Focus:** Prove that the native application can inject a prompt into ChatGPT's editor and submit it through DOM interaction.

---

## Goal

Build the first real ChatGPT send bridge on top of the DC2 application foundation. The app should let the user type a prompt in a native Swing input area, submit it through the established browser bridge, and confirm that ChatGPT received the prompt as a new user message.

DevCycle 003 stops after prompt submission is confirmed. Assistant response completion detection and assistant response extraction are deferred to DevCycle 004.

## Desired Outcome

At the end of DevCycle 003:

- The application has a native input panel with a multiline text area and Send button.
- A hardcoded injection action proves editor injection before user input is relied on.
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

Once `InputPanel` exists, the embedded ChatGPT browser should be treated as display and debug UI, not the primary input path. If the user manually types into the embedded ChatGPT editor and then uses the native Send flow, the injection script may clear and overwrite whatever is in the browser editor. This is acceptable for DC3 and does not need to be fixed here.

---

## Tasks

### Phase 1: Live DOM Inspection and Selector Confirmation

**Status:** Work Complete

- [x] Open the embedded browser and DevTools.
- [x] Inspect the current ChatGPT editor DOM through validation of the working selectors.
- [x] Confirm the best prompt editor selector through Test Inject.
- [x] Confirm the best send button selector through Test Inject and send validation.
- [x] Confirm a stable selector for submitted user message nodes through native send validation.
- [x] Add `userMsg` to `chatgpt_selectors.json`.
- [x] Add additional selector fallbacks for prompt editor and send button.
- [x] Record confirmed selectors and inspection date in this DevCycle document.

**Progress Notes:**

`chatgpt_selectors.json` now includes:

- `promptEditor`
- `sendButton`
- `stopButton`
- `assistantMsg`
- `userMsg`

Current selector values have been validated through successful Test Inject, hardcoded send, native prompt send, and repeated native sends.

**Technical Notes:**

Selector findings are time-stamped facts, not permanent truths. If ChatGPT changes its UI, these selectors may need to be updated.

### Phase 2: Prompt Encoding

**Status:** Work Complete

- [x] Implement `PromptEncoder`.
- [x] Encode user text using JSON string serialization as the primary path.
- [x] Keep base64 encoding as fallback.
- [x] Add tests for empty prompt.
- [x] Add tests for quotes.
- [x] Add tests for backslashes.
- [x] Add tests for newlines and carriage returns.
- [x] Add tests for multi-paragraph text.
- [x] Add tests for non-ASCII text and emoji.
- [x] Add test for very long prompt around 5,000 characters.
- [x] Reject or ignore blank/whitespace-only sends in the UI.

**Technical Notes:**

Implemented in:

- `src/main/java/com/chatstory/bridge/PromptEncoder.java`
- `src/test/java/com/chatstory/PromptEncoderTest.java`

User text is passed into JavaScript via JSON-safe serialization, not raw string replacement.

### Phase 3: Hardcoded Injection Spike

**Status:** Work Complete

- [x] Add a "Test Inject" button to the toolbar.
- [x] Add `inject_prompt.js` as a resource using the function definition pattern.
- [x] Load `inject_prompt.js` through Java resource loading.
- [x] Call `window.chatStoryInjectPrompt(options)` with a JSON options object.
- [x] Avoid string template substitution inside the script source.
- [x] Implement scoped `execCommand('insertText')` attempt.
- [x] Implement paste simulation fallback.
- [x] Implement manual event fallback.
- [x] Return structured `injectResult` bridge message through `cefQuery`.
- [x] Show a clear status-label error if injection fails.
- [x] Verify injection by confirming the ChatGPT send button becomes enabled.
- [x] Record which fallback approach succeeded in the DevCycle completion notes.

**Current Bug:**

Manual validation initially found that prompt injection did not enable the ChatGPT send button. The injection script was revised to use editor-scoped selection, textarea/native-value support, visible/enabled button detection, and short waits after each injection attempt.

Follow-up validation confirmed that Test Inject now enables the ChatGPT Send button.

**Technical Notes:**

Implemented in:

- `src/main/resources/js/inject_prompt.js`
- `src/main/java/com/chatstory/bridge/ChatGptBridge.java`
- `src/main/java/com/chatstory/AppFrame.java`

### Phase 4: Trigger Send and Confirm User Message

**Status:** Work Complete

- [x] Add `trigger_send.js` as a resource.
- [x] Load `trigger_send.js` through Java resource loading.
- [x] Call `window.chatStoryTriggerSend(options)` with `timeoutMs: 5000`.
- [x] In the script, locate the send button.
- [x] Confirm the send button exists.
- [x] Confirm the send button is enabled.
- [x] Click the send button.
- [x] Poll for a new user message node using `userMsg`.
- [x] Compare the new message text against expected text.
- [x] Return structured `sendResult` bridge message through `cefQuery`.
- [x] Call `ResponseListener.onPromptSubmitted(requestId)` after send confirmation.
- [x] Manually validate that hardcoded send confirms a new user message.
- [x] Manually validate that native input send confirms a new user message.

**Technical Notes:**

Implemented in:

- `src/main/resources/js/trigger_send.js`
- `src/main/java/com/chatstory/bridge/ChatGptBridge.java`

This phase is implemented and manually validated for both hardcoded send and native input send.

### Phase 5: Implement ChatBridge

**Status:** Work Complete

- [x] Implement `ChatGptBridge` as the concrete `ChatBridge`.
- [x] Assign a new incrementing `requestId` for each send attempt.
- [x] Register bridge handlers for `injectResult` and `sendResult`.
- [x] Drop stale bridge messages whose `requestId` does not match the active request.
- [x] Log stale request ID mismatches.
- [x] Transition `Ready` or `Complete` to `InjectingPrompt`.
- [x] Transition `InjectingPrompt` to `Sending`.
- [x] Transition `Sending` to `Ready` after prompt submission is confirmed.
- [x] Transition failures to `Error`.
- [x] Route failures through `ResponseListener.onError(requestId, errorCode, message)`.
- [x] Enforce concurrent send prevention inside `ChatGptBridge.sendPrompt()`.
- [x] Add send operation timeout.

**Technical Notes:**

Implemented in:

- `src/main/java/com/chatstory/bridge/ChatGptBridge.java`
- `src/main/java/com/chatstory/bridge/RequestIdGenerator.java`
- `src/main/java/com/chatstory/AppState.java`

DC3 returns to `Ready` after successful prompt submission. DC4 will introduce `WaitingForResponse` behavior when response detection is implemented.

### Phase 6: Native Input Panel

**Status:** Work Complete

- [x] Create `InputPanel` under `com.chatstory.ui`.
- [x] Include a multiline `JTextArea`.
- [x] Include a Send button.
- [x] Place `InputPanel` below the browser in `AppFrame`.
- [x] Keep the browser as the main visible area.
- [x] Register an `AppState.StateListener` in `InputPanel`.
- [x] Enable Send only when `appState.isSendEnabled()` and input text is non-blank.
- [x] Route UI updates through `UiThread.run(...)`.
- [x] On Send, call `ChatBridge.sendPrompt(...)`.
- [x] Clear the input only after send is confirmed.
- [x] Prefer Ctrl+Enter for send; plain Enter inserts a newline.

**Technical Notes:**

Implemented in:

- `src/main/java/com/chatstory/ui/InputPanel.java`
- `src/main/java/com/chatstory/AppFrame.java`

### Phase 7: Tests and Manual Validation

**Status:** Work Complete

- [x] Add `PromptEncoderTest`.
- [x] Add tests for DC3 state transitions in `AppStateTest`.
- [x] Update `ResourceLoadingTest` for new JS resources and `userMsg`.
- [x] Run existing `AppStateTest`, `BridgeMessageTest`, and `ResourceLoadingTest`.
- [x] Run `gradlew.bat clean test`.
- [x] Manually validate hardcoded injection.
- [x] Manually validate native prompt send.
- [x] Manually validate repeated sends.
- [x] Confirm no `BUILDING.md` update is needed because setup/run behavior did not change.

**Verification So Far:**

- `gradlew.bat clean test` passed after DC3 implementation.
- `gradlew.bat test` passed after the latest injection script update.
- `inject_prompt.js` and `trigger_send.js` parsed successfully with Node.

Manual browser validation has confirmed Test Inject, hardcoded send, native prompt send, repeated sends, and failure recovery.

---

## Error Codes

DC3 actively uses:

- `EDITOR_NOT_FOUND`
- `PROMPT_INJECTION_FAILED`
- `SEND_BUTTON_NOT_FOUND`
- `SEND_BUTTON_DISABLED`
- `SEND_CLICK_FAILED`
- `USER_MESSAGE_NOT_CONFIRMED`
- `BRIDGE_HANDLER_FAILED`
- `BRIDGE_MESSAGE_INVALID`
- `TIMEOUT`

---

## Bridge Message Types

DC3 message types:

```text
injectResult
sendResult
error
```

---

## Timeout Constants

Implemented in `ChatGptBridge`:

```java
USER_MESSAGE_CONFIRM_TIMEOUT_MS = 5000
SEND_OPERATION_TIMEOUT_MS = 10000
```

---

## Selector Snapshot

**Inspection / Validation Date:** 2026-05-28

The following selector groups were validated by successful DC3 manual tests:

- Test Inject enabled the ChatGPT Send button.
- Test Inject plus send produced a new user message in ChatGPT.
- Native `InputPanel` Send produced a new user message in ChatGPT.
- Repeated native sends worked.

Current selector groups in `src/main/resources/js/chatgpt_selectors.json`:

```json
{
  "promptEditor": [
    "[contenteditable='true'][data-id]",
    "#prompt-textarea",
    "div[contenteditable='true']"
  ],
  "sendButton": [
    "button[data-testid='send-button']",
    "button[aria-label='Send message']",
    "button[aria-label='Send prompt']",
    "button[type='submit']"
  ],
  "stopButton": [
    "button[aria-label='Stop generating']",
    "[data-testid='stop-button']"
  ],
  "assistantMsg": [
    "[data-message-author-role='assistant']"
  ],
  "userMsg": [
    "[data-message-author-role='user']"
  ]
}
```

Notes:

- `promptEditor`, `sendButton`, and `userMsg` are the DC3-critical selector groups.
- `stopButton` and `assistantMsg` are present for DC4 response detection and extraction work, but were not exercised by DC3.
- These selectors depend on ChatGPT's live web UI and may require updates after future ChatGPT frontend changes.

---

## Manual Validation Plan

### Test 1: No Regression From DC2

Steps:

1. Run `gradlew.bat run`.
2. Confirm ChatGPT loads and session is active.
3. Confirm DevTools opens.

Pass:

- No DC2 regression.

### Test 2: Hardcoded Injection

Steps:

1. Open a ChatGPT conversation.
2. Click the Test Inject button.

Pass:

- The hardcoded test prompt appears in the ChatGPT editor and the send button becomes enabled.

Current status:

- Passed after latest injection script revision. Test Inject enables the ChatGPT Send button.

### Test 3: Hardcoded Send

Steps:

1. Use Test Inject.
2. Trigger send through the send script.

Pass:

- ChatGPT shows a new user message with the expected test prompt.

Current status:

- Passed. Test Inject plus send produced a new user message in ChatGPT.

### Test 4: Native Prompt Send

Steps:

1. Type a short prompt into the native input panel.
2. Click Send.

Pass:

- The prompt appears in ChatGPT as a new user message.
- The native input clears after confirmation.
- The app returns to a send-ready state.

Current status:

- Passed. Native `InputPanel` Send produced a new user message in ChatGPT.

### Test 5: Prompt Encoding Edge Cases

Steps:

1. Send prompts containing quotes, backslashes, newlines, non-ASCII characters, and emoji.

Pass:

- The submitted ChatGPT user message exactly matches the native input text.

Current status:

- Java-side encoding tests pass. Browser-side sends with native input passed for normal prompt text. Additional manual edge-case prompts can be revisited in a later hardening cycle if needed.

### Test 6: Repeated Sends

Steps:

1. Send three short prompts in sequence.

Pass:

- Each prompt is submitted once.
- The UI does not allow overlapping sends.
- Stale bridge messages do not affect the current request.

Current status:

- Passed. Repeated native sends work.

### Test 7: Failure Recovery

Steps:

1. Attempt to send while not on a usable ChatGPT page or before the editor is available.

Pass:

- The app reports a specific error and can recover to `Ready`.

Current status:

- Passed. Sending from a non-ChatGPT/unusable page shows an error without crashing, and the app recovers after navigating back to ChatGPT.

---

## Notes and Risks

- **Resolved bug:** Prompt injection initially did not enable the ChatGPT send button. The revised injection script has now been manually confirmed to enable the Send button through Test Inject.
- **Selector risk:** Current selectors worked during DC3 validation, but they remain dependent on ChatGPT's live web UI and may need future updates.
- **React editor risk:** ChatGPT may visually accept text while internal editor state remains unchanged. The send button becoming enabled is the required signal.
- **Send confirmation risk:** Native send and repeated sends have now confirmed new user messages.
- **State-model note:** Returning to `Ready` after send is temporary DC3 behavior. DC4 should introduce response waiting and extraction.
- **Verification authority:** This DevCycle may not be marked `Verified` without explicit user permission.

---

## Completion Summary

**Completion Date:** 2026-05-28
**Phases Completed:** All
**Work Deferred:** Assistant response completion detection and response extraction remain deferred to DC4, by design.

**Accomplishments:**

- Added `ChatGptBridge` concrete bridge implementation.
- Added `PromptEncoder`.
- Added `RequestIdGenerator`.
- Added native `InputPanel`.
- Added toolbar `Test Inject` button.
- Added `inject_prompt.js`.
- Added `trigger_send.js`.
- Added `userMsg` selector group.
- Added selector fallbacks.
- Updated `AppState` for DC3 prompt-send flow.
- Added `PromptEncoderTest`.
- Updated `ResourceLoadingTest`.
- Updated `AppStateTest`.

**Metrics So Far:**

- Files added: 7
- Files modified: 7
- Clean test build: passing

**Lessons / Notes So Far:**

- Initial injection attempt did not enable the ChatGPT send button.
- Injection logic was revised to use editor-scoped selection, textarea/native-value support, visible/enabled button detection, and short waits after each injection attempt.
- Manual validation confirmed the revised Test Inject path enables the ChatGPT Send button.
- Hardcoded send and native prompt send were manually confirmed.
- Repeated sends and failure recovery were manually confirmed.
- DC3 was marked `Verified` after explicit user approval.
