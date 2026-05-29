# DevCycle 014: Streamlining Context Upload

**Status:** VERIFIED
**Start Date:** 2026-05-29
**Target Completion:** TBD
**Focus:** Reduce friction when uploading context files to ChatGPT, starting with one-click sending of the Session Controller file.

---

## Goal

DC14 improves the workflow for uploading context material to ChatGPT. Right now, sending context to ChatGPT requires manual copy-paste or staging steps. DC14 begins reducing that friction by introducing a dedicated Session Controller control on the MAIN tab: the user picks the controller file once, the path is remembered, and a single button reads the file and sends its contents as a prompt.

## Desired Outcome

At the end of DevCycle 014 (Phase 1):

- The MAIN tab contains a Session Controller section with a file path field and a Browse button.
- The selected controller file path is persisted across sessions.
- A **Send Controller** button reads the controller file from disk and sends its full text to ChatGPT as a prompt.
- If no controller file is configured, Send Controller is disabled.
- If the controller file cannot be read, the user receives an error message.
- All existing workflows (context staging, Canon, beat tracking, corrections) continue to work.

---

## Tasks

### Phase 1: Session Controller on MAIN Tab

**Status:** Verified

- [x] Create `SessionControllerStore` to persist the controller file path (modeled on `CanonFolderStore`).
- [x] Add a `sessionControllerPath` entry to `AppConfig` pointing to the store's JSON file in `%APPDATA%\ChatStory\`.
- [x] Create `MainPanel` to replace the empty `JPanel` currently occupying the MAIN tab in `AppFrame`.
- [x] Add a **Session Controller** section to `MainPanel`:
  - A read-only text field showing the current controller file path (or placeholder text when none is set).
  - A **Browse** button that opens a file chooser; selected path is saved to `SessionControllerStore`.
  - A **Send Controller** button that reads the file from disk and calls `chatBridge.sendPrompt(...)`.
- [x] Disable **Send Controller** when no path is configured.
- [x] Show an error dialog if the file cannot be read at send time.
- [x] Wire `MainPanel` into `AppFrame` in place of the empty MAIN tab panel.

**Technical Notes:**

- `SessionControllerStore` follows the same pattern as `CanonFolderStore`: a single JSON file, a `getPath()` / `setPath()` method, and a listener list for UI refresh.
- `AppConfig` already manages paths for context files and Canon config — add `getSessionControllerConfigPath()` alongside those.
- The Send Controller button sends file content as a raw prompt string via the same `chatBridge.sendPrompt(text, listener)` path used by other toolbar buttons. The response listener can reuse `AppFrame.statusResponseListener(...)`.
- The file chooser should remember the last directory (or default to the parent of the current controller path if one is set).
- No preprocessing of the file content in Phase 1 — send the raw text verbatim.

### Phase 2: Rules Context

**Status:** Verified

- [ ] Create `RulesFileStore` to persist the rules file list (modeled on `ContextFileStore`, without the staging-path concept — rules always stage to the shared context-staging directory).
- [ ] Add a `getRulesFileListPath()` method to `AppConfig` pointing to `%APPDATA%\ChatStory\rules-files.json`.
- [ ] Create `RulesPanel` (modeled on `ContextPanel`) and add it as a **Rules** tab in `AppFrame` alongside Context, Configuration, and Parsed Input.
  - Add Files, Remove Checked, Select All, and file viewer right-click menu — same as Context tab.
  - No Stage button — staging is triggered from MAIN via Send Rules.
  - No staging path label.
- [ ] Add `clearStaging()` to `ContextFileStore` to delete all files currently in the staging directory.
- [ ] Add `clickUploadFile()` to the `ChatBridge` interface and implement it in `ChatGptBridge` as a fire-and-forget DOM click via a new `click_upload_file.js` script.
- [ ] Add a **Send Rules** button to `MainPanel` (below the Session Controller section from Phase 1).
  - On click: call `contextFileStore.clearStaging()`, copy all files from `rulesFileStore.getEntries()` into the staging directory, then call `chatBridge.clickUploadFile()`.
  - Disable Send Rules when the rules list is empty.
  - Show an error dialog if any file cannot be copied or if the staging directory cannot be cleared.
- [ ] Wire `RulesFileStore` and the `chatBridge.clickUploadFile()` call into `AppFrame`.

