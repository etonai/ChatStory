# DevCycle 016: Input Area Improvements

**Status:** In Progress
**Start Date:** 2026-05-30
**Target Completion:** TBD
**Focus:** Make the input area taller and add a Clear button below the Reset button.

---

## Goal

DC16 makes two small usability improvements to the input area at the bottom of the window. The text area grows by 50% vertically so more of a prompt is visible while typing. A Clear button is added below Reset so the user can wipe the input without having to select-all and delete.

## Desired Outcome

At the end of DevCycle 016:

- The input text area is 50% taller than before (6 rows instead of 4).
- A **Clear** button sits below the Reset button in the button column to the right of the text area.
- Clicking Clear removes all text from the input text area.
- All existing workflows continue to work.

---

## Tasks

### Phase 1: Taller Input Area

**Status:** In Progress

- [ ] In `InputPanel`, change `new JTextArea(4, 60)` to `new JTextArea(6, 60)`.

### Phase 2: Clear Button

**Status:** In Progress

- [ ] Add a **Clear** button to the `actions` panel in `InputPanel`, below the Reset button.
- [ ] On click, call `textArea.setText("")`.

**Technical Notes:**

- `textArea` is already a field on `InputPanel`, so no new parameters are needed.
- Follow the same `BoxLayout(Y_AXIS)` / `Box.createVerticalStrut(4)` pattern already used between Send and Reset.

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
