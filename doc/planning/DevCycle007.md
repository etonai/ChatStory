# DevCycle 007: Canon Continuity

**Status:** Planning
**Start Date:** 2026-05-28
**Target Completion:** TBD
**Focus:** Let users accumulate selected ChatGPT responses into a persistent canon record that can be saved as a Markdown file.

---

## Goal

DC7 gives the user a way to build a running record of the ChatGPT responses they consider authoritative. After each assistant response appears in the output panel, the user can click "Add to Canon" to append it to an in-memory canon list. A Canon tab in the left pane shows all accumulated entries in order. When the user is done, they can save the full canon to a Markdown file.

This is the first step toward a durable story record that accumulates across multiple ChatGPT exchanges within a session.

## Desired Outcome

At the end of DevCycle 007:

- The left pane is tabbed, with a `Response` tab and a `Canon` tab.
- The `Response` tab contains what `OutputPanel` shows today — the latest assistant response with the `Copy` button.
- An `Add to Canon` button sits in the `Response` tab header next to `Copy`.
- Clicking `Add to Canon` appends the current response text to the in-memory canon list.
- The `Canon` tab displays all accumulated canon entries in the order they were added, separated by a visible divider.
- A `Save` button in the `Canon` tab opens a file-chooser dialog and writes the full canon to a `.md` file.
- The canon is session-only: it starts empty on each launch and is not automatically persisted across restarts.
- All DC6 behavior — Story Mode, Unassisted Mode, dark/light theme — is preserved with no regressions.

---

## Explicit Non-Goals

DevCycle 007 does not include:

- removing or reordering individual canon entries
- per-entry timestamps or metadata in the saved file
- automatic canon saving or auto-export
- loading a previously saved canon file back into the app
- canon synchronization across multiple windows or sessions
- right-click correction actions
- redo button
- context file upload

---

## Current Foundation

The left pane today is `OutputPanel` — a single panel with a `JTextArea`, a `Copy` button in the header, and a `setResponse(String)` method called by the response-extraction pipeline. `OutputPanel` is placed in the left side of the main split pane in `AppFrame`.

DC7 wraps `OutputPanel` inside a new tabbed structure and adds the canon list alongside it.

---

## Target Additions

```text
src/main/java/com/chatstory/ui/
  CanonPanel.java          displays accumulated canon entries; Save button
  LeftPanePanel.java       JTabbedPane wrapping OutputPanel and CanonPanel

src/main/java/com/chatstory/canon/
  CanonStore.java          in-memory ordered list of canon entries; add and export operations

src/test/java/com/chatstory/canon/
  CanonStoreTest.java
```

`OutputPanel` is modified to add the `Add to Canon` button and accept a `CanonStore` reference.

`AppFrame` is modified to replace the bare `OutputPanel` with `LeftPanePanel`.

---

## Tasks

### Phase 1: Canon Data Model

**Status:** Planning

- [ ] Create `CanonStore` in `com.chatstory.canon`.
- [ ] `CanonStore` holds an ordered `List<String>` of canon entries.
- [ ] Implement `add(String responseText)` — appends the entry; ignores null or blank text.
- [ ] Implement `getEntries()` — returns an unmodifiable snapshot of the list.
- [ ] Implement `export()` — returns all entries joined by a Markdown horizontal rule (`\n\n---\n\n`) as a single `String`.
- [ ] Implement `isEmpty()`.
- [ ] Write `CanonStoreTest` covering: empty on construction, add and retrieve, blank/null add is ignored, export format with one entry, export format with multiple entries.

**Technical Notes:**

`CanonStore` is pure Java with no Swing dependency. It does not need to be thread-safe beyond the EDT — all calls will originate from UI actions on the EDT. Keep it simple.

The export separator (`---`) renders as a horizontal rule in Markdown, which naturally divides entries when the file is viewed in a Markdown reader.

---

### Phase 2: Left Pane Tab Structure

**Status:** Planning