**Technical Notes:**

- `RulesFileStore` is simpler than `ContextFileStore`: it stores only a file list and a last-browsed directory. No staging path, no staging logic — those live in `ContextFileStore` and `MainPanel`.
- **Send Rules stages ALL files in the rules list**, regardless of checkbox state. The checkboxes in `RulesPanel` are for the user's organizational convenience (e.g., reviewing which files are registered) but do not gate the send.
- `clearStaging()` on `ContextFileStore` deletes every file in `getStagingPath()` but does not delete the directory itself. It is not recursive — only top-level files are cleared. Log a warning for any file that cannot be deleted rather than aborting.
- `click_upload_file.js` should locate the file upload input element on the ChatGPT page and trigger a click. This is fire-and-forget: the app does not wait for an upload result or confirmation. The selector for the upload button will need to be discovered via DevTools and added to `chatgpt_selectors.json`.
- `ChatBridge.clickUploadFile()` is not part of the prompt/response cycle and does not interact with `AppState`. It executes the JS immediately via `domBridge.execute(browser, script)`.
- Send Rules does not send a prompt and does not update the response panel.

### Phase 4: Intermediate Controller on MAIN Tab

**Status:** In Progress

- [ ] Create `IntermediateControllerStore` to persist the intermediate controller file path (identical pattern to `SessionControllerStore`).
- [ ] Add a `getIntermediateControllerConfigPath()` method to `AppConfig` pointing to `%APPDATA%\ChatStory\intermediate-controller.json`.
- [ ] Add an **Intermediate Controller** section to `MainPanel` (below the Context section):
  - A read-only text field showing the current file path (or placeholder when none is set).
  - A **Browse** button that opens a file chooser; selected path is saved to `IntermediateControllerStore`.
  - A **Send Intermediate** button that reads the file and calls `onSendPrompt`.
- [ ] Disable **Send Intermediate** when no path is configured.
- [ ] Show an error dialog if the file cannot be read at send time.
- [ ] Wire `IntermediateControllerStore` into `AppFrame` and pass it to `MainPanel`.

**Technical Notes:**

- `IntermediateControllerStore` is a structural copy of `SessionControllerStore` with its own config file (`intermediate-controller.json`) and no shared state.
- `MainPanel` gains a second `JTextField` and `JButton` pair mirroring the existing controller section. The `buildControllerSection()` pattern is duplicated as `buildIntermediateControllerSection()`.
- The Send Intermediate button reuses the same `onSendPrompt` consumer already wired in from `AppFrame`, with a `statusResponseListener("Intermediate controller sent")` label.
- The file chooser defaults to the parent of the current intermediate controller path, falling back to no starting directory if none is set.

### Phase 3: Context Upload from MAIN Tab

**Status:** Work Complete

- [x] Add checkbox-state persistence to `ContextFileStore`: store which files are checked so the selection survives app restarts.
- [x] Update `ContextPanel` to restore each checkbox's checked state from `ContextFileStore` on load, and save state back to `ContextFileStore` whenever a checkbox changes.
- [x] Add a **Context** section to `MainPanel` below the Rules section, with a **Send Context** button and a status label.
- [x] Send Context button behavior: clear the staging folder, copy all currently-checked context files into it, then trigger the upload shortcut (Ctrl+U via `Robot`). Abort with an error dialog if any file fails to copy.
- [x] Disable Send Context when no context files are checked.
- [x] Update the status label after staging to show how many files were staged.
- [x] Wire the checked-file supplier from `ContextFileStore` into `MainPanel`.

**Technical Notes:**

- Checkbox state persistence belongs in `ContextFileStore` alongside the file list. Add a `Set<Path> checkedFiles` field, serialize it in the JSON, and expose `isChecked(Path)`, `setChecked(Path, boolean)`, and `getCheckedEntries()` methods. `getCheckedEntries()` returns only the entries that are both present in the file list and marked checked.
- `ContextPanel` currently decides check state only in-memory. Change `addCheckbox(Path)` to initialize the checkbox from `contextFileStore.isChecked(path)`, and add an `ItemListener` that calls `contextFileStore.setChecked(path, cb.isSelected())` on every change.
- `MainPanel` already holds a reference to `ContextFileStore`, so Send Context can call `contextFileStore.clearStaging()` → `contextFileStore.stageSelected(contextFileStore.getCheckedEntries())` → `onClickUpload.run()` — no new parameters needed.
- The Send Context button should listen to `ContextFileStore` for changes to the checked set so it enables/disables correctly as the user checks and unchecks files in the Context tab.

