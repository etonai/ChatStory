# DevCycle 012: Quick Fixes II

**Status:** In Progress
**Start Date:** 2026-05-28
**Target Completion:** None — this cycle remains open indefinitely as a running log of bug fixes and small improvements discovered during user testing.
**Focus:** A continuation of DC 011, collecting small targeted improvements surfaced by user testing.

---

## Goal

DC12 collects quick fixes and small quality-of-life changes surfaced by continued user testing. Like DC11, each phase is independent and small in scope. The cycle does not close — new phases are appended as issues are discovered.

---

## Phase 1: "Make Canon" Correction Action

**Status:** Work Complete

### Desired Outcome

The right-click correction menu in the Response panel includes a new item: **"Make Canon"**. Selecting it with text highlighted sends the prompt `"Do not rewrite anything, but make the following information canonical: "` + selected text to ChatGPT as a formatted direction.

### Implementation

Add `MAKE_CANON` to `CorrectionType`. The `OutputPanel` correction menu is built dynamically from `CorrectionType.values()`, so the new item appears automatically with no changes to UI code.

```text
Modified:
  bridge/CorrectionType.java   add MAKE_CANON enum value
  test/CorrectionTypeTest.java add MAKE_CANON test; update menuLabels test
```

### Tasks

- [x] Add `MAKE_CANON("Make Canon", "Do not rewrite anything, but make the following information canonical: ")` to `CorrectionType`.
- [x] Add test case in `CorrectionTypeTest` for the new value.
- [x] Update `menuLabels` test to include `MAKE_CANON`.

---

## Phase 2: 14pt Font in Input and Response Text Areas

**Status:** Work Complete

### Desired Outcome

The input window and Assistant Response text areas both render at **14pt** instead of the default system font size, making them more comfortable to read and edit.

### Implementation

Call `setFont(textArea.getFont().deriveFont(14f))` on the `JTextArea` in both `InputPanel` and `OutputPanel`. Using `deriveFont` preserves the existing font family and style, changing only the size.

```text
Modified:
  ui/InputPanel.java    set textArea font to 14pt
  ui/OutputPanel.java   set textArea font to 14pt
```

### Tasks

- [x] Set 14pt font on `textArea` in `InputPanel`.
- [x] Set 14pt font on `textArea` in `OutputPanel`.

---

## Phase 3: Additional Correction Actions

**Status:** Work Complete

### Desired Outcome

Three new right-click correction actions appear in the Response panel when text is highlighted:

- **POV Dialogue** — flags that the AI invented a POV character's dialogue, which the story rules prohibit.
- **Beat Reset** — flags that the AI reset the beat, which is not allowed.
- **Header Missing** — flags that the Day # Beat # header is incorrect.

All three follow the same direction format as existing correction actions.

### Implementation

Add three new values to `CorrectionType`. The `OutputPanel` correction menu builds dynamically from `CorrectionType.values()`, so all three items appear automatically.

```text
Modified:
  bridge/CorrectionType.java   add POV_DIALOGUE, BEAT_RESET, HEADER_MISSING
  test/CorrectionTypeTest.java add tests for each new value; update menuLabels test
```

### Tasks

- [x] Add `POV_DIALOGUE` with prefix `"Do not rewrite the beat, but you invented POV character dialogue, which is not allowed: "`.
- [x] Add `BEAT_RESET` with prefix `"Do not rewrite the beat, but you reset the beat, which is not allowed: "`.
- [x] Add `HEADER_MISSING` with prefix `"Do not rewrite the beat, but the Day # Beat # header is incorrect: "`.
- [x] Add test cases for all three new values.
- [x] Update `menuLabels` test to cover all three.

---

## Phase 4: Toolbar Keyboard Shortcuts

**Status:** Work Complete

### Desired Outcome

Three keyboard shortcuts are available from anywhere in the Swing window:

| Shortcut | Action |
|---|---|
| Ctrl-X | Reset |
| Ctrl-R | Redo |
| Ctrl-F | Fetch |

### Implementation

Register a `KeyEventDispatcher` with `KeyboardFocusManager`. Dispatchers run before any component binding, so the shortcuts fire regardless of which Swing component has focus. Each shortcut delegates to `doClick()` on the corresponding toolbar button, so there is no logic duplication and the button visually responds.

Note: because the dispatcher takes priority over component bindings, Ctrl-X will always trigger Reset rather than Cut when the Swing window has focus. The shortcuts do not fire when the JCEF browser has native focus.

```text
Modified:
  AppFrame.java   add installKeyboardShortcuts(); call before setVisible
```

### Tasks

- [x] Add `installKeyboardShortcuts(resetBtn, redoBtn, fetchBtn)` method to `AppFrame`.
- [x] Bind Ctrl-X → `resetBtn.doClick()`.
- [x] Bind Ctrl-R → `redoBtn.doClick()`.
- [x] Bind Ctrl-F → `fetchBtn.doClick()`.

---

## Adding New Phases

When a new bug or small improvement is identified during user testing, add a new `## Phase N` section above this footer with a short description, implementation notes, and a task checklist. Mark the phase **Work Complete** when implementation is done and verification is pending. Do not close this document.

---

## Completion Summary

*This cycle does not close. Leave this document in `doc/planning/` indefinitely.*
