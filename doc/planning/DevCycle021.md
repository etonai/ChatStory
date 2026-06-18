# DevCycle 021: Diagnostic Logging for Automated Fetch / Beat Pipeline

**Status:** Work Complete
**Start Date:** 2026-06-17
**Target Completion:** TBD
**Focus:** Add targeted console logging across the automated response-fetch and beat-parsing pipeline to pinpoint where the application freezes.

---

## Goal

The application intermittently freezes around the point where it automatically fetches/extracts a ChatGPT response and processes it as a story beat. There is currently no visibility into which stage of that pipeline is running when a freeze happens: JS-side polling/extraction, the CEF query callback into `ChatGptBridge`, `AppState` transitions, `BeatParser.parse`, `CurrentBeatModel.update`, or `OutputPanel`/`CanonPanel` UI updates on the EDT. DC21 adds console log lines at each of these stages so the next freeze can be localized from log output alone, without needing to reproduce under a debugger.

## Desired Outcome

At the end of DevCycle 021:

- Each stage of the automated fetch/extract/beat pipeline logs a clear, timestamped console line on entry (and on exit/result where useful), including request id and text length where relevant.
- Logging covers both the automatic polling path (`extract_response.js` → `responseComplete` → `handleResponseComplete`) and the manual `Fetch` button path (`fetchLatestResponse` → `handleManualFetch`).
- Logging covers the beat-processing path: `LeftPanePanel.onResponseComplete`, `BeatParser.parse`, `CurrentBeatModel.update`, and the UI updates in `OutputPanel.setResponse` / `CanonPanel.appendEntry`.
- Logging makes it possible to tell, from console output alone, whether a freeze happened before a CEF callback returned, during `BeatParser`/`CurrentBeatModel` work, or during a Swing/EDT update.
- No behavior change — this cycle is purely additive logging, not a fix for the freeze itself (the root cause may be addressed in a follow-up cycle once logs identify it).

---

## Tasks

### Phase 1: Log the JS-side polling/extraction path

**Status:** Work Complete

- [x] Add `console.log`/`console.error` lines in `extract_response.js` around each poll iteration (or at a reduced frequency to avoid log spam), and at completion/timeout/error.
- [x] Add similar logging in `fetch_response.js` for the manual fetch path.
- [ ] Confirm these logs surface in the CEF/JCEF DevTools console (already reachable via the existing `DevTools` button in `AppFrame`). *(requires manual run — see Phase 4)*

**Technical Notes:**
Likely files: `src/main/resources/js/extract_response.js`, `src/main/resources/js/fetch_response.js` (exact resource paths per `ChatGptBridge.loadScript`). Keep per-poll logging lightweight (e.g., only log every Nth poll or state transitions) since `RESPONSE_POLL_INTERVAL_MS` is 500ms and polls can run for up to `RESPONSE_TIMEOUT_MS` (180s).

### Phase 2: Log the Java-side bridge callback path

**Status:** Work Complete

- [x] Add log lines at the start of `handleResponseComplete`, `handleManualFetch`, `handleSendResult`, and `handleInjectResult` in `ChatGptBridge`, including `requestId` and whether the message was active or dropped as stale.
- [x] Add a log line immediately before and after each `appState.transition(...)` call in this class.
- [x] Add a log line in `executeFunction` noting which script/function is being dispatched to the browser.

**Technical Notes:**
File: `src/main/java/com/chatstory/bridge/ChatGptBridge.java`. There is already a `System.out.println` pattern used for stale-message dropping (lines ~297, ~312) — match that style (`"[ChatGptBridge] ..."`) for consistency rather than introducing a new logging framework.

### Phase 3: Log the beat-parsing and UI-update path

**Status:** Work Complete

- [x] Add log lines at entry/exit of `LeftPanePanel.onResponseComplete`, including text length and the parsed beat number (or "no beat parsed").
- [x] Add log lines in `BeatParser.parse` for parse start/result.
- [x] Add log lines in `CurrentBeatModel.update` noting the `ResultKind` returned.
- [x] Add log lines around `outputPanel.setResponse(...)` and `canonPanel.appendEntry(...)` calls, since these touch Swing components and any blocking work here would freeze the EDT.
- [x] Confirm whether these calls already run on the EDT or are invoked from a CEF callback thread; if the latter, note this clearly in the log line (e.g. include `Thread.currentThread().getName()`) since cross-thread Swing access is a plausible freeze cause.

**Technical Notes:**
Files: `src/main/java/com/chatstory/ui/LeftPanePanel.java`, `src/main/java/com/chatstory/beat/BeatParser.java`, `src/main/java/com/chatstory/beat/CurrentBeatModel.java`, `src/main/java/com/chatstory/ui/OutputPanel.java`, `src/main/java/com/chatstory/ui/CanonPanel.java`. Including the thread name in these log lines is the most important diagnostic detail — `DomBridge`/CEF query callbacks may not run on the EDT, and direct Swing mutation off the EDT is a known class of Swing freeze/deadlock.