### Phase 8: Continue Button

**Status:** In Progress

- [ ] Add a `CONTINUE_PROMPT` constant to `CorrectionType` using `direction("Continue the scene")`.
- [ ] Add a **Continue** button in the Commands section of `MainPanel`, immediately after the Redo button.
- [ ] Pass an `onContinue` `Runnable` into `MainPanel` alongside the existing command runnables.
- [ ] In `AppFrame`, pass a runnable that calls `chatBridge.sendPrompt(CorrectionType.CONTINUE_PROMPT, statusResponseListener("Continue sent"))`.

### Phase 7: End Session Button

**Status:** In Progress

- [ ] Add a public `saveCanon()` method to `CanonPanel` (expose the existing `saveToFile()` logic).
- [ ] Add a public `endSession()` method to `LeftPanePanel`:
  - If `currentBeatModel.hasUnappendedBeat()`, call `canonPanel.appendEntry(currentBeatModel.getText())` and `currentBeatModel.markAppended()`.
  - Then call `canonPanel.saveCanon()` to open the save-file dialog.
- [ ] Add an **End Session** button to the Commands section in `MainPanel`, sized larger than the other buttons.
- [ ] Pass an `onEndSession` `Runnable` into `MainPanel` alongside the existing four command runnables.
- [ ] In `AppFrame`, pass `leftPane::endSession` as `onEndSession`.

**Technical Notes:**

- `endSession()` on `LeftPanePanel` runs on the Swing EDT; `saveCanon()` opens a `JFileChooser` which is also EDT-safe.
- The unappended-beat check in `CanonPanel.saveToFile()` already prompts the user with a YES/NO/CANCEL dialog before saving. `endSession()` bypasses that prompt by appending unconditionally first, so the save dialog opens with the beat already in canon.
- Button sizing: set a larger font (e.g., `deriveFont(14f)`) and `setPreferredSize` / `setMinimumSize` to make it visually distinct from Redo/End Scene/Reset/Fetch.

### Phase 6: Move Action Buttons to MAIN Tab

**Status:** In Progress

- [ ] Add a **Commands** section at the bottom of `MainPanel` containing **Redo**, **End Scene**, **Reset**, and **Fetch** buttons.
- [ ] Pass the four button actions into `MainPanel` as `Runnable` parameters (`onRedo`, `onEndScene`, `onReset`, `onFetch`) so `MainPanel` owns the buttons but `AppFrame` owns the logic.
- [ ] Remove Redo, End Scene, Reset, and Fetch from `AppFrame`'s toolbar `leftTools` panel.
- [ ] Update `AppFrame.installKeyboardShortcuts` to accept `Runnable`s instead of `JButton` refs so the Ctrl+X / Ctrl+R / Ctrl+F shortcuts continue to work.

**Technical Notes:**

- `MainPanel` does not need references to `chatBridge`, `leftPane`, or `statusLabel` — all four actions are fully encapsulated in the `Runnable`s passed from `AppFrame`.
- The Commands section uses a simple `FlowLayout` row of buttons, consistent with how they appeared in the toolbar.
- Keyboard shortcuts (`installKeyboardShortcuts`) currently calls `resetBtn.doClick()` etc. Change the method signature to `installKeyboardShortcuts(Runnable onReset, Runnable onRedo, Runnable onFetch)` and call the runnables directly.
- DevTools and Test Inject remain in the toolbar — only the four listed buttons move.

### Phase 5: Final Controller on MAIN Tab

**Status:** In Progress

- [ ] Create `FinalControllerStore` to persist the final controller file path (identical pattern to `SessionControllerStore`).
- [ ] Add a `getFinalControllerConfigPath()` method to `AppConfig` pointing to `%APPDATA%\ChatStory\final-controller.json`.
- [ ] Add a **Final Controller** section to `MainPanel` (below the Intermediate Controller section):
  - A read-only text field showing the current file path (or placeholder when none is set).
  - A **Browse** button that opens a file chooser; selected path is saved to `FinalControllerStore`.
  - A **Send Final** button that reads the file and calls `onSendPrompt`.
