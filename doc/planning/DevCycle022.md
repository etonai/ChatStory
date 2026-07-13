# DevCycle 022: Fix Canon Append Freeze in Response Processing

**Status:** Work Complete
**Start Date:** 2026-07-13
**Target Completion:** TBD
**Focus:** Address the likely Swing threading freeze when a completed ChatGPT response is processed as a new story beat and appended to Canon.

---

## Goal

ChatStory can freeze after ChatGPT finishes a response, while the application is rolling over from one story beat to the next and appending the previous beat to Canon. The latest freeze log narrows the hang to `CanonPanel.appendEntry`: the console shows entry into the method with `onEdt=false`, but no matching exit line appears. This cycle should fix the leading EDT-safety issue and add any remaining targeted logging needed to confirm whether response processing now completes cleanly.

## Desired Outcome

At the end of DevCycle 022:

- Response-complete UI processing does not mutate Swing components from CEF/browser callback threads.
- `CanonPanel.appendEntry` either always runs its Swing work on the EDT or clearly enforces that callers dispatch through the EDT before calling it.
- The automatic rollover path, manual `Fetch` path, browser context-menu fetch path, manual `Add to Canon`, and end-session append path all use a consistent, EDT-safe Canon append path.
- Logs around the suspected freeze point are detailed enough to distinguish `getText`, `setText`, caret update, document listener updates, temp auto-save dispatch, and CEF callback acknowledgement.
- The previous observed hang signature, `CanonPanel.appendEntry enter ... onEdt=false` with no `appendEntry exit`, should no longer occur.

---

## Tasks

### Phase 1: Confirm the freeze path and EDT boundaries

**Status:** Work Complete

- [x] Review every current caller of `LeftPanePanel.onResponseComplete`, `OutputPanel.setResponse`, and `CanonPanel.appendEntry`.
- [x] Document which callers may originate from CEF/browser callback threads and which originate from Swing event handlers.
- [x] Confirm whether `CurrentBeatModel` state is only touched from one thread after the fix, or whether it needs its own confinement/guard.

**Technical Notes:**
The latest captured log stops after:

```text
[CanonPanel] appendEntry enter thread=Thread-4760 onEdt=false textLen=2687
```

with no matching `[CanonPanel] appendEntry exit`. The call path appears to be:

- `ChatGptBridge.handleResponseComplete(...)`
- `ResponseListener.onResponseComplete(...)` in `AppFrame.statusResponseListener(...)`
- `LeftPanePanel.onResponseComplete(...)`
- `CanonPanel.appendEntry(...)`

Relevant files:

- `src/main/java/com/chatstory/bridge/ChatGptBridge.java`
- `src/main/java/com/chatstory/AppFrame.java`
- `src/main/java/com/chatstory/ui/LeftPanePanel.java`
- `src/main/java/com/chatstory/ui/OutputPanel.java`
- `src/main/java/com/chatstory/ui/CanonPanel.java`
- `src/main/java/com/chatstory/UiThread.java`

Caller review:

- CEF/browser callback paths: `ChatGptBridge.handleResponseComplete(...)` through `AppFrame.statusResponseListener(...)`, `ChatGptBridge.fetchLatestResponse(...)` through the `Fetch` button callback, and `BrowserContextMenuHandler(leftPane::onResponseComplete)` may call into response processing from non-EDT threads.
- Swing event paths: manual `Add to Canon`, `End Session`, and save-dialog append prompts originate from Swing action handlers and should already be on the EDT.
- `CurrentBeatModel` is safest if confined to the EDT. DC22 therefore dispatches `LeftPanePanel.onResponseComplete(...)` to the EDT before parsing/updating/appending/displaying, rather than splitting parsing and UI mutation across threads.

### Phase 2: Make response UI processing EDT-safe

**Status:** Work Complete

- [x] Decide the EDT boundary: either dispatch `LeftPanePanel.onResponseComplete(...)` internally via `UiThread.run(...)`, or dispatch from all external callers before invoking it.
- [x] Ensure `CanonPanel.appendEntry(...)` does not directly call `JTextArea.getText()`, `setText(...)`, `setCaretPosition(...)`, or button-update side effects off the EDT.
- [x] Ensure `OutputPanel.setResponse(...)` does not call `onBeatRecorded.accept(...)` off the EDT when that callback can reach Swing components or shared UI state.
- [x] Keep the beat rollover operation ordered: update current beat, append rolled-over beat when needed, update response display, and mark appended state in a predictable sequence.

**Technical Notes:**
The cleanest likely fix is to make `LeftPanePanel.onResponseComplete(...)` marshal its whole body to the EDT when called from a non-EDT thread. That keeps beat parsing, `CurrentBeatModel.update(...)`, Canon append, and response display updates in one ordered UI transaction. Parsing is cheap enough that keeping it on the EDT is acceptable for current response sizes.

`CanonPanel.appendEntry(...)` can also defensively wrap itself with `UiThread.run(...)` when `SwingUtilities.isEventDispatchThread()` is false. If that approach is chosen, avoid returning before logs clearly show that work was queued. Consider splitting into a private `appendEntryOnEdt(...)` helper so the public method is a dispatch wrapper and the Swing-mutating method has a clear EDT assumption.

