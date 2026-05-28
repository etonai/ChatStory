# DevCycle 011: Quick Fixes

**Status:** Verified
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

## Phase 2: Alphabetical Sorting of Context Files

**Status:** Complete

### Desired Outcome

Files in the Context checklist are always displayed in case-insensitive alphabetical order by filename, regardless of the order they were added.

### Implementation

After each call to `checklistPanel.add(cb)` in `addCheckbox()`, re-sort the panel's children by the filename portion of each checkbox's `filePath` client property using a case-insensitive comparator. Remove all components, re-add them in sorted order, then revalidate and repaint.

Extract `sortChecklist()` for clarity. This is called at the end of `addCheckbox()` instead of the bare `revalidate/repaint`.

```text
Modified:
  ui/ContextPanel.java   add sortChecklist(); call from addCheckbox()
```

### Tasks

- [x] Implement `sortChecklist()`: collect all components, sort by `((Path) cb.getClientProperty("filePath")).getFileName().toString()` case-insensitively, remove all, re-add in order, revalidate, repaint.
- [x] Replace the `checklistPanel.revalidate(); checklistPanel.repaint()` calls at the end of `addCheckbox()` with a single `sortChecklist()` call.
- [x] Confirm files added in any order appear alphabetically.
- [x] Confirm existing selection state is preserved after sort.

---

## Phase 3: Default Canon Folder and Auto-Save Temp File

**Status:** Complete

### Desired Outcome

- The Configuration panel has a "Canon Folder" section with a Browse button, mirroring the Staging Folder section.
- The selected folder is persisted across sessions.
- Every time the user clicks "Add to Canon", the full canon text area content is written to `{canonFolder}/00_Canon_Temp.md`.
- When the user explicitly saves the canon via the Save button (file chooser), `00_Canon_Temp.md` is deleted.
- If no canon folder is configured, auto-save is silently skipped.

### Architecture

**`CanonFolderStore`** (new, `com.chatstory.canon`) — persists the canon folder path to `%APPDATA%\ChatStory\canon-config.json`. Provides `getCanonFolder()` (nullable `Path`), `setCanonFolder(Path)`, `getTempFile()`, and listener support.

**`AppConfig`** — gains `getCanonConfigPath()` returning the path to `canon-config.json`.

**`Main`** — creates `CanonFolderStore` and passes it to `AppFrame`.

**`AppFrame`** — threads `CanonFolderStore` to `LeftPanePanel` (for `CanonPanel`) and `ConfigurationPanel` (for the folder picker).

**`LeftPanePanel`** — threads `CanonFolderStore` to `CanonPanel`.

**`CanonPanel`** — after `appendEntry()`, calls `autoSaveTemp()` on a background thread. After a successful explicit save, calls `wipeTemp()`.

**`ConfigurationPanel`** — adds "Canon Folder" section identical in structure to "Staging Folder".

```text
New:
  canon/CanonFolderStore.java

Modified:
  config/AppConfig.java
  Main.java
  AppFrame.java
  ui/LeftPanePanel.java
  ui/CanonPanel.java
  ui/ConfigurationPanel.java
```

### Tasks

- [x] Create `CanonFolderStore` with `getCanonFolder()`, `setCanonFolder(Path)`, `getTempFile()`, `addListener(Runnable)`, load/save via Gson.
- [x] Add `getCanonConfigPath()` to `AppConfig`.
- [x] Construct `CanonFolderStore` in `Main` and pass to `AppFrame`.
- [x] Thread `CanonFolderStore` through `AppFrame` → `LeftPanePanel` → `CanonPanel`.
- [x] Pass `CanonFolderStore` to `ConfigurationPanel`; add "Canon Folder" section with Browse button.
- [x] In `CanonPanel.appendEntry()`, call `autoSaveTemp()` (background thread write to `00_Canon_Temp.md`).
- [x] In `CanonPanel.saveToFile()` on success, call `wipeTemp()` (delete `00_Canon_Temp.md`).

