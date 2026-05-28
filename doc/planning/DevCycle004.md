# DevCycle 004: Completion Detection and Response Extraction

**Status:** Planning
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

**Status:** Planning

- [ ] Create `OutputPanel` under `com.chatstory.ui`.
- [ ] Include a read-only text area for the latest assistant response.
- [ ] Include a Copy button if trivial.
- [ ] Auto-scroll output to the top or bottom consistently after updates.
- [ ] Place `OutputPanel` to the left of the browser, not below it.
- [ ] Add an empty placeholder panel to the right of the browser for UI testing.
- [ ] Use nested horizontal `JSplitPane`s or an equivalent resizable layout:
  - left: native output panel
  - center: embedded browser
  - right: empty UI testing panel
- [ ] Keep `InputPanel` below the browser/output split.
- [ ] Preserve DevTools and Test Inject access.

**Technical Notes:**

The browser remains the source of truth during debugging, so the split panes should be resizable. The user should be able to give the browser most of the space when inspecting DOM behavior. The right-side placeholder panel is intentionally empty in DC4 and exists only for UI layout testing.

### Phase 2: Response State Flow

**Status:** Planning

- [ ] Change successful native send behavior from returning directly to `Ready`.
- [ ] After prompt submission, transition `Sending -> WaitingForResponse`.
- [ ] After response extraction succeeds, transition `WaitingForResponse -> Complete`.
- [ ] Allow `Complete -> Ready` on the next send.
- [ ] On timeout or extraction failure, transition `WaitingForResponse -> Error`.
- [ ] Keep Send disabled while waiting for response.
- [ ] Ensure status label shows clear states such as:
  - `Waiting for response...`
  - `Response complete`
  - `Response timeout`

**Technical Notes:**

DC3 temporarily returned to `Ready` after send because response tracking did not exist yet. DC4 should replace that temporary behavior with real response waiting.

### Phase 3: Selector Inspection and Update

**Status:** Planning

- [ ] Use DevTools to inspect assistant message DOM after a generated response.
- [ ] Confirm the best `assistantMsg` selector.
- [ ] Identify an inner content node if the outer assistant message includes UI controls.
- [ ] Confirm stop/generation indicator selector candidates.
- [ ] Update `chatgpt_selectors.json` if needed.
- [ ] Record selector snapshot and inspection date in this document.

**Technical Notes:**

The current `assistantMsg` and `stopButton` selectors are candidates from earlier planning. DC4 must confirm them against the live ChatGPT DOM before relying on them.

### Phase 4: Completion Detection

**Status:** Planning

- [ ] Record assistant message count or latest assistant message identity before send.
- [ ] After send, poll for a new assistant message.
- [ ] Poll for generation status using stop-button or generation-indicator selectors.
- [ ] Treat response as complete only when:
  - a new assistant message exists
  - generation controls are absent
  - assistant message text has remained stable for the stability window
- [ ] Use initial constants:
  - poll interval: `500 ms`
  - stability window: `1500 ms`
  - maximum timeout: `180 s`
- [ ] On timeout, preserve any available text and show a partial/timeout status.

**Technical Notes:**

Use polling for DC4. MutationObserver can be considered later if polling is unreliable or too slow.

### Phase 5: Response Extraction

**Status:** Planning

- [ ] Add `extract_response.js` as a resource.
- [ ] Define a clear JS contract for extraction results.
- [ ] Extract readable plain text from the new assistant message.
- [ ] Preserve line breaks well enough for multi-paragraph responses.
- [ ] Avoid including copy buttons, labels, citations UI, or unrelated page text.
- [ ] Return structured bridge messages through `cefQuery`, including `requestId`.
- [ ] Discard stale response messages whose `requestId` does not match the active request.
- [ ] Display extracted text in `OutputPanel`.

**Technical Notes:**

Full Markdown rendering is not required. The output should be readable plain text.

### Phase 6: Bridge Integration

**Status:** Planning

- [ ] Extend `ChatGptBridge` or create a response-specific bridge component.
- [ ] Register handlers for response extraction messages.
- [ ] Preserve strict `requestId` matching.
- [ ] Update `ResponseListener.onResponseComplete(...)` usage.
- [ ] Use `ResponseListener.onResponsePartial(...)` only if needed; streaming display is not required.
- [ ] Ensure errors route through `ResponseListener.onError(...)`.
- [ ] Keep failure messages specific:
  - `timeout`
  - `extraction_failed`
  - `bridge_message_invalid`
  - `bridge_handler_failed`

**Technical Notes:**

Avoid making `ChatGptBridge` too large if response handling becomes complex. A small helper for response polling/extraction is acceptable if it keeps responsibilities clear.

### Phase 7: Tests and Manual Validation

**Status:** Planning

- [ ] Add resource-loading test coverage for `extract_response.js`.
- [ ] Add pure Java tests for any response polling state helpers, if introduced.
- [ ] Run existing tests.
- [ ] Run `gradlew.bat clean test`.
- [ ] Manually validate one prompt/response cycle.
- [ ] Manually validate three repeated prompt/response cycles.
- [ ] Manually validate timeout/failure behavior if practical.
- [ ] Update `BUILDING.md` only if setup/run behavior changes.

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

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** [YYYY-MM-DD]
**Phases Completed:** [Pending]
**Work Deferred:** [Pending]

**Accomplishments:**
- [Pending]

**Metrics:**
- Files modified: [Pending]
- Tests passing: [Pending]

**Lessons / Notes:**
[Pending]
