# DevCycle 020: Load and Save Application State

**Status:** VERIFIED
**Start Date:** 2026-06-11
**Target Completion:** TBD
**Focus:** Add user-driven Load/Save functionality backed by a JSON file and exposed through a dropdown menu in the upper-left corner of the application.

---

## Goal

DC20 adds an explicit way for users to save their current ChatStory working state to a JSON file and load it again later. The feature should make session continuity more intentional than the existing per-feature auto-persistence, while reusing the existing JSON store patterns wherever practical. The load/save entry point will be a dropdown menu in the upper-left corner of the application so it is easy to find without taking over the main workflow panels.

## Desired Outcome

At the end of DevCycle 020:

- A dropdown menu appears in the upper-left corner of the application toolbar.
- The dropdown includes clear Load and Save actions.
- Save writes the selected persistent application details to a user-chosen JSON file.
- Load reads a compatible JSON file and restores the saved application details into the running UI.
- The JSON format is structured, versioned, and tolerant of missing future fields.
- Load failures, malformed JSON, and incompatible files are handled with a visible error message and no partial UI corruption.
- Existing automatic persistence behavior continues to work.

---

## Tasks

### Phase 1: Define Save Data Scope and JSON Contract

**Status:** Work Complete

- [x] Identify which existing state should be included in the explicit save file.
- [x] Define a versioned JSON root object, e.g. `schemaVersion`, `savedAt`, and nested state sections.
- [x] Decide how file paths should be represented, using absolute paths for current local-machine compatibility.
- [x] Document which runtime-only state is intentionally excluded.

**Technical Notes:**

Initial recommended save scope:

- Session controller file path from `SessionControllerStore`.
- Intermediate controller file path from `IntermediateControllerStore`.
- Final controller file path from `FinalControllerStore`.
- Context file entries, checked state, last directory, and staging path from `ContextFileStore` if public APIs can support it cleanly.
- Rules file entries from `RulesFileStore`.
- Picture file selection/configuration from `PictureFileStore`.
- Canon folder configuration from `CanonFolderStore`.
- Redo count from `RedoCountStore`.
- Current app mode/theme only if the corresponding models already expose stable read/write APIs.

Recommended exclusions for DC20 unless the implementation proves straightforward:

- JCEF browser page state and ChatGPT conversation state.
- In-memory transcript/canon entries that are not already represented by a dedicated file or store.
- Temporary staged upload files, except for the staging folder setting itself.

The first format can be intentionally local-machine oriented. Cross-machine path rebasing can be deferred to a future DevCycle if needed.

### Phase 2: Add an Application Snapshot Service

**Status:** Work Complete

- [x] Create a small model for the save file, e.g. `ApplicationSnapshot`.
- [x] Create a service responsible for collecting state from stores and applying loaded state back to stores.
- [x] Serialize and deserialize with Gson, matching the existing project JSON approach.
- [x] Ensure load validates the parsed root object before applying any changes.
- [x] Add focused unit tests for round-trip serialization, missing optional fields, malformed JSON, and version handling.

**Technical Notes:**

The service should avoid duplicating store internals where possible. If existing stores lack setters/getters needed for snapshot restore, add narrow public APIs rather than reaching into private fields. Applying a loaded snapshot should update the underlying stores so the existing listener refresh behavior updates the UI naturally.

Suggested implementation names:

- `src/main/java/com/chatstory/session/ApplicationSnapshot.java`
- `src/main/java/com/chatstory/session/ApplicationSnapshotService.java`
- `src/test/java/com/chatstory/ApplicationSnapshotServiceTest.java`

Use a schema version such as `1`, and reject unsupported future major versions with a user-facing error.

### Phase 3: Add Upper-Left Load/Save Dropdown

**Status:** Work Complete

- [x] Add a dropdown menu to the upper-left toolbar area in `AppFrame`.
- [x] Add `Load...` and `Save...` menu items.
- [x] Use `JFileChooser` for selecting the JSON file to load or save.
- [x] Default save files to a `.json` extension.
- [x] Keep the menu available without disrupting the existing DevTools/Test Inject toolbar controls.

**Technical Notes:**