Implemented recommendation: `LeftPanePanel.onResponseComplete(...)` is the primary EDT boundary. `OutputPanel.setResponse(...)` and `CanonPanel.appendEntry(...)` also defensively dispatch to the EDT if called directly from a non-EDT thread.

### Phase 3: Add focused freeze-point logging

**Status:** Work Complete

- [x] In `CanonPanel.appendEntry`, add before/after logs around blank-input check, `textArea.getText()`, updated text construction, `textArea.setText(...)`, `setCaretPosition(0)`, and `autoSaveTemp(...)`.
- [x] In the `DocumentListener.updateButtons`, log thread name and entry/exit while this bug is being investigated.
- [x] In `LeftPanePanel.onResponseComplete`, include `onEdt=` in the entry log.
- [x] In `OutputPanel.setResponse`, log whether the method and the `onBeatRecorded` callback are running on the EDT.
- [x] In `ChatGptBridge.handleResponseComplete`, log immediately before and after `callback.success("")`.

**Technical Notes:**
The current `ChatGptBridge.handleResponseComplete(...)` calls the listener before acknowledging the CEF query callback with `callback.success("")`. If UI processing hangs, the browser bridge callback remains open too. Logging around `callback.success("")` will clarify whether the Java-side response handling returns to CEF cleanly after the EDT fix.

Keep this logging targeted and temporary enough that it can be reduced later. The goal is to prove the freeze point moved or disappeared, not to create permanent high-volume logs.

### Phase 4: Verification

**Status:** Work Complete

- [x] Run the relevant Java tests for beat parsing/current-beat behavior.
- [x] Compile the project.
- [ ] Manually test a normal ChatGPT send/response cycle that rolls from one beat to the next.
- [ ] Manually test the `Fetch` button path.
- [ ] Manually test the browser context-menu response capture path if practical.
- [ ] Manually test `Add to Canon` and `End Session` append/save behavior.
- [ ] Confirm logs show Canon append work running on the EDT and reaching `appendEntry exit`.

**Technical Notes:**
Useful verification commands may include:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test --tests "com.chatstory.*Beat*"
```

Manual verification is important because the suspected bug depends on JCEF callback threading and Swing event timing.

---

## Open Questions

1. **Should `ChatGptBridge` acknowledge `callback.success("")` before invoking UI listeners?**
   Recommendation: Consider this after the EDT fix. Acknowledging earlier could decouple CEF from UI work, but it changes callback ordering and error visibility. The first fix should be Swing thread confinement.

2. **Should `CanonPanel.appendEntry(...)` be synchronous or asynchronous when called off the EDT?**
   Recommendation: Prefer asynchronous `UiThread.run(...)` for consistency with the existing helper. If callers need to know when append completes, introduce an explicit callback/future later rather than blocking a CEF callback thread with `invokeAndWait`.

3. **Should parsing stay on the CEF callback thread while only Swing updates move to the EDT?**
   Recommendation: Keep the whole `LeftPanePanel.onResponseComplete(...)` body on the EDT for now. It preserves ordering and confines `CurrentBeatModel` to the UI thread. Revisit only if response parsing becomes measurably expensive.

---

## Notes and Risks

- This cycle is a fix cycle, not just a logging cycle. DC21 already provided enough evidence to identify the leading hypothesis.
- The freeze may still have a second cause after the EDT violation is fixed. The added logging should make that visible if it happens.
- Avoid broad refactoring of response handling unless needed for thread safety. The smallest high-confidence change is to establish one clear EDT boundary and make Canon append defensive.
- `CanonPanel.java` currently has duplicate `java.nio.file.Files` imports. Cleaning that up is harmless if this file is edited during implementation, but it is not the core bug.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-07-13
**Phases Completed:** 1-3 and automated verification items from Phase 4
**Work Deferred:** Manual JCEF/Swing verification remains pending user approval; the cycle is not marked Verified.

**Accomplishments:**
- Confined `LeftPanePanel.onResponseComplete(...)` response processing to the Swing EDT before beat parsing, current-beat updates, Canon append, and response display updates.
- Added defensive EDT dispatch in `CanonPanel.appendEntry(...)` and `OutputPanel.setResponse(...)` for direct non-EDT callers.
- Added focused logs around Canon append internals, `DocumentListener.updateButtons`, response/display EDT status, and `ChatGptBridge.handleResponseComplete(...)` callback acknowledgement.
- Removed the duplicate `Files` import from `CanonPanel.java` while editing that file.

**Metrics:**
- Files modified: 5 (`ChatGptBridge.java`, `LeftPanePanel.java`, `OutputPanel.java`, `CanonPanel.java`, `DevCycle022.md`)
- `./gradlew.bat compileJava` passes.
- `./gradlew.bat test --tests "com.chatstory.*Beat*"` passes.

**Lessons / Notes:**
The implementation follows the recommendation to keep `CurrentBeatModel` and Swing mutations on the EDT. `ChatGptBridge.handleResponseComplete(...)` still invokes the listener before `callback.success("")`, but the listener now queues UI work and returns quickly when called from a CEF thread, reducing the chance that CEF waits on Swing work.
