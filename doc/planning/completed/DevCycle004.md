# DevCycle 004: Completion Detection and Response Extraction

**Status:** Verified
**Start Date:** 2026-05-28
**Target Completion:** 2026-07-09
**Focus:** Detect completed ChatGPT assistant responses and display the latest response in a native output panel alongside the browser.

---

## Goal

Build the first native response-reading workflow. After the native input sends a prompt successfully, the app should detect the new assistant response, wait until generation appears complete, extract readable plain text, and display it in a native `OutputPanel`.

DC4 builds directly on DC3's confirmed native prompt-send bridge. It should not add story parsing, correction tools, metadata protocols, or continuity features.

## Desired Outcome

At the end of DevCycle 004:

- The app has a native `OutputPanel` to the left of the embedded browser.
- The app has an empty placeholder panel to the right of the embedded browser for UI testing.
- Native Send transitions the app into response-waiting behavior after prompt submission.
- The bridge identifies the new assistant message created after the send.
- The app detects when the assistant response appears complete.
- The latest assistant response is extracted as readable plain text.
- The extracted response appears in the native output panel.
- Repeated prompt/response cycles update the output panel with the correct latest response.
- Timeout or partial-response cases produce a clear error/status message.

---

## Explicit Non-Goals

DevCycle 004 does not include:

- story-only parsing
- XML or tagged response protocols
- continuity tracking
- relationship state tracking
- correction workflows
- upload profiles
- prompt history
- resend behavior
- full Markdown rendering
- long-term archive/session storage
- MutationObserver-based generation detection unless polling proves insufficient

DC4 extracts plain text only.

---

## Tasks

### Phase 1: OutputPanel UI

**Status:** Work Complete

- [x] Create `OutputPanel` under `com.chatstory.ui`.
- [x] Include a read-only text area for the latest assistant response.
- [x] Include a Copy button if trivial.
- [x] Auto-scroll output to the top or bottom consistently after updates.
- [x] Place `OutputPanel` to the left of the browser, not below it.
- [x] Add an empty placeholder panel to the right of the browser for UI testing.
- [x] Use nested horizontal `JSplitPane`s or an equivalent resizable layout:
  - left: native output panel
  - center: embedded browser
  - right: empty UI testing panel
- [x] Keep `InputPanel` below the browser/output split.
- [x] Preserve DevTools and Test Inject access.

**Technical Notes:**

The browser remains the source of truth during debugging, so the split panes should be resizable. The user should be able to give the browser most of the space when inspecting DOM behavior. The right-side placeholder panel is intentionally empty in DC4 and exists only for UI layout testing.

### Phase 2: Response State Flow

**Status:** Work Complete

- [x] Change successful native send behavior from returning directly to `Ready`.
- [x] After prompt submission, transition `Sending -> WaitingForResponse`.
- [x] After response extraction succeeds, transition `WaitingForResponse -> Complete`.
- [x] Allow `Complete -> Ready` on the next send.
- [x] On timeout or extraction failure, transition `WaitingForResponse -> Error`.
- [x] Keep Send disabled while waiting for response.
- [x] Ensure status label shows clear states such as:
  - `Waiting for response...`
  - `Response complete`
  - `Response timeout`

**Technical Notes:**

DC3 temporarily returned to `Ready` after send because response tracking did not exist yet. DC4 should replace that temporary behavior with real response waiting.

### Phase 3: Selector Inspection and Update

**Status:** Work Complete

- [x] Use live manual response tests to validate assistant message detection after generated responses.
- [x] Confirm the current `assistantMsg` selector works for DC4 extraction.
- [x] Identify an inner content node fallback for readable assistant text extraction.
- [x] Confirm stop/generation indicator candidates needed a fallback completion signal.
- [x] Update `chatgpt_selectors.json` if needed.
- [x] Record selector snapshot and inspection date in this document.

**Implementation Note:**

Selector values from `chatgpt_selectors.json` are wired into the DC4 extraction flow and were validated through live manual prompt/response tests. No selector JSON update was needed for DC4, but completion detection was hardened because the stop-button signal alone was insufficient for longer responses.

**Selector Snapshot - 2026-05-28:**

- `assistantMsg`: `[data-message-author-role='assistant']`
- inner response content preference: `.markdown`, then `[data-message-content]`, then the assistant message node itself
- `stopButton`: `button[aria-label='Stop generating']`, `[data-testid='stop-button']`
- completion fallback: enabled `sendButton` plus stable assistant text

**Technical Notes:**

The current `assistantMsg` and `stopButton` selectors are candidates from earlier planning. DC4 must confirm them against the live ChatGPT DOM before relying on them.

### Phase 4: Completion Detection

