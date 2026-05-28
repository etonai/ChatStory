# DevCycle 011: Quick Fixes

**Status:** Active — ongoing
**Start Date:** TBD
**Target Completion:** None — this cycle remains open indefinitely as a running log of bug fixes and small improvements discovered during user testing.
**Focus:** A collection of small, targeted improvements that do not individually justify a full DevCycle.

---

## Goal

DC11 collects quick fixes and small quality-of-life changes surfaced by early user testing. Each phase is independent and small in scope.

---

## Phase 1: Select All Button in Context File List

**Status:** Complete

### Desired Outcome

A button sits in the button row between "Add Files" and "Remove Checked". Its label reflects the current selection state:

- When any checkbox is unchecked the label reads **"Select All"** — clicking checks all checkboxes.
- When every checkbox is checked the label reads **"Unselect All"** — clicking unchecks all checkboxes.

The button label updates immediately whenever any checkbox changes state.

### Implementation

Add a `JButton selectAllButton` to `ContextPanel`. Its action calls `selectAllOrNone()`, which iterates `checklistPanel.getComponents()`: if any `JCheckBox` is unselected, all are set to selected; otherwise all are set to deselected.

Add `updateSelectAllButton()`, which sets the button label to `"Unselect All"` if every checkbox is selected, or `"Select All"` otherwise. This is called from the existing `ItemListener` on each checkbox (alongside `updateStageButton()`), and also after `selectAllOrNone()` runs.

```text
Modified:
  ui/ContextPanel.java   add selectAllButton; implement selectAllOrNone(); updateSelectAllButton()
```

### Tasks

- [ ] Add `JButton selectAllButton = new JButton("Select All")` field.
- [ ] Implement `selectAllOrNone()`: if any `JCheckBox` in `checklistPanel` is unselected, set all to selected; otherwise set all to deselected. Call `updateSelectAllButton()` after.
- [ ] Implement `updateSelectAllButton()`: set label to `"Unselect All"` if all checkboxes are selected, `"Select All"` otherwise.
- [ ] In `addCheckbox()`, add `updateSelectAllButton()` to the `ItemListener` alongside `updateStageButton()`.
- [ ] Wire `selectAllButton.addActionListener(e -> selectAllOrNone())`.
- [ ] Insert `selectAllButton` into `buttonRow` between `addButton` and `removeButton`.
- [ ] Confirm Stage button enables/disables correctly after Select All / Unselect All.
- [ ] Confirm button label updates correctly as individual checkboxes are toggled.
- [ ] Confirm clicking on an empty list does nothing.

---

---

## Adding New Phases

When a new bug or small improvement is identified during user testing, add a new `## Phase N` section above this footer with a short description, implementation notes, and a task checklist. Mark the phase **Complete** when done. Do not close this document.

---

## Completion Summary

*This cycle does not close. Leave this document in `doc/planning/` indefinitely.*
