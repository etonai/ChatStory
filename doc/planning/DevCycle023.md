# DevCycle 023: Auto-Reset Before Send, Redo, and Continue

**Status:** Work Complete
**Start Date:** 2026-08-01
**Target Completion:** TBD
**Focus:** Automatically run the existing reset logic immediately before any Send, Redo, or Continue action fires, to prevent stuck app state from blocking or freezing subsequent prompts.

---

## Goal

The user has repeatedly run into problems caused by the app not being in a reset/Ready state when a new prompt is sent. `ChatGptBridge.reset()` (clears the active request lock and calls `appState.reset()`) already exists and is already invoked defensively in the `onFetch` path in `AppFrame.java` before fetching a response. There is no real downside to always resetting immediately before every Send/Redo/Continue action fires, since a reset on an already-clean state is a no-op in effect. This cycle extends that same defensive pattern to every button that submits a prompt.

## Desired Outcome

At the end of DevCycle 023:

- Every button that sends a prompt to ChatGPT calls `chatBridge.reset()` immediately before submitting, matching the existing `onFetch` pattern.
- This applies to:
  - `InputPanel`'s main **Send** button, plus its Enter and Ctrl+Shift+Enter (send-as-direction) keyboard shortcuts.
  - **Redo** button.
  - **Continue** button.
  - `MainPanel`'s **Send Controller**, **Send Intermediate**, **Send Final**, **Send Rules**, and **Send Context** buttons.
- The explicit standalone **Reset** buttons remain unchanged (they already just call `chatBridge.reset()` directly).
- No behavior change for users beyond the app reliably being in a clean/Ready state right before each send-type action fires.

---

## Tasks

### Phase 1: Reset before InputPanel Send actions

**Status:** Work Complete

- [x] In `InputPanel.send(ResponseListener, boolean)`, call `chatBridge.reset()` before the existing `sender.sendPrompt(...)` / `sender.sendRawPrompt(...)` call so the Send button, Enter shortcut, and Ctrl+Shift+Enter (send-as-direction) shortcut are all covered by the single `send(...)` method.

**Technical Notes:**
Relevant file: `src/main/java/com/chatstory/ui/InputPanel.java`. `send()`, `sendAsDirection()`, and `triggerSend()` all funnel through the private `send(ResponseListener, boolean)` method, so one change covers all three entry points.

### Phase 2: Reset before Redo and Continue

**Status:** Work Complete

- [x] In `AppFrame.java`, update the `onRedo` and `onContinue` `Runnable` definitions to call `chatBridge.reset()` immediately before `chatBridge.sendPrompt(...)`.

**Technical Notes:**
Relevant file: `src/main/java/com/chatstory/AppFrame.java` (`onRedo` around line 95, `onContinue` around line 99). Follow the same shape already used by `onFetch` (lines 109-119), which calls `chatBridge.reset()` before its subsequent bridge call.

### Phase 3: Reset before MainPanel Send* buttons

**Status:** Work Complete

- [x] Add a reset call before the prompt-sending logic behind **Send Controller**, **Send Intermediate**, **Send Final**, **Send Rules**, and **Send Context** in `MainPanel.java`.

**Technical Notes:**
`sendController()`, `sendIntermediate()`, and `sendFinal()` all funnel through the `onSendPrompt` consumer, so the reset was added once at the call site in `AppFrame.java` where that consumer is constructed (`text -> { chatBridge.reset(); chatBridge.sendPrompt(text, ...); }`), covering all three. `sendRules()` and `sendContext()` don't go through `onSendPrompt` — they call `onClickUpload.run()` directly — so each now calls the existing `onReset.run()` field (already wired to `chatBridge::reset`) as its first action.

### Phase 4: Verification

**Status:** Work Complete

- [x] Compile the project.
- [x] Run existing relevant tests.
- [ ] Manually verify Send, Redo, Continue, Send Controller, Send Intermediate, Send Final, Send Rules, and Send Context each still function normally and now reset first.
- [ ] Manually verify resetting no longer causes any regression (e.g. no double-reset issues, no lost in-flight state that the user actually needed).

**Technical Notes:**
`./gradlew.bat compileJava` succeeds. `./gradlew.bat test` reports 149 tests, 148 pass; the one failure (`CorrectionTypeTest.endScenePromptIsFormattedDirection`) was confirmed pre-existing on `main` before this cycle's changes (reproduced via `git stash`) and is unrelated to this cycle. Manual verification of the running app is still pending user confirmation.

**Technical Notes:**
Useful verification commands may include:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
```

---

## Open Questions

1. **Do any of the MainPanel Send* buttons need to preserve in-flight state that a reset would clear?**
   Recommendation: Review `ChatGptBridge.reset()` (`clearActiveLocked()` + `appState.reset()`) during implementation to confirm it's safe to call unconditionally before every send-type action, matching the existing `onFetch` precedent.

2. **Should the reset call be centralized in one place (e.g. inside `ChatGptBridge.sendPrompt`/`sendRawPrompt` themselves) rather than at each call site?**
   Recommendation: Consider this during implementation. Centralizing inside the bridge's send methods would be the smallest-surface-area change and guarantee no call site is missed, but changes the bridge's implicit contract (every send always resets first) rather than being explicit at each UI call site. Decide based on which approach the existing codebase style favors.

---

## Notes and Risks

- This cycle assumes resetting immediately before a send is always safe/harmless, per the user's own observation and the existing `onFetch` precedent. If any send-type action turns out to depend on state that reset clears, that should surface during manual verification in Phase 4.
- Keep the change minimal — this is a defensive-reset cycle, not a broader refactor of prompt-sending or button wiring.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** TBD (pending manual verification)
**Phases Completed:** 1-3 and the automated portion of Phase 4
**Work Deferred:** Manual in-app verification of each button (Phase 4) is pending user confirmation; cycle is not marked Verified.

**Accomplishments:**
- `InputPanel.send(...)` now calls `chatBridge.reset()` before sending, covering the Send button, Enter, and Ctrl+Shift+Enter shortcuts.
- `AppFrame`'s `onRedo` and `onContinue` now call `chatBridge.reset()` before sending their respective prompts.
- The `onSendPrompt` consumer passed into `MainPanel` now resets before sending, covering Send Controller, Send Intermediate, and Send Final.
- `MainPanel.sendRules()` and `MainPanel.sendContext()` now call `onReset.run()` before staging/uploading.

**Metrics:**
- Files modified: 3 (`InputPanel.java`, `AppFrame.java`, `MainPanel.java`)
- `./gradlew.bat compileJava` passes.
- `./gradlew.bat test`: 148/149 pass; the 1 failure is pre-existing on `main` and unrelated to this cycle.

**Lessons / Notes:**
Reset was added at the fewest call sites that still guarantee coverage: one shared `send()` method in `InputPanel`, the `onRedo`/`onContinue` Runnables in `AppFrame`, and the shared `onSendPrompt` consumer for the three controller-file sends, plus two direct calls in `MainPanel` for the upload-staging paths that don't go through `onSendPrompt`.
</content>