---

## Phase 4: Continuity Error Correction Action

**Status:** Complete

### Desired Outcome

The right-click correction menu in the Response panel includes a fourth item: **"Continuity Error"**. Selecting it with text highlighted sends the prompt `"Do not rewrite the beat, but there is a continuity error with: "` + selected text to ChatGPT, following the same pattern as the existing correction actions.

### Implementation

Add `CONTINUITY_ERROR` to `CorrectionType`. The `OutputPanel` correction menu is built dynamically from `CorrectionType.values()`, so the new item appears automatically with no changes to UI code.

```text
Modified:
  bridge/CorrectionType.java   add CONTINUITY_ERROR enum value
```

### Tasks

- [x] Add `CONTINUITY_ERROR("Continuity Error", "Do not rewrite the beat, but there is a continuity error with: ")` to `CorrectionType`.
- [x] Add test case in `CorrectionTypeTest` for the new value.

---

## Phase 5: "Add to Canon" Button Persists Until New Response

**Status:** Complete

### Desired Outcome

After the user clicks "Add to Canon", the button label changes to **"Added!"** and the button becomes **disabled**, preventing a double-add. It stays that way until a new response arrives. When `setResponse` is called with new content, the button reverts to **"Add to Canon"** and becomes enabled again. The one-second timer revert is removed.

### Implementation

In `OutputPanel`, remove the `Timer` in the `addToCanonButton` action listener. Set text to `"Added!"` and call `setEnabled(false)`. In `setResponse`, reset text to `"Add to Canon"` and re-enable whenever new non-empty content arrives.

```text
Modified:
  ui/OutputPanel.java   remove Timer revert; disable on add; reset in setResponse
```

### Tasks

- [x] Keep label as `"Added!"` in the action listener.
- [x] Call `addToCanonButton.setEnabled(false)` immediately after setting the label.
- [x] Remove the `Timer` that reverted the label after 1 second.
- [x] In `setResponse`, when text is non-null and non-blank, reset `addToCanonButton.setText("Add to Canon")` and re-enable.

---

## Phase 6: Force Fetch Response Button

**Status:** Complete

### Desired Outcome

A **"Fetch"** button in the toolbar reads the last visible assistant message directly from the DOM and puts it in the response panel, bypassing the normal extraction pipeline. This rescues a response that is visible in the browser but was missed by the app — due to selector drift, index mismatch, or a stability timeout.

The button works from any app state (Ready, Error, stuck mid-send). It does not touch the state machine.

### Architecture

- **`fetch_response.js`** — one-shot script that finds the last `[data-message-author-role='assistant']` element, extracts its text, and posts it back via `cefQuery` as type `"manualFetch"`.
- **`ChatGptBridge`** — registers a `"manualFetch"` handler; adds `fetchLatestResponse(Consumer<String>)` that stores a one-shot callback, then executes `fetch_response.js`.
- **`AppFrame`** — adds a "Fetch" button after the Reset button; its action calls `chatBridge.fetchLatestResponse(text -> ...)` and pipes the result directly to `leftPane.setResponse(text)`.

```text
New:
  src/main/resources/js/fetch_response.js

Modified:
  bridge/ChatGptBridge.java    register manualFetch handler; add fetchLatestResponse()
  AppFrame.java                add Fetch button to toolbar
```

### Tasks

- [x] Write `fetch_response.js`: query last assistant element, extract text using same `.markdown` fallback as `extract_response.js`, post `{type:"manualFetch", ok:true, text:...}`.
- [x] Add `manualFetchCallback` field and `handleManualFetch` handler to `ChatGptBridge`.
- [x] Add `fetchLatestResponse(Consumer<String>)` to `ChatGptBridge`.
- [x] Add "Fetch" button to `AppFrame` toolbar after Reset.

---

## Phase 7: REDO as Direction, Add END SCENE Button