- [ ] Create `LeftPanePanel` — a `JPanel` containing a `JTabbedPane` with two tabs: `Response` and `Canon`.
- [ ] The `Response` tab holds the existing `OutputPanel`.
- [ ] The `Canon` tab holds the new `CanonPanel` (stubbed for now — can be an empty placeholder until Phase 3).
- [ ] Replace the bare `OutputPanel` reference in `AppFrame` with `LeftPanePanel`.
- [ ] Confirm the app still launches and the existing response display works correctly.

**Technical Notes:**

`AppFrame` currently places `OutputPanel` directly into the left side of a `JSplitPane`. After this phase, it places `LeftPanePanel` there instead. The response-pipeline call to `outputPanel.setResponse(...)` continues to work because `LeftPanePanel` exposes a delegation method or holds a direct reference to `OutputPanel`.

Apply the current dark/light theme to `LeftPanePanel` and its `JTabbedPane` consistently with how `ConfigurationPanel` and `ParsePreviewPanel` are themed.

---

### Phase 3: Add to Canon Button

**Status:** Planning

- [ ] Add a `CanonStore` instance to the application — created in `Main` and passed where needed, or created in `AppFrame`.
- [ ] Modify `OutputPanel` to accept a `CanonStore` reference at construction.
- [ ] Add an `Add to Canon` button to the `OutputPanel` header, next to the existing `Copy` button.
- [ ] Clicking `Add to Canon` calls `canonStore.add(textArea.getText())`.
- [ ] `Add to Canon` is disabled when `textArea` is empty.
- [ ] After a successful add, briefly indicate the action (update button text to "Added!" for ~1 second, then revert — or a status label update). Do not open a dialog.

**Technical Notes:**

Keep the button state simple: enabled when there is response text present, disabled when the response area is empty. Do not tie it to `AppState` — adding to canon is a user decision that can happen at any time while a response is visible.

The brief confirmation avoids silent success (user wonders if the click registered) without blocking them with a popup.

---

### Phase 4: Canon Tab Display

**Status:** Planning

- [ ] Implement `CanonPanel` — a `JPanel` with a read-only `JTextArea` and a `Save` button.
- [ ] `CanonPanel` holds a reference to `CanonStore`.
- [ ] Implement `refresh()` on `CanonPanel` — reads `canonStore.export()` and sets it as the text area content.
- [ ] Call `canonPanel.refresh()` after each successful `Add to Canon` action so the Canon tab always reflects the current state.
- [ ] Apply theme styling to `CanonPanel` consistently with other panels.

**Technical Notes:**

The Canon tab text area is display-only — the user cannot edit the accumulated canon inside the app. Editing the saved Markdown file externally is out of scope for DC7.

`JTextArea` is fine here; no Markdown rendering is needed. The content is readable plain text.

Refreshing the whole text area on each add is acceptable for now. Canon entries are not expected to be numerous enough within a session to make this a performance concern.

---

### Phase 5: Save to File

**Status:** Planning

- [ ] Add a `Save` button to `CanonPanel`.
- [ ] Clicking `Save` opens a `JFileChooser` in save mode, defaulting to a `.md` file extension.
- [ ] If the user confirms a path, write `canonStore.export()` to that file using UTF-8 encoding.
- [ ] If the write succeeds, briefly confirm in the button or a label ("Saved.").
- [ ] If the write fails, show a simple error dialog with the failure reason.
- [ ] `Save` is disabled when `canonStore.isEmpty()`.

**Technical Notes:**

Use `JFileChooser` with a `FileNameExtensionFilter` for `.md` files. Do not default to any specific directory; let the OS decide the initial directory.

Write the file with `Files.writeString(path, content, StandardCharsets.UTF_8)`. Wrap in a try/catch and show a `JOptionPane.showMessageDialog` on failure.

The save path is not remembered between saves in DC7. If the user saves twice, the chooser opens fresh each time.

---

### Phase 6: Tests and Manual Validation

**Status:** Planning

