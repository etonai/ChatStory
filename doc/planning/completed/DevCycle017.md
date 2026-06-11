# DevCycle 017: Picture Tab

**Status:** Work Complete
**Start Date:** 2026-05-31
**Target Completion:** TBD
**Focus:** Add a Picture tab to the right panel that lets the user browse and display a PNG or JPG image.

---

## Goal

DC17 adds a new **Picture** tab to the right-panel `JTabbedPane` in `AppFrame`. The tab contains two sections: a **File** section where the user can browse for a PNG or JPG file and see its path, and an **Image** section below it that renders the selected image. No other file types are supported.

## Desired Outcome

At the end of DevCycle 017:

- A **Picture** tab appears in the right panel alongside MAIN, Configuration, Parsed Input, Context, and Rules.
- The **File** section shows a Browse button and a read-only text field displaying the selected file's name (or path).
- Clicking Browse opens a file chooser filtered to `.png` and `.jpg`/`.jpeg` files only.
- The **Image** section displays the selected image, scaled to fit the available space.
- If no file is selected the image section is empty.
- **Next** and **Prev** buttons in the File section navigate to the next or previous PNG/JPG file in the same directory, sorted alphabetically.
- Navigation wraps: Prev on the first file goes to the last; Next on the last goes to the first.
- Next and Prev are disabled when no file is loaded.
- All existing tabs and workflows continue to function.

---

## Tasks

### Phase 1: Picture Tab

**Status:** Work Complete

- [x] Create `src/main/java/com/chatstory/ui/PicturePanel.java` with a `File` titled section (browse button + read-only filename field) and an `Image` titled section (scaled image display).
- [x] Implement the `JFileChooser` filter to accept only `.png`, `.jpg`, and `.jpeg` files.
- [x] Load the selected file as a `BufferedImage`, scale it to fit the panel width while preserving aspect ratio, and paint it in the image section.
- [x] Create `src/main/java/com/chatstory/picture/PictureFileStore.java` to persist the selected image file path to a JSON config file, following the same pattern as `CanonFolderStore`.
- [x] Register the new tab in `AppFrame.java`: `rightTabs.addTab("Picture", new PicturePanel(pictureFileStore))`.

**Technical Notes:**

- Right-panel tabs are assembled in `AppFrame.java` lines 105–110 as a `JTabbedPane` named `rightTabs`.
- Follow the titled-border section pattern already used in `MainPanel` (`BorderFactory.createTitledBorder` + `createEtchedBorder`).
- Use a `JLabel` with a custom `Icon` (via `ImageIcon`) for the image display, or override `paintComponent` on a `JPanel` — either approach is fine. A `JScrollPane` wrapping the image label is acceptable if the image is larger than the panel.
- The file chooser filter: use `FileNameExtensionFilter("PNG and JPG images", "png", "jpg", "jpeg")` from `javax.swing.filechooser`.
- Display only the **file name** (not the full path) in the filename field, but keep the full `Path` internally for loading.
- `PictureFolderStore` stores the selected image file path to a JSON config file using Gson, the same way `CanonFolderStore` (`src/main/java/com/chatstory/canon/CanonFolderStore.java`) does. On startup, `PicturePanel` reads the persisted path and loads the image automatically if the file still exists.

### Phase 2: Next / Prev Navigation

**Status:** Work Complete

- [x] Add **Prev** and **Next** buttons to the File section in `PicturePanel`, placed to the right of the Browse button.
- [x] On click, list all `.png`/`.jpg`/`.jpeg` files in the current file's directory, sort alphabetically, find the current file's index, and step ±1 with wraparound.
- [x] Disable both buttons when no file is loaded; enable them once a file is selected.

**Technical Notes:**

- Use `Files.list(parent)` filtered by extension and sorted by filename (case-insensitive) to build the sibling list.
- Buttons start disabled; call `refreshNavButtons()` after any file change (browse, next, prev, startup load).
- Keep the nav logic in a private helper `navigateTo(int delta)` to avoid duplication between Next and Prev.

---

## Notes and Risks

- Image scaling should be done on the EDT-safe path; loading a large image could block the UI briefly, but for the scope of DC17 a simple synchronous load on the EDT is acceptable.

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
