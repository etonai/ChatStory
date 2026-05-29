# DevCycle 015: Response Window and Transcript

**Status:** In Progress
**Start Date:** 2026-05-29
**Target Completion:** TBD
**Focus:** Rename the response area label and add a Transcript tab that records every beat version displayed during a session.

---

## Goal

DC15 makes two improvements to the left pane. First, a cosmetic rename: the response area label changes from "Assistant Response" to "Response Window". Second, a new Transcript tab is added alongside Response and Canon. The Transcript records every beat that appears in the response window during the session — including revised versions of the same beat — and lets the user save it to a text file.

## Desired Outcome

At the end of DevCycle 015:

- The response area label reads "Response Window" (and resets to "Response Window" on clear, not "Assistant Response").
- A **Transcript** tab appears in the left pane alongside Response and Canon.
- Every time a beat is displayed in the response window (including revisions of the same beat number), it is appended to the Transcript in order.
- The Transcript tab has a **Save** button that opens a file chooser and writes the transcript to a `.txt` file.
- The Transcript is session-only — it is not persisted across restarts.
- All existing workflows continue to work.

---

## Tasks

### Phase 1: Rename Response Label

**Status:** In Progress

- [ ] In `OutputPanel`, change the initial label text from `"Assistant Response"` to `"Response Window"`.
- [ ] In `OutputPanel.clear()`, change the reset value from `"Assistant Response"` to `"Response Window"`.

### Phase 2: Transcript Tab

**Status:** In Progress

- [ ] Create `TranscriptStore`: an append-only, in-memory list of beat text entries.
  - `add(String text)` — appends one entry.
  - `export()` — joins entries with `"\n\n---\n\n"`.
  - `isEmpty()` — returns true when no entries exist.
  - `addListener(Runnable)` — notifies listeners after each `add`.
- [ ] Create `TranscriptPanel`: a read-only `JTextArea` with a **Save** button.
  - Save opens a `JFileChooser` filtered to `.txt`, writes `transcriptStore.export()`, and shows a brief "Saved!" flash on the button (same pattern as `CanonPanel`).
  - Save is disabled when the transcript is empty; enabled after the first entry is added.
- [ ] Add an `onBeatRecorded` `Consumer<String>` parameter to `OutputPanel`'s constructor. Call it inside `setResponse(String text, int beatNumber)` with the beat text each time a beat is displayed.
- [ ] In `LeftPanePanel`, create a `TranscriptStore` and `TranscriptPanel`. Pass `transcriptStore::add` as `onBeatRecorded` into `OutputPanel`. Add the Transcript tab to the left pane's `JTabbedPane`.

**Technical Notes:**

- The transcript fires on every `setResponse(text, beatNumber)` call — this captures both new beats and revised versions of the same beat number, which is the desired behavior.
- `TranscriptPanel` appends to its text area by listening to `TranscriptStore` (via the listener). It does not re-render the full export on each add; it appends the new entry incrementally with a separator.
- The Save button does not clear the transcript — the transcript is session-only and accumulates until the app closes.
- No changes to `AppFrame`, `Main`, or any store files are needed.

---

## Bugs and Refinements

*None yet.*

---

## Completion Summary

*Fill in when the cycle closes.*

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