- [ ] Run `gradlew.bat clean test` — confirm all existing tests plus new `CanonStoreTest` pass.
- [ ] Manually validate: response appears in `Response` tab, `Add to Canon` appends it, `Canon` tab shows all entries, `Save` produces a readable Markdown file.
- [ ] Manually validate: `Add to Canon` is disabled when no response is present.
- [ ] Manually validate: `Save` is disabled when canon is empty.
- [ ] Manually validate: multiple adds produce correctly separated entries in the Canon tab and saved file.
- [ ] Manually validate: Story Mode and Unassisted Mode send behavior is not regressed.
- [ ] Manually validate: dark and light theme apply correctly to both new tabs.

---

## Manual Validation Plan

### Test 1: No Regression

Steps:
1. Run `gradlew.bat run`.
2. Send a prompt in Story Mode.
3. Confirm response appears in `Response` tab.
4. Confirm `Copy` button still works.

Pass: No DC6 regression.

### Test 2: Add to Canon

Steps:
1. Wait for an assistant response to appear.
2. Click `Add to Canon`.
3. Switch to the `Canon` tab.

Pass: The response text appears in the Canon tab.

### Test 3: Multiple Entries

Steps:
1. Add two responses to canon.
2. Inspect the Canon tab.

Pass: Both entries appear in order, separated by a visible horizontal rule.

### Test 4: Save to File

Steps:
1. Add at least one response to canon.
2. Click `Save`, choose a location.
3. Open the file in a text editor or Markdown viewer.

Pass: File contains all canon entries separated by `---`. Text matches what appeared in the Canon tab.

### Test 5: Disabled States

Steps:
1. Launch the app before any response has arrived.
2. Inspect the `Add to Canon` button.
3. Inspect the `Save` button in the Canon tab.

Pass: Both are disabled.

### Test 6: Theme

Steps:
1. Launch in dark mode, inspect both tabs.
2. Switch to light mode.

Pass: Both `Response` and `Canon` tabs update correctly in both themes.

---

## Notes and Risks

- `OutputPanel` currently has no way to know whether its text area is empty short of checking `textArea.getText().isEmpty()`. The `Add to Canon` button enable/disable logic should use this directly.
- The `JTabbedPane` in `LeftPanePanel` needs theme application. Check that `ConfigurationPanel`'s theme-application approach in DC6 covers tabbed panes, or add it.
- Refreshing the entire Canon text area on each add is a simple approach. If a future DC adds many entries and performance suffers, switch to append-only updates at that time.
- `JFileChooser` on Windows can be slow to open on first use. This is a known JVM behavior and not something to fix in DC7.

---

## Open Questions

1. **Should the `Add to Canon` button be disabled or hidden while the app is in Unassisted Mode?**
   In Unassisted Mode the response panel is not updated from native response extraction — the ChatGPT response is only visible in the browser. If a previous Story Mode response is still showing in the response area, the user could add it to canon while in Unassisted Mode. Is that acceptable, or should canon adds be gated to Story Mode only?
   Recommendation: Allow it in both modes. Canon adds are a user decision about whatever text is currently in the response area, regardless of how it got there.

2. **Should the Canon tab switch automatically when the user clicks `Add to Canon`?**
   Automatically switching to the Canon tab after each add gives immediate visual feedback, but it may be disruptive if the user is in a flow of reviewing the response.
   Recommendation: Do not auto-switch. The brief confirmation on the button is enough. Let the user choose when to check the Canon tab.

3. **Should adding the same response text twice be allowed?**
   If the user clicks `Add to Canon` on the same response twice (perhaps by mistake), the entry will appear twice in the canon list.
   Recommendation: Allow duplicates for now. Adding deduplication logic in DC7 adds complexity without clear benefit — the user controls when they click the button.

4. **Should the app warn the user if they close the window with unsaved canon entries?**
   A close-confirmation dialog would prevent accidental data loss, but the canon is session-only by design and the user should already know to save before closing.
   Recommendation: No close warning in DC7. If this becomes a real pain point it can be added later.

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