`AppFrame` currently builds a `leftTools` panel in the north toolbar containing DevTools and Test Inject buttons. The new dropdown should be added at the start of that panel so it is visually in the upper-left corner. A `JMenuBar`, `JMenu`, or `JButton` with a `JPopupMenu` are all acceptable Swing approaches, but the final UI should read as a compact dropdown rather than another large right-panel button.

Suggested menu label: `File` or `Session`. Since the feature is explicitly Load/Save, `File` is likely the clearest first pass.

### Phase 4: Load/Save Behavior and Error Handling

**Status:** Work Complete

- [x] Save the current snapshot to the chosen JSON path.
- [x] Show a success status or dialog after a successful save.
- [x] Load a snapshot from the chosen JSON path.
- [x] Validate the snapshot before mutating app state.
- [x] Apply loaded state to stores and refresh dependent UI.
- [x] Show a clear error dialog for read/write failures, malformed JSON, unsupported schema versions, or invalid snapshot contents.

**Technical Notes:**

To avoid partial restore problems, parse and validate the file first, then apply changes. If applying state touches several stores, keep each setter idempotent and listener-friendly. The existing stores generally save immediately when setters are called; that is acceptable for DC20 because a loaded snapshot should become the current persisted app state.

### Phase 5: Verification

**Status:** Work Complete

- [x] Run the relevant unit tests.
- [ ] Manually validate saving a populated app state to a JSON file.
- [ ] Manually validate loading that JSON file after changing state.
- [ ] Manually validate loading after application restart.
- [ ] Manually validate malformed JSON and incompatible schema errors.
- [ ] Confirm existing auto-persistence files are not broken by explicit Load/Save.

**Technical Notes:**

Manual checks should cover at least controller file paths, context/rules lists, picture/canon configuration if included, and redo count. If some state is deferred from the save scope, record that clearly in this document before moving the cycle to completion.

---

## Open Questions

1. **Should the JSON save file include only configuration-style state, or also story/session content?**
   Recommendation: Start with configuration-style state and existing store-backed state. Story content that exists only in memory should be deferred until there is a clearer durable model for it.

2. **Should Load overwrite the current auto-persisted app settings immediately?**
   Recommendation: Yes. Loading a snapshot should become the new current app state so restart behavior matches what the user just loaded.

3. **Should saved path references be portable across machines?**
   Recommendation: Not in DC20. Use absolute paths for a simpler and reliable local workflow, and defer path rebasing/import helpers to a later cycle.

---

## Notes and Risks

- There are already multiple feature-specific JSON files under `%APPDATA%\ChatStory`; DC20 should add explicit snapshot import/export without removing those existing stores.
- The main risk is partial restore if a file is valid JSON but semantically incomplete. Validate the snapshot before applying store mutations.
- Some current stores may need small API additions to expose all state required for a snapshot.
- JCEF/browser state is outside the likely DC20 scope and should not be implied by the term "Save" unless explicitly added later.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-06-11
**Phases Completed:** All implementation phases; manual UI verification remains pending user review.
**Work Deferred:** Browser/ChatGPT conversation state and in-memory story content that is not already represented by an existing store remain deferred.

**Accomplishments:**
- Added a versioned `ApplicationSnapshot` JSON format and `ApplicationSnapshotService`.
- Added upper-left `File` dropdown actions for `Load...` and `Save...`.
- Added shared, persistent last-used snapshot directory tracking for both `Load...` and `Save...`.
- Added restore APIs for context/rules stores and redo-count listener support.
- Updated Context and Rules panels to refresh from store listener events so loaded snapshots repaint visible lists.
- Added focused unit tests for snapshot round-trip, apply, malformed JSON, and unsupported schema validation.

**Metrics:**
- Files modified: 14
- Focused DC20 tests: passing with `./gradlew.bat test --tests com.chatstory.SnapshotFileStoreTest --tests com.chatstory.ApplicationSnapshotServiceTest`
- Full suite: currently blocked by existing `CorrectionTypeTest.endScenePromptIsFormattedDirection` mismatch unrelated to DC20.
- Manual UI verification: pending.

**Lessons / Notes:**
Snapshot restore validates schema version, enum values, redo count, and path syntax before applying state, reducing the chance of partial UI/store mutation from an invalid file.