**Status:** Work Complete

- [x] Record assistant message count or latest assistant message identity before send.
- [x] After send, poll for a new assistant message.
- [x] Poll for generation status using stop-button or generation-indicator selectors.
- [x] Treat response as complete only when:
  - a new assistant message exists
  - generation controls are absent
  - assistant message text has remained stable for the stability window
- [x] Use initial constants:
  - poll interval: `500 ms`
  - stability window: `1500 ms`
  - maximum timeout: `180 s`
- [x] On timeout, preserve any available text and show a partial/timeout status.

**Technical Notes:**

Use polling for DC4. MutationObserver can be considered later if polling is unreliable or too slow.

### Phase 5: Response Extraction

**Status:** Work Complete

- [x] Add `extract_response.js` as a resource.
- [x] Define a clear JS contract for extraction results.
- [x] Extract readable plain text from the new assistant message.
- [x] Preserve line breaks well enough for multi-paragraph responses.
- [x] Avoid including copy buttons, labels, citations UI, or unrelated page text.
- [x] Return structured bridge messages through `cefQuery`, including `requestId`.
- [x] Discard stale response messages whose `requestId` does not match the active request.
- [x] Display extracted text in `OutputPanel`.

**Technical Notes:**

Full Markdown rendering is not required. The output should be readable plain text.

### Phase 6: Bridge Integration

**Status:** Work Complete

- [x] Extend `ChatGptBridge` or create a response-specific bridge component.
- [x] Register handlers for response extraction messages.
- [x] Preserve strict `requestId` matching.
- [x] Update `ResponseListener.onResponseComplete(...)` usage.
- [x] Use `ResponseListener.onResponsePartial(...)` only if needed; streaming display is not required.
- [x] Ensure errors route through `ResponseListener.onError(...)`.
- [x] Keep failure messages specific:
  - `timeout`
  - `extraction_failed`
  - `bridge_message_invalid`
  - `bridge_handler_failed`

**Technical Notes:**

Avoid making `ChatGptBridge` too large if response handling becomes complex. A small helper for response polling/extraction is acceptable if it keeps responsibilities clear.

### Phase 7: Tests and Manual Validation

**Status:** Work Complete

- [x] Add resource-loading test coverage for `extract_response.js`.
- [x] Add pure Java tests for any response polling state helpers, if introduced.
- [x] Run existing tests.
- [x] Run `gradlew.bat clean test`.
- [x] Manually validate one prompt/response cycle.
- [x] Manually validate three repeated prompt/response cycles.
- [x] Reproduce and fix browser-to-native-input focus recovery bug.
- [x] Manually validate timeout/failure behavior if practical.
- [x] Update `BUILDING.md` only if setup/run behavior changes.

**Automated Test Notes:**

- `node --check src/main/resources/js/extract_response.js` passed.
- `node --check src/main/resources/js/trigger_send.js` passed.
- `node --check src/main/resources/js/inject_prompt.js` passed.
- `gradlew.bat clean test` passed on 2026-05-28.
- No `BUILDING.md` update was needed.

**Manual Validation Notes:**

- Initial DC4 testing showed the layout and basic response extraction looked good.
- The longer four-paragraph response test initially exposed a completion-detection bug; after the completion fallback fix, the user confirmed the behavior looks good.
- The browser-to-native-input focus recovery bug was reproduced, fixed, and confirmed by the user.
- The three repeated prompt/response cycle test passed.
- Timeout/failure behavior was not forced further in DC4; the implemented timeout and partial-response path is considered sufficient for this cycle because the core manual response flow passed.

**Technical Notes:**

Do not attempt to automate live ChatGPT behavior in unit tests. Browser response behavior remains manually validated for DC4.

---

## Manual Validation Plan

### Test 1: No Regression From DC3

Steps:

1. Run `gradlew.bat run`.
2. Confirm ChatGPT loads and session is active.
3. Confirm native input can still send a prompt.

Pass:

- Prompt appears in ChatGPT as a new user message.

### Test 2: Output Panel Layout

Steps:

1. Launch the app.
2. Confirm the output panel is on the left, the browser is in the center, and the empty UI testing panel is on the right.
3. Resize the split panes.

Pass:

- Output panel, browser, and right-side placeholder panel are all visible. The output panel is on the left, the browser is in the center, and the empty UI testing panel is on the right. The browser can still be expanded for debugging.

### Test 3: Single Response Extraction

Steps:

1. Send a short prompt through native input.
2. Wait for ChatGPT to finish generating.

Pass:

- The latest assistant response appears in the native output panel as readable plain text.

### Test 4: Repeated Prompt/Response Cycles

Steps:

1. Send three short prompts through native input.
2. Wait for each response to complete.