- [ ] Disable **Send Final** when no path is configured.
- [ ] Show an error dialog if the file cannot be read at send time.
- [ ] Wire `FinalControllerStore` into `AppFrame` and pass it to `MainPanel`.

**Technical Notes:**

- `FinalControllerStore` is a structural copy of `SessionControllerStore` with its own config file (`final-controller.json`) and no shared state.
- The Send Final button reuses the same `onSendPrompt` consumer already wired in from `AppFrame`, with a `statusResponseListener("Final controller sent")` label.
- The file chooser defaults to the parent of the current final controller path, falling back to no starting directory if none is set.

---

## Open Questions

**Q1: Should Send Controller clear the response panel?**
The `onPromptSubmitted` path no longer clears the response panel (changed in DC13). A controller send is a setup step, not a story beat — confirm whether it should follow the same no-clear rule or explicitly clear.

**Q2: File type filter on the Browse dialogs?**
Should the file choosers for the controller and rules files restrict to `.md` or `.txt`, or accept any file? These are likely Markdown but enforcing a filter may be inconvenient.

**Q3: MAIN tab layout.**
Phase 1 adds a Session Controller section; Phase 2 adds a Send Rules button below it. `MainPanel` should use a `BoxLayout` (stacked vertical sections) from Phase 1 so Phase 2 slots in naturally.

**Q4: Upload File button selector.**
The selector for ChatGPT's file upload input must be discovered via DevTools and added to `chatgpt_selectors.json` before Phase 2 can be fully implemented. This is a discovery task, not a design decision.

**Q5: Send Rules error handling granularity.**
If some rules files copy successfully but others fail, should Send Rules abort (and not click upload) or proceed with whatever staged successfully? Recommend: show an error dialog listing failures and abort — a partial upload could mislead the model.

---

## Notes and Risks

- The MAIN tab is currently an empty `new JPanel(new BorderLayout())` in `AppFrame`. Replacing it with `MainPanel` requires passing `chatBridge`, `SessionControllerStore`, `RulesFileStore`, and `ContextFileStore` into `AppFrame`'s construction chain.
- Sending a large controller file as a prompt is subject to ChatGPT's input length limits. DC14 does not add any truncation or warning — that is a future concern.
- **Send Rules clears the staging directory.** Any files previously staged via the Context tab will be deleted. The user must be aware that Send Rules and Context staging share the same staging folder. A future cycle may separate them if this causes friction.
- The ChatGPT upload button selector in `chatgpt_selectors.json` is the most likely point of fragility — if ChatGPT updates its DOM, `click_upload_file.js` will silently do nothing. Logging a console warning when the element is not found is the minimum mitigation.
- Phase 3 and beyond are not designed here. This document will be extended as new phases are planned.

---

## Bugs and Refinements

### BR-1: Upload button selector did not match — no file dialog appeared

**Discovered:** 2026-05-29, during Phase 2 manual testing.

**Problem:** Clicking Send Rules staged the files correctly but the ChatGPT file upload dialog never appeared. The four selector candidates in `chatgpt_selectors.json` did not match any element in the live ChatGPT DOM, so `click_upload_file.js` silently fell through to the console warning with no visible result.

**Fix (partial — feedback only):** Added a `rulesStatusLabel` to the Rules section of `MainPanel`. After `onClickUpload.run()` is called, the label shows `"N file(s) staged — select them in the browser's upload dialog."` This confirms the Java side fired regardless of whether the DOM click succeeded.

**Fix (full):** Replaced the JS DOM-click approach entirely. `ChatGptBridge.clickUploadFile()` now:
1. Calls `browser.setFocus(true)` to give the embedded browser native OS focus.
2. Waits 100 ms on a virtual thread for focus to settle.
3. Uses `java.awt.Robot` to send a real OS-level Ctrl+U keystroke to the focused browser window.

This is independent of ChatGPT's DOM structure. Ctrl+U triggers ChatGPT's file upload dialog directly as a native keyboard shortcut. The app-level status label added in the partial fix remains in place to confirm the Java side fired. `click_upload_file.js` is retained in resources but is no longer called.

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