**Status:** Complete

### Desired Outcome

- The **Redo** button sends a properly formatted story direction rather than a freeform sentence, consistent with how Story Mode prompts work: `SCENE_INPUT_SEQUENCE:\n1. DIRECTION: "Redo the last story beat"`.
- A new **End Scene** button sits next to Redo in the toolbar and sends `SCENE_INPUT_SEQUENCE:\n1. DIRECTION: "End the scene"`.

### Implementation

`CorrectionType` gains an `END_SCENE_PROMPT` constant alongside `REDO_PROMPT`. Both are built using `ScenePromptBuilder` with a single `DIRECTION` segment, so they stay in sync with the story prompt format automatically.

`AppFrame` adds the End Scene button wired to `END_SCENE_PROMPT`, mirroring the Redo button.

```text
Modified:
  bridge/CorrectionType.java   update REDO_PROMPT; add END_SCENE_PROMPT
  AppFrame.java                add End Scene button
  CorrectionTypeTest.java      update REDO_PROMPT test; add END_SCENE_PROMPT test
```

### Tasks

- [x] In `CorrectionType`, add a private static `direction(String)` helper that uses `ScenePromptBuilder` to build a single-direction prompt.
- [x] Update `REDO_PROMPT` to `direction("Redo the last story beat")`.
- [x] Add `END_SCENE_PROMPT = direction("End the scene")`.
- [x] Add "End Scene" button to `AppFrame` toolbar next to Redo.
- [x] Update `CorrectionTypeTest` for the new prompt values.

---

## Phase 8: Correction Actions as Directions

**Status:** Complete

### Desired Outcome

Right-click correction actions (Context Leakage, Bad Writing, Re-evaluate, Continuity Error) send prompts formatted as story directions, consistent with Redo and End Scene:

```
SCENE_INPUT_SEQUENCE:
1. DIRECTION: "Do not rewrite the beat, but this is bad writing: [selected text]"
```

### Implementation

`CorrectionType.buildPrompt(String selectedText)` currently returns `prefix + selectedText` as a raw string. Change it to wrap the result in `direction(...)`, reusing the existing private helper.

```text
Modified:
  bridge/CorrectionType.java   wrap buildPrompt result in direction()
  CorrectionTypeTest.java      update all buildPrompt assertions
```

### Tasks

- [x] In `buildPrompt`, return `direction(prefix + selectedText)` instead of `prefix + selectedText`.
- [x] Update all `buildPrompt` test assertions to expect the formatted direction string.

---

## Phase 9: Fetch Button Does Not Reset State — Corrections Fail After Fetch

**Status:** Complete

### Bug

After clicking Fetch, right-click correction actions (e.g. Context Leakage) silently failed. `fetchLatestResponse` does not touch the state machine, so if the app was in `Error` or `WaitingForResponse` when Fetch was pressed, `appState.isSendEnabled()` remains false. `sendPrompt` checks this and rejects the call before the correction prompt is sent.

### Fix

Call `chatBridge.reset()` at the start of the Fetch button's action listener, before `fetchLatestResponse`. This puts the app in `Ready` state immediately, so by the time the response lands and the user tries a correction, sends are allowed.

```text
Modified:
  AppFrame.java   call chatBridge.reset() before fetchLatestResponse in Fetch button action
```

### Tasks

- [x] In `AppFrame` Fetch button action listener, call `chatBridge.reset()` before `chatBridge.fetchLatestResponse(...)`.

---

## Phase 10: Editable Response Panel

**Status:** Complete

### Desired Outcome

The response text area is editable so the user can clean up or modify the response before adding it to Canon. `Add to Canon` picks up whatever is currently in the text area, including any edits. During streaming, `setResponse` still replaces the text as normal — editing is only meaningful once the response is complete.

### Implementation

`OutputPanel` gains a `Runnable beforeFocusRequest` constructor parameter and the same JCEF focus recovery `MouseAdapter` pattern used by `CanonPanel`. `LeftPanePanel` already holds `beforeFocusRequest` and passes it through.

