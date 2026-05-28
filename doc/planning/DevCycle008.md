# DevCycle 008: Context File Staging

**Status:** Planning
**Start Date:** TBD
**Target Completion:** TBD
**Focus:** Let users maintain a persistent list of context files and copy selected ones to a staging folder for manual upload to ChatGPT.

---

## Goal

DC8 gives users a way to manage a reusable list of context files (plain text and Markdown) that they want to provide to ChatGPT. Because ChatGPT's file upload uses a native OS file picker that cannot be driven programmatically from an embedded browser, the app's role is to copy the user's selected files into a single known staging folder. The user then opens ChatGPT's file picker once, navigates to that folder, and uploads everything in one action.

The file list persists across app restarts so the user does not need to re-add their context files each session.

## Desired Outcome

At the end of DevCycle 008:

- The right pane has a `Context` tab.
- The `Context` tab shows a scrollable checklist of files the user has added.
- Each entry shows the filename; the app tracks the full path internally.
- The user can add files via a file chooser dialog (`.txt` and `.md` only; multi-select supported).
- The user can remove individual files from the list.
- Duplicate paths are silently ignored when adding.
- Files that no longer exist on disk are shown with a visual warning but remain in the list.
- Checking files and clicking `Stage Selected` copies those files into the staging folder (`%LOCALAPPDATA%\ChatStory\context-staging\`).
- If a file with the same name already exists in the staging folder, it is overwritten.
- The staging folder is not managed or cleared by the app — the user handles it manually.
- The file list is saved to `%APPDATA%\ChatStory\context-files.json` and reloaded on next launch.
- All DC7 behavior is preserved with no regressions.

---

## Explicit Non-Goals

DevCycle 008 does not include:

- automatic upload of files into ChatGPT
- interaction with ChatGPT's file picker or upload DOM elements
- file content preview inside the app
- support for file types other than `.txt` and `.md`
- reordering files in the list
- auto-clearing the staging folder
- watching files for changes on disk
- syncing the staging folder back to the file list

---

## Staging Folder Contract

The staging folder is `%LOCALAPPDATA%\ChatStory\context-staging\`.

`AppConfig` already creates `%LOCALAPPDATA%\ChatStory\` at startup. The staging subfolder is created on first use by `ContextFileStore` or the staging action, whichever runs first.

The staging folder is not touched by the app except during a staging action. Its contents between sessions are the user's responsibility.

---

## Persistence Contract

The file list is stored as a JSON array of absolute path strings:

```json
[
  "C:\\Users\\user\\Documents\\worldbuilding.md",
  "C:\\Users\\user\\Documents\\characters.txt"
]
```

Storage path: `%APPDATA%\ChatStory\context-files.json`

`AppConfig` will expose a new `getContextFileListPath()` getter returning this path, consistent with how `getConfigFilePath()` and `getProfilePath()` are exposed. The JSON file is written on every add or remove action so the list is always up to date.

Gson is already on the classpath from DC2. Use `Gson` for reading and writing.

If the file is absent on launch, the list starts empty — no error. If the file is malformed, log a warning and start with an empty list — never crash on startup due to a bad context file list.

---

## Target Additions

```text
src/main/java/com/chatstory/context/
  ContextFileStore.java        persistent ordered list of context file paths

src/main/java/com/chatstory/ui/
  ContextPanel.java            right-pane tab: checklist, Add/Remove/Stage buttons

src/test/java/com/chatstory/
  ContextFileStoreTest.java
```

`AppConfig` is modified to add `getContextFileListPath()` and `getStagingFolderPath()`.

`AppFrame` is modified to add the `Context` tab to the right-pane `JTabbedPane`.

---

## Tasks

### Phase 1: AppConfig Path Extensions

**Status:** Work Complete

- [x] Add `getContextFileListPath()` to `AppConfig` — returns `%APPDATA%\ChatStory\context-files.json`.
- [x] Add `getStagingFolderPath()` to `AppConfig` — returns `%LOCALAPPDATA%\ChatStory\context-staging`. Staging folder not created here; created on first staging action.

**Technical Notes:**

Both paths derive from directories `AppConfig` already resolves. No new directory resolution logic is needed — just expose the paths as strings.

---

### Phase 2: Context File Store

**Status:** Work Complete

- [x] Create `ContextFileStore` in `com.chatstory.context`.
- [x] Constructor accepts the JSON file path (String) and the staging folder path (String).
- [x] On construction, load the file list from the JSON file. If absent or malformed, start with an empty list and log.
- [x] Implement `add(Path filePath)` — appends if not already present (deduplicate by absolute path); saves to JSON immediately.
- [x] Implement `remove(Path filePath)` — removes the entry; saves to JSON immediately.
- [x] Implement `getEntries()` — returns an unmodifiable `List<Path>` in insertion order.
- [x] Implement `stageSelected(List<Path> selected)` — creates the staging folder if absent; copies each path in `selected` to the staging folder, overwriting any existing file with the same name; returns a `StagingResult` with counts of successes and failures.
- [x] Define `StagingResult` as a simple record: `int succeeded`, `int failed`, `List<String> failureMessages`.
- [x] Write `ContextFileStoreTest` — 12 test cases; I/O tests use `@TempDir`.

**Technical Notes:**

`ContextFileStore` is pure Java with no Swing dependency.

Use `Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)` for the staging copy. Wrap each copy individually so one failure does not abort the rest.

JSON persistence: serialize with `new Gson().toJson(pathStrings)` where `pathStrings` is a `List<String>` of absolute paths. Deserialize with `gson.fromJson(reader, String[].class)` and convert back to `Path` objects. Wrap deserialization in a try/catch — any exception yields an empty list.

`ContextFileStoreTest` cases:
- Empty list on construction when JSON file is absent.
- Empty list on construction when JSON file is malformed.
- Add a path and retrieve it.
- Adding the same path twice results in one entry.
- Remove a path.
- Remove a path not in the list is a no-op.
- `getEntries()` is unmodifiable.
- Insertion order is preserved across add calls.
- `stageSelected` with a valid file returns `succeeded = 1`, `failed = 0`.
- `stageSelected` with a missing file returns `succeeded = 0`, `failed = 1`.

Note: tests that exercise real file I/O should use `@TempDir` (JUnit 5) to create throwaway directories.

---

### Phase 3: Context Panel UI

**Status:** Work Complete

- [x] Create `ContextPanel` in `com.chatstory.ui`.
- [x] Display a scrollable vertical list of checkboxes, one per file in `ContextFileStore.getEntries()`.
- [x] Each checkbox label shows the filename only; full `Path` stored in the checkbox client property.
- [x] Missing files shown with italic `[missing]` prefix; still checkable; staging produces a failure entry.
- [x] Three buttons: `Add Files`, `Remove Checked`, `Stage Checked`.
- [x] `Add Files` opens a multi-select `JFileChooser` filtered to `.txt` and `.md`.
- [x] `Remove Checked` removes checked entries from both the UI and `ContextFileStore`.
- [x] `Stage Checked` shows result label: `"X file(s) staged to context-staging"` or `"X staged, Y failed — check console"`.
- [x] `Stage Checked` disabled when no checkboxes are checked.
- [x] Staging folder path displayed as a small label below the buttons.
- [x] `Context` tab added to right-pane `JTabbedPane` in `AppFrame`.
- [x] `ContextFileStore` created in `Main`, passed to `AppFrame` and `ContextPanel`.
- [x] Startup log prints context list path and staging path.

**Technical Notes:**

Use a `JPanel` with `BoxLayout.Y_AXIS` inside a `JScrollPane` to hold the checkboxes. A `DefaultListModel` with a custom renderer is an alternative but adds complexity without benefit here — plain `JCheckBox` components on a panel are simpler and sufficient.

`ContextPanel` should reload the checkbox list from `ContextFileStore.getEntries()` at construction. No live reload after that — the panel is the source of truth for what is checked, and `ContextFileStore` is the source of truth for what is stored.

The status message line below the buttons can be a `JLabel` that starts blank and is updated after each staging action. It does not need to auto-clear.

Swing `JFileChooser` on Windows already supports multi-select via `setMultiSelectionEnabled(true)`. File filtering: `new FileNameExtensionFilter("Text and Markdown files", "txt", "md")`.

Apply theme styling through `NativeThemeApplier` as with all other panels — no extra wiring needed.

---

### Phase 5: Persist Last Browse Directory

**Status:** Work Complete

- [x] Add `lastDirectory` field to `ContextFileStore` (nullable `Path`).
- [x] Change JSON format from flat `String[]` to an object (`StoredData`) with `files`, `lastDirectory`, and `stagingPath` fields.
- [x] Add `getLastDirectory()` — returns last browsed directory, or `null` if never set.
- [x] Add `setLastDirectory(Path dir)` — stores and saves immediately.
- [x] `ContextPanel.addFiles()` sets initial chooser directory from `getLastDirectory()` and saves `chooser.getCurrentDirectory()` after approval.

**Technical Notes:**

`JFileChooser.getCurrentDirectory()` returns the directory the chooser was in when the user clicked Approve — even if the user navigated away from the initial directory. This is the value to persist.

---

### Phase 6: Configurable Staging Folder

**Status:** Work Complete

- [x] `ContextFileStore` now has `currentStagingPath` (defaults to constructor arg, overridden from JSON if saved).
- [x] Add `getStagingPath()`, `setStagingPath(Path)`, `addStagingPathListener(Runnable)`.
- [x] `stageSelected()` uses `currentStagingPath`.
- [x] `ContextPanel` staging path label is a field; listener updates it when path changes via `UiThread.run`.
- [x] `ConfigurationPanel` has a "Staging Folder" section: path label (truncated with `…` + tooltip), `Browse...` button. Updates its own label and calls `contextFileStore.setStagingPath()` on confirm.
- [x] `ConfigurationPanel` now takes `ContextFileStore` as a third constructor parameter.
- [x] `AppFrame` passes `contextFileStore` to `ConfigurationPanel`; `stagingFolderPath` string parameter removed from `AppFrame`.
- [x] 4 new tests added: `lastDirectory` persists, `stagingPath` defaults, `stagingPath` persists, listener notified on change.

**Technical Notes:**

`ConfigurationPanel` updates its own path label directly in the Browse button's action listener — no need for a listener there. `ContextPanel`'s label is updated through the `stagingPathListener` because `ConfigurationPanel` and `ContextPanel` are independent panels with no direct reference to each other.

Show the path in the Configuration tab truncated if longer than ~50 characters (`"..." + last 47 chars`), with the full path as a `setToolTipText`. Small font (`11pt plain`) consistent with other informational labels in the app.

---

### Phase 4: Tests and Manual Validation

**Status:** In Progress

- [x] Run `gradlew.bat clean test` — all tests pass (existing suite + 12 new `ContextFileStoreTest` cases).
- [ ] Manually validate: `Context` tab appears in right pane.
- [ ] Manually validate: `Add Files` opens a file chooser filtered to `.txt` and `.md`.
- [ ] Manually validate: added files appear as checkboxes showing filename only.
- [ ] Manually validate: duplicate add is silently ignored.
- [ ] Manually validate: file list survives app restart (relaunch and confirm list is restored).
- [ ] Manually validate: `Remove Checked` removes selected entries.
- [ ] Manually validate: `Stage Checked` copies checked files to the staging folder.
- [ ] Manually validate: staging a file that has since been deleted produces a failure message, not a crash.
- [ ] Manually validate: `Stage Checked` is disabled when nothing is checked.
- [ ] Manually validate: dark and light themes apply correctly to the `Context` tab.

---

## Manual Validation Plan

### Test 1: No Regression

Steps:
1. Run `gradlew.bat run`.
2. Confirm ChatGPT loads, session is active, DevTools opens.
3. Confirm `Response` and `Canon` tabs still work.

Pass: No DC7 regression.

### Test 2: Add Files

Steps:
1. Open the `Context` tab.
2. Click `Add Files`, select one `.md` and one `.txt` file.

Pass: Both appear in the checklist by filename.

### Test 3: Persistence

Steps:
1. Add two files.
2. Close and relaunch the app.
3. Open the `Context` tab.

Pass: Both files are still in the list.

### Test 4: Stage Selected

Steps:
1. Check one file.
2. Click `Stage Checked`.
3. Open `%LOCALAPPDATA%\ChatStory\context-staging\` in Explorer.

Pass: The file is present in the staging folder. Status message says "1 file(s) staged to context-staging".

### Test 5: Missing File

Steps:
1. Add a file, then delete it from disk.
2. Relaunch the app and open the `Context` tab.
3. Check the missing file and click `Stage Checked`.

Pass: Status message says "0 staged, 1 failed — check console". App does not crash.

### Test 6: Remove Checked

Steps:
1. Check one file in the list.
2. Click `Remove Checked`.

Pass: Entry disappears from the list. Relaunching the app confirms it is not in the persisted list.

### Test 7: Duplicate Add

Steps:
1. Click `Add Files` and add a file.
2. Click `Add Files` again and add the same file.

Pass: Only one entry appears in the list.

---

## Notes and Risks

- `JFileChooser` on Windows can be slow to open. This is a known JVM behavior — acceptable for DC8.
- Files with identical filenames but different directory paths will collide in the staging folder. The later copy overwrites the earlier one. This is acceptable and expected; the user should be aware their context files need distinct names. Log a warning when an overwrite occurs.
- `ContextFileStore` writes the JSON file on every mutation. For a small list of context files this is fine. If the list ever grows very large, batch writes would be worth considering — defer to a future cycle.
- The `[missing]` visual indicator requires checking `Files.exists(path)` when building the checkbox list. Do this at panel construction time only, not on every repaint.

---

## Open Questions

1. **Should there be a "Select All" / "Deselect All" convenience?**
   Recommendation: Skip for DC8. The list is expected to be short and manual checking is sufficient.

2. **Should the staging folder path be shown somewhere in the UI** so the user knows where to look?
   Recommendation: Yes — show it as a small label below the button row (e.g., `"Staging: C:\Users\...\context-staging"`). Static text, not a clickable link. This removes any ambiguity about where to navigate in the file picker.

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
