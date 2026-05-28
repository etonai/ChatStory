# DevCycle 010: Canon Editing, Tab Highlight Fix, Context File Viewer

**Status:** Planning
**Start Date:** TBD
**Target Completion:** TBD
**Focus:** Make the Canon tab editable, fix the selected-tab text contrast problem, and add a right-click file viewer to the Context tab.

---

## Goal

DC10 adds three independent quality-of-life improvements:

1. **Editable Canon** — the Canon text area becomes editable so users can freely revise accumulated canon without losing their changes when new entries are added.
2. **Tab highlight contrast fix** — the selected tab label is currently hard to read because the highlight colour is too close to the text colour. This is corrected for both dark and light themes.
3. **Context file viewer** — right-clicking a file in the Context tab shows a "View" option that loads the file content into a scrollable read-only panel in the lower half of the Context tab, without opening an external window.

All three goals are independent. They can be implemented in any order.

## Desired Outcome

At the end of DevCycle 010:

- The Canon text area accepts keyboard input; the user can freely edit the accumulated canon text.
- When a new entry is added via "Add to Canon", the new text is appended to whatever is already in the text area (with a `---` separator) rather than replacing it, so prior edits are not lost.
- "Save" writes `textArea.getText()` to file, reflecting any in-text edits.
- The selected tab label is clearly readable in both dark and light themes.
- Right-clicking a file in the Context checklist shows a popup menu with a single "View" item.
- Selecting "View" loads the full file content into a read-only, line-wrapping `JTextArea` in the lower half of the Context panel, inside a `JSplitPane`.
- The viewer panel is scrollable and handles large files without truncation.
- The viewer is initially collapsed (divider at the bottom); it expands when the first "View" is triggered.
- All DC9 behaviour is preserved with no regressions.

---

## Explicit Non-Goals

DevCycle 010 does not include:

- Persisting in-flight Canon edits back to `CanonStore` entries (the store is append-only; edits live in the text area until saved to file)
- Syntax highlighting or formatting in the Canon or file viewer
- Editing files directly from the Context viewer
- Multiple simultaneous file viewer tabs
- Keyboard shortcut for "View"

---

## Architecture

### Goal 1: Editable Canon

`CanonPanel` today calls `refresh()` → `canonStore.export()` → `textArea.setText(...)` every time a new entry is added, which would overwrite user edits.

The fix is to change the `onCanonAdded` callback from a `Runnable` to a `Consumer<String>` carrying the text that was just added. `OutputPanel` already has the text — it passes `textArea.getText()` to `canonStore.add(...)`. Rather than calling a no-arg refresh, it can pass that same string directly to `CanonPanel`, which appends it (with a separator if the text area is non-empty) instead of re-rendering the whole store.

`saveToFile` changes to read `textArea.getText()` instead of `canonStore.export()`. The `saveButton` enabled state uses `!textArea.getText().isBlank()` via a `DocumentListener` so it stays accurate as the user edits.

`CanonStore` itself is unchanged.

### Goal 2: Tab Highlight Contrast

`NativeThemeApplier` sets `JTabbedPane` foreground/background but does not override the LAF's selected-tab rendering, which produces low contrast in dark mode.

The fix is to call `UIManager.put("TabbedPane.selected", color)` and `UIManager.put("TabbedPane.foreground", color)` (and the focus/highlight variants) before `SwingUtilities.updateComponentTreeUI(window)` in `NativeThemeApplier.apply(...)`. For dark theme, the selected tab background should be noticeably lighter than `DARK_PANEL` (e.g. `DARK_FIELD` or a dedicated highlight colour) so the label text contrasts clearly. For light theme, the selected tab should similarly have a clear contrast from the panel colour.

This is a two-line UIManager change scoped inside `NativeThemeApplier`.

### Goal 3: Context File Viewer

`ContextPanel` currently has:
- `CENTER`: `JScrollPane(checklistPanel)`
- `SOUTH`: button row + labels