**Finding while instrumenting:** `OutputPanel.setResponse` calls `onBeatRecorded.accept(currentText)` *before* its `UiThread.run(...)` block, and that callback chain leads straight into `CanonPanel.appendEntry`, which mutates `textArea` directly with no `UiThread`/`SwingUtilities.invokeLater` guard. If `setResponse` is ever invoked from a non-EDT thread (e.g. directly from a CEF query callback rather than after being marshalled onto the EDT), this is a direct cross-thread Swing mutation — a plausible freeze/deadlock source. The new `appendEntry` log line reports `onEdt=...` specifically to confirm or rule this out from real logs. No fix was made this cycle; this is left as the leading hypothesis for a follow-up DevCycle.

### Phase 4: Verification

**Status:** Planning

- [ ] Manually trigger a normal send/response cycle and confirm the new log lines appear in order in the console/DevTools output.
- [ ] Manually trigger the `Fetch` button path and confirm its logs appear.
- [ ] If a freeze can be reproduced during this cycle, capture the resulting log output and record the last logged stage in this document's Completion Summary.

**Technical Notes:**
This phase does not require fixing the freeze — only confirming the logging is in place and useful. If a freeze is captured, file a follow-up DevCycle to address the root cause it points to.

---

## Open Questions

1. **Should logging use `System.out.println`/`console.log`, or introduce a real logging framework (e.g. SLF4J)?**
   Recommendation: Stick with `System.out.println` / `console.log` for this cycle, matching existing project conventions. A logging framework is a larger change better suited to its own cycle if warranted.

2. **Should JS poll-iteration logging be throttled to avoid console spam during the up-to-180s response wait?**
   Recommendation: Yes — log state transitions and every few seconds rather than every 500ms poll tick.

---

## Notes and Risks

- This cycle is diagnostic only; it does not change the freeze behavior itself. The user should expect a follow-up DevCycle once logs reveal the root cause.
- Risk: if the freeze is a true deadlock (e.g. EDT blocked waiting on a CEF thread that is itself blocked waiting on the EDT), logging may stop appearing entirely at the point of deadlock — that absence of further logs is itself diagnostic information and should be treated as such.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-06-17
**Phases Completed:** Phases 1–3 (logging implementation). Phase 4 (manual verification of a live freeze) is pending user follow-up since it requires reproducing the freeze interactively.
**Work Deferred:** Actually fixing the freeze is deferred to a follow-up DevCycle once log output from a real freeze is captured.

**Accomplishments:**
- Added throttled `console.log`/`console.error` instrumentation to `extract_response.js` (poll start, periodic progress, completion, timeout, errors) and `fetch_response.js` (start/complete/error).
- Added `[ChatGptBridge]`-prefixed logging to `handleResponseComplete`, `handleManualFetch`, `handleSendResult`, `handleInjectResult`, `handleErrorResult`, `startPrompt`, `fetchLatestResponse`, and `executeFunction`, plus a new `transition(...)` wrapper that logs before/after every `appState.transition(...)` call.
- Added entry/exit logging through the beat pipeline: `LeftPanePanel.onResponseComplete`, `BeatParser.parse`, `CurrentBeatModel.update`, `OutputPanel.setResponse`, and `CanonPanel.appendEntry`, including thread name and (for `appendEntry`) explicit EDT-or-not status.
- Identified a likely freeze culprit while instrumenting: `CanonPanel.appendEntry` mutates Swing components with no EDT guard, and it can be reached from `OutputPanel.setResponse` before that method's own `UiThread.run(...)` block runs. Documented as the leading hypothesis for the next cycle rather than fixed here, per DC21's diagnostic-only scope.

**Metrics:**
- Files modified: 8 (`extract_response.js`, `fetch_response.js`, `ChatGptBridge.java`, `LeftPanePanel.java`, `BeatParser.java`, `CurrentBeatModel.java`, `OutputPanel.java`, `CanonPanel.java`)
- `./gradlew.bat compileJava` succeeds with no warnings beyond pre-existing native-access notices.
- `./gradlew.bat test --tests "com.chatstory.*Beat*" --tests "com.chatstory.*ChatGptBridge*"` passes.

**Lessons / Notes:**
No live freeze was reproduced during this cycle, so the logging is unverified against a real hang — only confirmed to compile, pass existing tests, and follow the intended call paths by code inspection. The most actionable lead from this pass is the `CanonPanel.appendEntry` EDT-safety gap; the next time a freeze occurs, check whether `onEdt=false` appears in the `[CanonPanel] appendEntry enter` log line.