Pass:

- The output panel updates to the correct latest assistant response each time.
- No stale response is displayed.

### Test 5: Timeout or Partial Response Handling

Steps:

1. Trigger a practical timeout/failure case if possible.

Pass:

- The app does not hang.
- A clear timeout or partial-response status appears.
- Any available text is preserved if extraction partially succeeded.

### Test 6: Browser Focus Recovery

Steps:

1. Launch the app.
2. Click inside the embedded browser content area.
3. Optionally type a few letters into the browser/ChatGPT area.
4. Click back into the native input panel.

Pass:

- The native input panel accepts focus and typing immediately.
- The user does not need to switch to another application, such as IntelliJ, and then back to Story Workstation to recover native input focus.

---

## Success Criteria

DevCycle 004 is `Work Complete` when:

- `gradlew.bat run` launches with no DC3 regressions.
- The native output panel appears to the left of the browser.
- An empty UI testing panel appears to the right of the browser.
- Native input still sends prompts successfully.
- The app enters `WaitingForResponse` after send confirmation.
- The app detects a completed assistant response.
- The latest assistant response appears in `OutputPanel`.
- Repeated prompt/response cycles display the correct latest response.
- Timeout or extraction failures produce specific status/error feedback.
- Existing and new unit tests pass.
- Response extraction remains plain text only.

Only the user may approve `Verified`.

---

## Notes and Risks

- ChatGPT assistant-message selectors may change.
- Assistant message containers may include UI chrome that should not appear in extracted text.
- Stop-button based completion detection may be brittle.
- Text may pause during streaming; stability-window tuning may be needed.
- The output panel and right-side placeholder should not obscure the browser during debugging; make the split panes resizable.
- DC4 should not expand into story parsing or continuity features.

## Active Bugs

### Browser Focus Can Trap Native Input Focus

**Status:** Fixed - User Confirmed
**Reported:** 2026-05-28

When the user clicks inside the embedded browser section and possibly types a few letters there, the native input panel may stop accepting focus when clicked afterward. The current workaround is to switch focus to another application, such as IntelliJ, and then return to Story Workstation; after the app regains window focus, the native input can be focused again.

This appears to be a Swing/JCEF focus handoff issue and should be fixed before DC4 is considered complete.

**Fix Implemented:**

The native `InputPanel` now explicitly requests focus when clicked. Before requesting Swing focus, it asks the embedded browser to release focus with `browser.setFocus(false)`, then requests focus for the native text area on the Swing event queue. Automated tests pass, and the user confirmed the bug looks fixed.

**Reproduction:**

1. Launch Story Workstation.
2. Click inside the embedded browser area.
3. Optionally type a few letters.
4. Click the native input field.

**Expected:**

The native input field gains focus and accepts typing.

**Original Actual:**

The native input field does not regain focus until the user switches to another application and back.

**Validated Result:**

The user confirmed the bug looks fixed after the focus recovery change.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-05-28
**Phases Completed:** All planned DC4 phases are work complete.
**Work Deferred:** No DC4-blocking work remains. Deeper automated browser validation and richer timeout simulation may be considered in a future cycle.
**Verified Date:** 2026-05-28
**Verification:** User explicitly approved marking DC4 verified after successful manual validation of response extraction, repeated prompt/response cycles, long-response completion detection, and browser-to-native-input focus recovery.

**Accomplishments:**
- Added native `OutputPanel` to the left of the embedded browser.
- Added empty right-side `UI Test Panel` placeholder.
- Changed send flow so confirmed sends enter `WaitingForResponse`.
- Added polling-based assistant completion detection in `extract_response.js`.
- Added response extraction and stale `requestId` protection in `ChatGptBridge`.
- Displayed completed or partial assistant text in the native output panel.
- Preserved DC3 DevTools, Test Inject, and native input behavior.
- Fixed browser-to-native-input focus recovery.
- Validated three repeated prompt/response cycles.

**Metrics:**
- Files modified: 9
- Tests passing: `gradlew.bat clean test`

**Lessons / Notes:**
The original DC3 send timeout needed to be canceled after the user message was confirmed; otherwise long assistant responses could be failed incorrectly while DC4 waited for response completion. DC4 now relies on the extraction timeout during `WaitingForResponse`. The next-send path from `Complete` also needed an explicit `Complete -> Ready -> InjectingPrompt` sequence before starting another request.

Initial manual testing found that a longer four-paragraph response could finish in ChatGPT while the app remained in `WaitingForResponse`. The extractor was relying too heavily on absence of a visible stop button. Completion detection now also treats an enabled send button as a completion signal and includes a conservative long-stability fallback for cases where generation controls are stale or misleading.