```text
Modified:
  ui/OutputPanel.java      setEditable(true); add beforeFocusRequest param; install focus recovery
  ui/LeftPanePanel.java    pass beforeFocusRequest to OutputPanel
```

### Tasks

- [x] Change `textArea.setEditable(false)` to `setEditable(true)` in `OutputPanel`.
- [x] Add `Runnable beforeFocusRequest` parameter to `OutputPanel` constructor.
- [x] Install focus recovery `MouseAdapter` on `textArea`, `scrollPane`, and `scrollPane.getViewport()`.
- [x] In `LeftPanePanel`, pass `beforeFocusRequest` to `OutputPanel`.

---

## Phase 11: Copy to Response Window from Browser

**Status:** Complete

### Desired Outcome

When the user highlights text in the browser window and right-clicks, a **"Copy to Response Window"** item appears at the bottom of the context menu. Selecting it places the highlighted text directly into the Assistant Response panel, exactly as if `setResponse` had been called with that text.

The item only appears when text is selected. It works from any app state and does not affect the state machine.

### Implementation

A new `BrowserContextMenuHandler` (in the `browser` package) extends `CefContextMenuHandlerAdapter`. In `onBeforeContextMenu`, if `params.getSelectionText()` is non-blank, a separator and the "Copy to Response Window" item are appended to the menu using `MENU_ID_USER_FIRST` (26500) as the command ID. In `onContextMenuCommand`, when that ID is matched, `UiThread.run` dispatches the selected text to the `onCopyToResponse` consumer.

`AppFrame` registers the handler via `client.addContextMenuHandler(new BrowserContextMenuHandler(leftPane::setResponse))`. This requires threading `CefClient` as a new parameter through `AppFrame` and updating `Main` accordingly.

```text
New:
  browser/BrowserContextMenuHandler.java

Modified:
  AppFrame.java    add CefClient param; register BrowserContextMenuHandler
  Main.java        pass client to AppFrame
```

### Tasks

- [x] Create `BrowserContextMenuHandler` extending `CefContextMenuHandlerAdapter`.
- [x] In `onBeforeContextMenu`, append separator + "Copy to Response Window" if selection is non-blank.
- [x] In `onContextMenuCommand`, dispatch selected text to consumer via `UiThread.run` when command ID matches.
- [x] Add `CefClient client` parameter to `AppFrame`; register handler after `leftPane` is created.
- [x] Pass `client` to `AppFrame` in `Main`.

---

## Phase 12: Clear Button for Assistant Response

**Status:** Complete

### Desired Outcome

A **"Clear"** button sits in the Assistant Response header between "Add to Canon" and "Copy". Clicking it empties the text area and resets "Add to Canon" to its enabled state (label `"Add to Canon"`, enabled), so the user can type new content and add it to Canon without waiting for a new response.

### Implementation

Add `JButton clearButton` to `OutputPanel`. Its action calls `textArea.setText("")`, resets `addToCanonButton.setText("Add to Canon")`, and calls `addToCanonButton.setEnabled(true)`. Insert `clearButton` into `buttonPanel` between `addToCanonButton` and `copyButton`.

```text
Modified:
  ui/OutputPanel.java   add clearButton; insert into buttonPanel
```

### Tasks

- [x] Add `JButton clearButton = new JButton("Clear")`.
- [x] Action: clear `textArea`, reset and enable `addToCanonButton`.
- [x] Insert `clearButton` into `buttonPanel` between `addToCanonButton` and `copyButton`.

---

## Adding New Phases

When a new bug or small improvement is identified during user testing, add a new `## Phase N` section above this footer with a short description, implementation notes, and a task checklist. Mark the phase **Complete** when done. Do not close this document.

---

## Completion Summary

*This cycle does not close. Leave this document in `doc/planning/` indefinitely.*