The new layout splits `CENTER` into a vertical `JSplitPane`:
- **Top**: existing `JScrollPane(checklistPanel)`
- **Bottom**: `JScrollPane(viewerTextArea)` — read-only, line-wrapping `JTextArea`

The split pane starts with the divider at the bottom (viewer collapsed). When "View" is selected the divider moves to roughly 60 % of the panel height to give the viewer useful space.

A `JPopupMenu` with a single "View" `JMenuItem` is attached to each `JCheckBox` via a `MouseListener` checking `isPopupTrigger()` in both `mousePressed` and `mouseReleased`. The action reads the file at the checkbox's `filePath` client property, sets `viewerTextArea.setText(content)`, scrolls to the top, and expands the split pane if it is currently collapsed.

---

## Target Changes

```text
Modified:
  ui/CanonPanel.java          make text area editable; accept Consumer<String> instead of Runnable;
                              append new entries; save reads textArea; saveButton uses DocumentListener
  ui/OutputPanel.java         change onCanonAdded from Runnable to Consumer<String>
  ui/LeftPanePanel.java       update OutputPanel construction to pass Consumer<String>
  theme/NativeThemeApplier.java  add UIManager tab selection colour keys before updateComponentTreeUI
  ui/ContextPanel.java        add JSplitPane with file viewer; right-click "View" on each checkbox
```

No new files. No new tests required (all changes are UI-only; the one logic change — appending vs replacing — is validated manually).

---

## Tasks

### Phase 1: Editable Canon

**Status:** Planning

- [ ] In `CanonPanel`, change `textArea.setEditable(false)` to `setEditable(true)`.
- [ ] Change the constructor parameter `Runnable onCanonAdded` to `Consumer<String> appendedText`.
- [ ] Replace `refresh()` with `appendEntry(String text)`: if `textArea.getText()` is blank, set it to `text`; otherwise append `"\n\n---\n\n" + text`.
- [ ] Change `saveToFile()` to read `textArea.getText()` instead of `canonStore.export()`.
- [ ] Replace the static `saveButton.setEnabled(!canonStore.isEmpty())` call in `refresh()` with a `DocumentListener` on `textArea` that enables the save button when the text area is non-blank and disables it when blank.
- [ ] In `OutputPanel`, change `onCanonAdded` from `Runnable` to `Consumer<String>` and call `onCanonAdded.accept(textArea.getText())` in the action listener (after `canonStore.add(...)`).
- [ ] In `LeftPanePanel`, update the `OutputPanel` constructor call to pass `canonPanel::appendEntry`.

### Phase 2: Tab Highlight Contrast Fix

**Status:** Planning

- [ ] In `NativeThemeApplier.apply(...)`, before calling `SwingUtilities.updateComponentTreeUI(window)`, call `applyTabUiDefaults(theme)`.
- [ ] Implement `applyTabUiDefaults(NativeTheme theme)`: for dark theme, set `UIManager.put("TabbedPane.selected", ...)` to a colour clearly lighter than `DARK_PANEL` (e.g. `new Color(70, 75, 85)`), and set `UIManager.put("TabbedPane.foreground", DARK_TEXT)`. For light theme, set selected to a colour clearly distinct from `LIGHT_PANEL` (e.g. `Color.WHITE`) and foreground to `LIGHT_TEXT`.
- [ ] Run the app, switch to dark and light themes, and confirm the selected tab label is clearly readable in both.

### Phase 3: Context File Viewer

**Status:** Planning

