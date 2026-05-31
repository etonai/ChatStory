# DevCycle 018: Right Panel Navigation

**Status:** Verified
**Start Date:** 2026-05-31
**Target Completion:** TBD
**Focus:** Make the right panel easier to navigate by reordering its tabs and adding keyboard shortcuts.

---

## Goal

DC18 improves the usability of the right panel. The tabs are reordered to put the most-used tabs first. A set of global keyboard shortcuts is added so the user can switch to key tabs and trigger common commands without reaching for the mouse. All shortcuts are documented in the README.

## Desired Outcome

At the end of DevCycle 018:

- The right panel tabs appear in this order: **Main, Picture, Context, Rules, Parsed Input, Configuration**.
- The following global keyboard shortcuts work from anywhere in the app:
  - `Ctrl+Shift+M` — switch to the Main tab
  - `Ctrl+Shift+P` — switch to the Picture tab
  - `Ctrl+Shift+F` — Fetch
  - `Ctrl+Shift+X` — Reset
  - `Ctrl+Shift+R` — Redo
  - `Ctrl+Shift+S` — Send (submit the current input)
- All shortcuts are listed in `README.md`.
- No tab content or behavior changes.
- All existing workflows continue to function.

---

## Tasks

### Phase 1: Tab Reorder

**Status:** Work Complete

- [x] In `AppFrame.java`, reorder the `rightTabs.addTab(...)` calls to: Main, Picture, Context, Rules, Parsed Input, Configuration.

**Technical Notes:**

- Tab order is determined solely by the order of `addTab` calls in `AppFrame.java` (lines 105–111 as of DC17).
- No other files need to change.

### Phase 2: Keyboard Shortcuts

**Status:** Work Complete

- [x] In `AppFrame.java`, extend `installKeyboardShortcuts` to accept the `rightTabs` reference and add the two tab-navigation shortcuts (`Ctrl+Shift+M` → Main tab index 0, `Ctrl+Shift+P` → Picture tab index 1).
- [x] Replace the existing bare `Ctrl+F`, `Ctrl+X`, `Ctrl+R` shortcuts with `Ctrl+Shift+F` (Fetch), `Ctrl+Shift+X` (Reset), `Ctrl+Shift+R` (Redo).
- [x] Add `Ctrl+Shift+S` to trigger Send (submit the current input prompt). Added `triggerSend()` to `InputPanel`; wired via `inputPanel::triggerSend` in `AppFrame`.
- [x] Add `Ctrl+Shift+C` to trigger Send Context. Added `triggerSendContext()` to `MainPanel`; wired via `mainPanel::triggerSendContext` in `AppFrame`.
- [x] Document all shortcuts in `README.md` under a **Keyboard Shortcuts** section.

**Technical Notes:**

- The existing `installKeyboardShortcuts` in `AppFrame.java` uses `KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher`, which is the correct global hook — extend it rather than adding a second dispatcher.
- Check both `CTRL_DOWN_MASK` and `SHIFT_DOWN_MASK` in `getModifiersEx()` to distinguish the new `Ctrl+Shift` shortcuts from the old bare `Ctrl` ones that are being replaced.
- Tab indices after Phase 1 reorder: Main=0, Picture=1, Context=2, Rules=3, Parsed Input=4, Configuration=5. Use `rightTabs.setSelectedIndex(n)` to switch tabs.
- The Send action needs a reference passed into `installKeyboardShortcuts`; add it as a parameter alongside the existing `onReset`, `onRedo`, `onFetch`.
- The existing bare `Ctrl+X`, `Ctrl+R`, `Ctrl+F` shortcuts are replaced (not kept alongside) to avoid conflicts and confusion.

---

## Notes and Risks

- The existing `Ctrl+X`, `Ctrl+R`, `Ctrl+F` shortcuts are being replaced by their `Ctrl+Shift` equivalents. Users accustomed to the old shortcuts will need to relearn them.

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
