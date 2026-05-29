# DevCycle 009: Correction Actions and Redo

**Status:** Verified
**Start Date:** TBD
**Target Completion:** TBD
**Focus:** Let users right-click highlighted text in the response panel to send a targeted correction prompt, and add a Redo button that resends the last story beat.

---

## Goal

DC9 adds two ways for users to steer ChatGPT without typing freeform prompts. Right-clicking selected text in the Response tab reveals a correction menu with three fixed-prompt actions. A Redo button in the toolbar sends a single fixed prompt to regenerate the last story beat. Both actions go through the established send pipeline and update the response panel normally.

## Desired Outcome

At the end of DevCycle 009:

- Right-clicking selected text in the Response tab shows a context menu with three items: `Context Leakage`, `Bad Writing`, and `Re-evaluate`.
- Each menu item sends a fixed prefix + the selected text as a prompt to ChatGPT.
- The menu items are disabled when no text is selected.
- A `Redo` button appears in the toolbar and sends `"Please redo the last story beat"` to ChatGPT.
- The `Redo` button is disabled when `AppState.isSendEnabled()` is false and enabled reactively as state changes.
- All three correction actions and Redo go through the same send pipeline as normal prompts: response panel clears on submission, updates as the response streams in.
- Correction prompts are defined as constants and are unit-tested.
- All DC8 behavior is preserved with no regressions.

---

## Explicit Non-Goals

DevCycle 009 does not include:

- custom or user-editable correction prompt templates
- correction action history
- mode-gating correction actions or Redo to Story Mode only
- keyboard shortcuts for correction actions
- a confirmation dialog before sending

---

## Correction Prompt Templates

The three fixed prompts:

| Action | Prompt sent to ChatGPT |
|--------|------------------------|
| Context Leakage | `"Do not rewrite the beat, but there is a context leakage problem with: "` + selected text |
| Bad Writing | `"Do not rewrite the beat, but this is bad writing: "` + selected text |
| Re-evaluate | `"Do not rewrite the beat, but re-evaluate this text you generated: "` + selected text |

Redo prompt: `"Please redo the last story beat"`

These strings are constants. They are not configurable in DC9.

---

## Architecture

`OutputPanel` currently has no send capability — it only displays text. DC9 adds a `Consumer<String> onSendPrompt` parameter to `OutputPanel` (and by extension `LeftPanePanel`). `AppFrame` provides the lambda that calls `chatBridge.sendPrompt(prompt, statusResponseListener(...))`.

This keeps `OutputPanel` decoupled from `ChatBridge` and `ResponseListener` — it only knows "call this when you want to send a string."

The `Redo` button lives in `AppFrame`'s toolbar and calls `chatBridge.sendPrompt(...)` directly, the same as `testInjectBtn` already does.

---

## Target Additions

```text
src/main/java/com/chatstory/bridge/
  CorrectionType.java          enum of correction actions with prompt-building logic

src/test/java/com/chatstory/
  CorrectionTypeTest.java

Modified:
  ui/OutputPanel.java          add right-click context menu; accept onSendPrompt callback
  ui/LeftPanePanel.java        thread onSendPrompt through to OutputPanel
  AppFrame.java                provide onSendPrompt lambda; add Redo button to toolbar
```

---

## Tasks

### Phase 1: Correction Prompt Model

**Status:** Planning

- [ ] Create `CorrectionType` enum in `com.chatstory.bridge` with values `CONTEXT_LEAKAGE`, `BAD_WRITING`, `RE_EVALUATE`.
- [ ] Each value holds a `prefix` string (the fixed prompt prefix from the table above).
- [ ] Implement `buildPrompt(String selectedText)` — returns `prefix + selectedText`.
- [ ] Implement `menuLabel()` — returns the human-readable label for each action (`"Context Leakage"`, `"Bad Writing"`, `"Re-evaluate"`).
- [ ] Write `CorrectionTypeTest` covering: prompt is prefix + text for each type; empty selected text produces prefix only; null selected text throws or is handled gracefully.

**Technical Notes:**

`CorrectionType` is pure Java with no Swing dependency. Keeping it in `com.chatstory.bridge` is consistent with `ErrorCodes` and `PromptEncoder`, which are also protocol/content helpers in that package.

The Redo prompt (`"Please redo the last story beat"`) is a constant on `CorrectionType` or a top-level constant in the same class — it does not need a `selectedText` argument.

---

### Phase 2: Right-Click Correction Menu

**Status:** Planning

- [ ] Add `Consumer<String> onSendPrompt` as a new parameter to `OutputPanel`'s constructor, after the existing `CanonStore` and `onCanonAdded` parameters.
- [ ] Register a `MouseListener` on `OutputPanel`'s `JTextArea` that opens a `JPopupMenu` on right-click (check `MouseEvent.isPopupTrigger()` on both `mousePressed` and `mouseReleased` for cross-platform correctness).
- [ ] Add three `JMenuItem` entries to the menu, one per `CorrectionType`, using `correctionType.menuLabel()` as the label.
- [ ] Each item's action listener: get `textArea.getSelectedText()`; if null or blank, do nothing; otherwise call `onSendPrompt.accept(correctionType.buildPrompt(selectedText))`.
- [ ] Disable all three items when `textArea.getSelectedText()` is null or blank; enable when text is selected. Update item states in a `PopupMenuListener.popupMenuWillBecomeVisible` callback so the check runs at display time.
- [ ] Update `LeftPanePanel` to accept and thread `Consumer<String> onSendPrompt` through to `OutputPanel`.
- [ ] Update `AppFrame` to provide the `onSendPrompt` lambda: `prompt -> chatBridge.sendPrompt(prompt, statusResponseListener("Correction sent"))`.