- [ ] Add `JTextArea viewerArea` (editable false, line-wrap true, wrap-style word) and `JScrollPane viewerScroll` as fields.
- [ ] Add `JSplitPane splitPane` (vertical split) — top: existing `scrollPane` (checklist), bottom: `viewerScroll`.
- [ ] Replace `add(scrollPane, BorderLayout.CENTER)` with `add(splitPane, BorderLayout.CENTER)`.
- [ ] Set the initial divider location so the viewer is collapsed (e.g. `splitPane.setDividerLocation(0.999)` deferred via `SwingUtilities.invokeLater`).
- [ ] Extract `addCheckbox(Path path)` change: after creating each `JCheckBox`, attach a `MouseListener` that calls `maybeShowViewMenu(e, path)` in both `mousePressed` and `mouseReleased`.
- [ ] Implement `maybeShowViewMenu(MouseEvent e, Path path)`: if `e.isPopupTrigger()`, show a `JPopupMenu` with a single "View" item; the item's action calls `viewFile(path)`.
- [ ] Implement `viewFile(Path path)`: read file bytes as UTF-8 (handle `IOException` by showing the error message in the viewer); set `viewerArea.setText(content)`; `viewerArea.setCaretPosition(0)`; if the divider is near the bottom, set it to `0.55`.

---

## Manual Validation Plan

### Test 1: No Regression

Steps:
1. Run `gradlew.bat run`.
2. Send a prompt, add response to Canon, stage a context file, confirm all DC9 behaviour works.

Pass: No regression.

### Test 2: Canon Editing

Steps:
1. Add a response to Canon. Text appears in Canon tab.
2. Edit some words directly in the Canon text area.
3. Add a second response to Canon.
4. Confirm the first entry's edits are still present and the second entry is appended with a `---` separator.
5. Click "Save" and confirm the file contains the edited first entry and the second entry.

Pass: Edits survive new additions; saved file reflects edits.

### Test 3: Tab Labels Readable

Steps:
1. Switch to Dark theme. Click through all tabs (Response, Canon, MAIN, Configuration, Parsed Input, Context).
2. Confirm each selected tab label is clearly legible.
3. Switch to Light theme. Repeat.

Pass: No low-contrast selected tab labels in either theme.

### Test 4: Right-Click View — Normal File

Steps:
1. Add a context file.
2. Right-click it in the Context tab → select "View".
3. Confirm the file content appears in the lower panel.
4. Scroll through the content.

Pass: Full file content shown, scrollable.

### Test 5: Right-Click View — Large File

Steps:
1. Add a large file (e.g. > 100 KB).
2. Right-click → "View".

Pass: Content loads without truncation or error; panel scrolls.

### Test 6: Right-Click View — Missing File

Steps:
1. Add a file, then delete it from disk.
2. Right-click → "View".

Pass: Viewer shows a readable error message rather than crashing.

---

## Notes and Risks

- `Consumer<String>` is already used in the `OutputPanel` / `LeftPanePanel` chain (DC9 added `onSendPrompt`), so adding a second `Consumer<String>` parameter to `OutputPanel` requires care to keep constructor parameter order clear. `onCanonAdded` moves from parameter 2 to position 2, changing type only — `LeftPanePanel` is the only caller, so the impact is contained.
- UIManager tab colour keys vary between LAFs. Test after `SwingUtilities.updateComponentTreeUI` to confirm the keys take effect. If the active LAF ignores `TabbedPane.selected`, a custom `TabRenderer` on each `JTabbedPane` is the fallback.
- `JSplitPane.setDividerLocation(double)` only works after the component is realized (visible on screen). Wrap in `SwingUtilities.invokeLater` or set it in `addNotify()` to avoid the divider snapping back to 0.
- File viewer reads the full file into memory — acceptable for context files (text/markdown, typically small). No streaming needed.

---

## Open Questions

1. **Should the Canon text area show a dirty indicator (e.g. title change or asterisk) when the user has unsaved edits?**
   Recommendation: No for DC10 — keep it simple. The save button being enabled is sufficient feedback.

2. **Should "View" replace the currently viewed file or stack views?**
   Recommendation: Replace — one viewer panel, always shows the last file viewed. Stacking adds complexity with no clear benefit.

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
