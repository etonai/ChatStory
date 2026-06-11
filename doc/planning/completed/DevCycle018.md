# DevCycle 018: Right Panel Navigation

**Status:** Verified
**Start Date:** 2026-05-31
**Target Completion:** TBD
**Focus:** Make the right panel easier to navigate and improve response readability with markdown rendering.

---

## Goal

DC18 improves the usability of the right panel. The tabs are reordered to put the most-used tabs first. A set of global keyboard shortcuts is added so the user can switch to key tabs and trigger common commands without reaching for the mouse. The response window is updated to render AI output as formatted markdown rather than plain text, matching the presentation in the browser. All shortcuts are documented in the README.

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
- The response window renders AI output as formatted markdown (headings, bold, italic, lists, code blocks).
- Copy and Add to Canon operations use the original raw markdown text, not the rendered HTML.
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

**Status:** Work Complete (with known limitation — see Notes and Risks)

- [x] In `AppFrame.java`, extend `installKeyboardShortcuts` to accept the `rightTabs` reference and add the two tab-navigation shortcuts (`Ctrl+Shift+M` → Main tab index 0, `Ctrl+Shift+P` → Picture tab index 1).
- [x] Replace the existing bare `Ctrl+F`, `Ctrl+X`, `Ctrl+R` shortcuts with `Ctrl+Shift+F` (Fetch), `Ctrl+Shift+X` (Reset), `Ctrl+Shift+R` (Redo).
- [x] Add `Ctrl+Shift+S` to trigger Send (submit the current input prompt). Added `triggerSend()` to `InputPanel`; wired via `inputPanel::triggerSend` in `AppFrame`.
- [x] Add `Ctrl+Shift+C` to trigger Send Context. Added `triggerSendContext()` to `MainPanel`; wired via `mainPanel::triggerSendContext` in `AppFrame`.
- [x] Add `Ctrl+Shift+B` to focus the browser window for scrolling. Calls `browser.setFocus(true)` then `document.activeElement.blur()` via JS so the browser receives scroll events without focusing the ChatGPT text input.
- [x] Add `Ctrl+Shift+W` to focus the response window for scrolling. Added `focusResponse()` to `OutputPanel` and `LeftPanePanel`.
- [x] Add `Ctrl+Shift+I` to focus the input area. Added `focusInput()` to `InputPanel`; wired via `inputPanel::focusInput` in `AppFrame`.
- [x] Document all shortcuts in `README.md` under a **Keyboard Shortcuts** section.

**Technical Notes:**

- The existing `installKeyboardShortcuts` in `AppFrame.java` uses `KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher`, which is the correct global hook — extend it rather than adding a second dispatcher.
- Check both `CTRL_DOWN_MASK` and `SHIFT_DOWN_MASK` in `getModifiersEx()` to distinguish the new `Ctrl+Shift` shortcuts from the old bare `Ctrl` ones that are being replaced.
- Tab indices after Phase 1 reorder: Main=0, Picture=1, Context=2, Rules=3, Parsed Input=4, Configuration=5. Use `rightTabs.setSelectedIndex(n)` to switch tabs.
- The Send action needs a reference passed into `installKeyboardShortcuts`; add it as a parameter alongside the existing `onReset`, `onRedo`, `onFetch`.
- The existing bare `Ctrl+X`, `Ctrl+R`, `Ctrl+F` shortcuts are replaced (not kept alongside) to avoid conflicts and confusion.

### Phase 3: Markdown Rendering in the Response Window

**Status:** Work Complete

- [x] Add `commonmark-java` to `build.gradle.kts`: `implementation("org.commonmark:commonmark:0.24.0")`.
- [x] In `OutputPanel.java`, replace the `JTextArea` with a `JEditorPane` configured for `"text/html"` content type.
- [x] Add a `private String currentMarkdown` field to store the raw markdown text received from the AI.
- [x] Write a private `toHtml(String markdown)` helper that converts markdown to an HTML string using commonmark's `Parser` and `HtmlRenderer`, wrapped in a minimal `<html><body>` envelope with a base font style that matches the rest of the app.
- [x] Update `setResponse(String text)` and `setResponse(String text, int beatNumber)` to store the raw text in `currentMarkdown` and call `editorPane.setText(toHtml(text))`.
- [x] Update the **Copy** button to copy `currentMarkdown` (the raw markdown), not the rendered HTML.
- [x] Update the **Add to Canon** action to pass `currentMarkdown` to `canonStore.add(...)`.
- [x] Update the right-click correction menu to use `editorPane.getSelectedText()` for the selection (same API as `JTextArea`).
- [x] Make the `JEditorPane` non-editable (`setEditable(false)`) — the text is AI-generated and corrections go through the correction menu, not direct edits.

**Technical Notes:**

- `OutputPanel` currently uses a `JTextArea` (`textArea` field). The field must be replaced with `JEditorPane editorPane`. All references to `textArea` must be updated.
- `JEditorPane` with `"text/html"` uses Swing's built-in HTML renderer. It supports basic HTML 3.2 / CSS 1 — sufficient for the headings, bold, italic, lists, and code blocks that ChatGPT produces.
- The commonmark dependency: `org.commonmark:commonmark:0.24.0`. Only the core module is needed (no extensions required for basic markdown).
- The `JEditorPane` must have `setEditable(false)` and should have `putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)` so that the font set via `setFont()` is respected within the rendered HTML.
- Text selection for the right-click correction menu works identically on `JEditorPane` — `getSelectedText()` is the same method.
- The `clear()` method should set `currentMarkdown = ""` and `editorPane.setText("")`.
- The `JScrollPane` wrapper, focus recovery, and header/button layout do not change.

---

## Notes and Risks

- The existing `Ctrl+X`, `Ctrl+R`, `Ctrl+F` shortcuts are being replaced by their `Ctrl+Shift` equivalents. Users accustomed to the old shortcuts will need to relearn them.
- **Known limitation:** Shortcuts do not work when the browser has focus. The embedded JCEF browser runs as a native Windows window; keyboard events bypass Java's event system entirely. `Ctrl+Shift+B` traps the user in the browser until they click a Swing component or use the mouse to return focus. Documented in `README.md`. Accepted for now.

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