**Technical Notes:**

`isPopupTrigger()` must be checked in both `mousePressed` and `mouseReleased` — on Windows the trigger fires on release, but checking both is the portable pattern.

Use `PopupMenuListener.popupMenuWillBecomeVisible` to update item enabled states at show time rather than tracking selection changes continuously. This is simpler and reliable for a `JTextArea`.

`onSendPrompt` should be called on the EDT (it originates from a mouse event, so it already is). `chatBridge.sendPrompt` is thread-safe per the established pattern.

---

### Phase 3: Redo Button

**Status:** Planning

- [ ] Add a `Redo` button to `AppFrame`'s toolbar `leftTools` panel, after `testInjectBtn`.
- [ ] Button action: call `chatBridge.sendPrompt(CorrectionType.REDO_PROMPT, statusResponseListener("Redo sent"))`.
- [ ] Disable the button at startup; register an `AppState.StateListener` to enable it when `appState.isSendEnabled()` is true and disable it otherwise.
- [ ] Wire through `UiThread.run(...)` for the state listener update, consistent with all other state-driven UI updates.

**Technical Notes:**

`Redo` sits in the toolbar rather than the response panel because it is not tied to selected text — it is a global action that makes sense regardless of what is currently shown in the response panel.

The enabled/disabled state mirrors the Send button in `InputPanel`: both follow `appState.isSendEnabled()`.

---

### Phase 4: Tests and Manual Validation

**Status:** Planning

- [ ] Run `gradlew.bat clean test` — confirm all existing tests plus `CorrectionTypeTest` pass.
- [ ] Manually validate: right-clicking with no selection shows menu with all items disabled.
- [ ] Manually validate: right-clicking with selected text shows menu with items enabled.
- [ ] Manually validate: each correction action sends the correct prompt (confirm in ChatGPT what message appeared).
- [ ] Manually validate: response panel clears on correction send and updates as response arrives.
- [ ] Manually validate: Redo button is disabled at startup and becomes enabled after ChatGPT loads.
- [ ] Manually validate: Redo sends the correct prompt.
- [ ] Manually validate: Redo is disabled while a send is in progress.
- [ ] Manually validate: DC8 context tab, canon, and existing send behavior not regressed.

---

## Manual Validation Plan

### Test 1: No Regression

Steps:
1. Run `gradlew.bat run`.
2. Confirm ChatGPT loads, session active, DevTools opens.
3. Confirm normal prompt send, response display, Add to Canon all work.

Pass: No DC8 regression.

### Test 2: Right-Click With No Selection

Steps:
1. Wait for a response to appear in the Response tab.
2. Click somewhere in the response area without selecting text.
3. Right-click.

Pass: Context menu appears with all three items visible but disabled.

### Test 3: Right-Click Correction Send

Steps:
1. Wait for a response to appear.
2. Select a phrase in the response text.
3. Right-click → `Bad Writing`.

Pass: ChatGPT receives `"Do not rewrite the beat, but this is bad writing: [selected phrase]"` as a new user message. Response panel clears and updates.

### Test 4: All Three Correction Actions

Steps:
1. Repeat Test 3 for `Context Leakage` and `Re-evaluate`.

Pass: Each sends the correct prefix + selected text.

### Test 5: Redo Button State

Steps:
1. Launch app. Inspect Redo button.
2. Wait for ChatGPT to finish loading.

Pass: Redo is disabled at startup; becomes enabled once the app is Ready.

### Test 6: Redo Send

Steps:
1. Click Redo when enabled.

Pass: ChatGPT receives `"Please redo the last story beat"` as a new user message.

### Test 7: Redo Disabled During Send

Steps:
1. Send a normal prompt.
2. While waiting for response, inspect Redo button.

Pass: Redo is disabled while the app is not in a send-enabled state.

---

## Notes and Risks

- `isPopupTrigger()` behavior differs between platforms. Always check in both `mousePressed` and `mouseReleased`. On Windows this is fine as-is but the portable pattern costs nothing.
- The `JTextArea`'s selected text can change between the right-click and the menu-item click on some platforms. Reading `getSelectedText()` inside the item's action listener (not in `popupMenuWillBecomeVisible`) is the safer read point, since the selection should not change while the menu is open.
- Correction actions and Redo use the same `statusResponseListener` pipeline as normal sends. If the app is in `Sending` or another non-ready state and the button is clicked anyway (race condition), `ChatGptBridge.sendPrompt` will reject the call and `onError` will be invoked, showing an error in the status label. No special handling needed.
- `LeftPanePanel` grows a `Consumer<String>` constructor parameter. This is the only structural change to the left-pane wiring.

---

## Open Questions

1. **Should the right-click menu also appear when the response panel is empty?**
   Recommendation: Yes — the menu always registers. Items are disabled when nothing is selected, which handles the empty case automatically.

2. **Should correction actions clear the response panel immediately on send, matching normal Story Mode behavior?**
   Recommendation: Yes — `statusResponseListener` already calls `leftPane.clearResponse()` on `onPromptSubmitted`, so this happens automatically with no extra work.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** [YYYY-MM-DD]
**Phases Completed:** [List or "All"]
**Work Deferred:** [What was not done and why, or "None"]

**Accomplishments:**
- [What was built or changed]

**Metrics:**
- Files modified: [N]
- Tests passing: [N]

**Lessons / Notes:**
[Anything worth remembering for future cycles.]
