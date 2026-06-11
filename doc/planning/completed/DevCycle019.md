# DevCycle 019: End Session Placement and Redo Counter

**Status:** Work Complete
**Start Date:** 2026-06-02
**Target Completion:** TBD
**Focus:** Move the End Session button to its own panel and add a persistent redo count to the Redo button.

---

## Goal

DC19 makes two targeted improvements to the Commands area of `MainPanel`. First, the End Session button is moved out of the Commands section into a dedicated panel directly below it, so its destructive/finalization nature is visually separated from the per-beat commands. Second, the Redo button gains a running counter that shows how many times it has been pressed since the current scene began (beat 1). That count persists across application restarts and resets only when the Send Controller button is pressed.

## Desired Outcome

At the end of DevCycle 019:

- The Commands section contains: Redo, Continue, End Scene, Reset, Fetch — no End Session button.
- A new titled section (e.g., "Session") appears directly below Commands and contains only the End Session button.
- The Redo button label reads `"Redo N"` where N is the cumulative press count (e.g., `"Redo 0"`, `"Redo 14"`).
- The redo count survives application restarts.
- Pressing the Send Controller button resets the count to 0 and updates the button label to `"Redo 0"`.
- All existing Redo and End Session behaviours are otherwise unchanged.

---

## Tasks

### Phase 1: Move End Session to Its Own Section

**Status:** Work Complete

- [x] In `MainPanel.java`, remove the End Session button and its bottom row from `buildCommandsSection()`.
- [x] Create a new private method `buildEndSessionSection()` that returns a titled panel containing only `endSessionBtn`.
- [x] In the main panel layout (wherever `buildCommandsSection()` result is added), add the `buildEndSessionSection()` result directly below it.

**Technical Notes:**

- `endSessionBtn` is currently defined in `buildCommandsSection()` (`MainPanel.java` lines ~446–449) and added to the bottom row of the Commands box. Remove it from that row and the bottom row entirely; the bottom row can be deleted once empty.
- The new section should follow the same visual pattern as the existing titled panels (`BorderFactory.createTitledBorder(...)`). A title of `"Session"` is suggested but can be adjusted.
- The `endSessionBtn` field is already a member of `MainPanel` — it does not need to move; only the layout code changes.
- No other files should need to change for this phase.

### Phase 2: Persistent Redo Counter on the Redo Button

**Status:** Work Complete

- [x] Create `src/main/java/com/chatstory/session/RedoCountStore.java` following the existing Gson JSON store pattern. It stores a single `int redoCount` field and persists to `%APPDATA%\ChatStory\redo-count.json`.
- [x] In `MainPanel.java`, inject or construct `RedoCountStore` alongside the other stores.
- [x] On startup, read the stored count and set the Redo button label to `"Redo " + count`.
- [x] In the Redo button's action listener, increment the count, save it via `RedoCountStore`, and update the button label.
- [x] In the Send Controller button's action listener, reset the count to 0, save it via `RedoCountStore`, and update the Redo button label to `"Redo 0"`.

**Technical Notes:**

- `RedoCountStore` should mirror the pattern of `SessionControllerStore` or `CanonFolderStore`: constructor loads from disk (defaulting to 0 if the file is absent), `getCount()` returns the value, `setCount(int n)` saves it to disk immediately.
- Storage location: `%APPDATA%\ChatStory\redo-count.json`. Use the same `AppConfig` path helper that other stores use to resolve `%APPDATA%\ChatStory\`.
- The Redo button is `redoBtn` in `MainPanel.java` (~line 426). Its label is currently set once at construction; replace with a helper method `private void updateRedoLabel()` that calls `redoBtn.setText("Redo " + redoCountStore.getCount())` so the same logic is reused on startup, increment, and reset.
- The Send Controller button action is in `MainPanel.java` (around `sendControllerButton`). Add the reset call there after the existing send logic.
- The count tracks presses since the scene started at beat 1. "Reset on Send Controller" means the user is starting a new scene, which is when the count should clear.

---

## Notes and Risks

- The redo count is intentionally not reset by other actions (End Scene, Reset, etc.) — only Send Controller resets it.
- `RedoCountStore` is a new file in a new package (`com.chatstory.session`). If a `session` package doesn't exist yet, it needs to be created; alternatively the store can live in an existing package such as `com.chatstory.config`.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** [YYYY-MM-DD]
**Phases Completed:** [List or "All"]
**Work Deferred:** [What was not done and why, or "None"]

**Accomplishments:**
- [Pending]

**Metrics:**
- Files modified: [Pending]

**Lessons / Notes:**
[Pending]
