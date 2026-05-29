# DevCycle 013: Automatic Canon Beat Tracking

**Status:** Planning
**Start Date:** 2026-05-29
**Target Completion:** 2026-08-06
**Focus:** Automatically track story beats from assistant responses and move completed beats into Canon when a newer beat arrives.

---

## Goal

DC13 adds automatic canon tracking around the story beat workflow. Instead of relying only on the user clicking **Add to Canon**, the app will recognize assistant responses that declare a beat number, treat the response panel as the current beat, and automatically append the previous beat to Canon when a newer beat arrives. This builds on the existing Canon tab, editable Canon text area, and `00_Canon_Temp.md` autosave behavior.

## Desired Outcome

At the end of DevCycle 013:

- Assistant responses can be recognized as beats when one of the first two response lines contains `<Beat #>`.
- The Response tab title/header changes from generic assistant response wording to `Current Beat #` when a beat is active.
- If no `<Beat #>` is found in the first two lines, the response is treated as non-beat output and does not change the current beat.
- If a new response has the same beat number as the current beat, it replaces/overwrites the current beat instead of adding to Canon.
- If a new response has a different beat number, the previous current beat is automatically appended to Canon before the new beat becomes current.
- Automatic Canon updates use the same Canon tab and temp autosave path as manual Add to Canon.
- Manual Add to Canon remains available.
- Saving Canon warns/prompts when there is an unsaved current beat that has not yet been written to Canon.
- Existing Story Mode, Unassisted Mode, correction actions, Redo, context staging, and manual Canon workflows continue to work.

---

## Explicit Non-Goals

DevCycle 013 does not include:

- summarizing Canon documents
- Canon highlight sections
- character header extraction
- relationship-value tracking
- editing intermediate relationship values
- changing the context-file workflow
- designing a full story metadata schema
- requiring every non-story ChatGPT response to be a beat
- automatic upload of Canon or context files to ChatGPT

DC13 focuses only on detecting beat-numbered responses and maintaining the current beat / Canon handoff.

---

## Beat Contract

### Beat Detection

- Inspect only the first two lines of an assistant response.
- A beat marker has the form `<Beat #>`.
- `#` is a positive integer.
- Whitespace inside the marker may be tolerated, for example `<Beat 12>`.
- If no marker is found in the first two lines, the response is not a beat.

### Current Beat Behavior

- The response panel represents the current beat when a beat marker is detected.
- The UI should show `Current Beat #` where `#` is the current beat number.
- The full assistant response remains editable in the response text area.
- If the response has the same beat number as the current beat, replace the current beat text and do not append anything to Canon.
- If the response has a different beat number, append the previous current beat to Canon, then make the new response the current beat.

### Canon Handoff Behavior

- Automatic append should reuse the Canon append pathway so the Canon tab and `00_Canon_Temp.md` stay synchronized.
- A beat should not be appended twice because of repeated UI refreshes or same-beat rewrites.
- Manual Add to Canon remains possible for non-beat text or user-edited current beat text.
- If the user saves Canon while the current beat has not been appended to Canon, ask whether to append it before saving.

---

## Tasks

### Phase 1: Beat Parsing Model

**Status:** Planning

- [ ] Add a pure Java beat parser/model.
- [ ] Parse `<Beat #>` markers from the first two lines only.
- [ ] Return no beat when the marker is absent.
- [ ] Return no beat for invalid markers.
- [ ] Add unit tests for first-line marker, second-line marker, missing marker, invalid marker, and marker after line two.

**Technical Notes:**

Keep beat parsing independent from Swing. A small `BeatParser` returning an optional beat number is enough unless implementation reveals more state is needed.

### Phase 2: Current Beat State

**Status:** Planning

- [ ] Add a current beat model that tracks beat number, text, and whether it has been appended to Canon.
- [ ] Support setting/replacing the current beat with the same beat number.
- [ ] Support detecting when a new beat number requires the old beat to be appended first.
- [ ] Avoid duplicate append of the same current beat.
- [ ] Add unit tests for same-beat replacement, new-beat rollover, and non-beat response handling.

**Technical Notes:**

This should be testable without UI. The model can return an append-needed event/string when a new beat supersedes the current beat.

### Phase 3: Response Panel Integration

**Status:** Planning

- [ ] Update response handling so beat-numbered responses are routed through current beat tracking.
- [ ] Change the response header/title to `Current Beat #` when a beat is active.
- [ ] Keep non-beat responses visible without changing current beat state.
- [ ] Preserve editable response text behavior.
- [ ] Preserve correction menu behavior.
- [ ] Preserve Clear, Copy, and Add to Canon controls.
- [ ] Ensure same-beat rewrites update the current beat display without appending to Canon.

**Technical Notes:**

`OutputPanel.setResponse(...)` currently owns the response text and Add to Canon button state. DC13 may need a richer method such as `setResponse(text, beatNumber)` or a small presentation model.

### Phase 4: Automatic Canon Append

**Status:** Planning

- [ ] When a new beat number arrives, append the previous current beat to Canon.
- [ ] Reuse the same append path that updates `CanonPanel`.
- [ ] Ensure `00_Canon_Temp.md` is updated after automatic append.
- [ ] Record automatic append behavior in the DC13 document after implementation.
- [ ] Keep manual Add to Canon working for user-controlled cases.

**Technical Notes:**

The existing `CanonPanel.appendEntry(...)` already auto-saves temp content. Prefer calling the existing Canon callback rather than duplicating file-writing logic.

### Phase 5: Save Canon Unsaved Beat Prompt

**Status:** Planning

- [ ] Detect whether the current beat has not been appended to Canon.
- [ ] When the user saves Canon, ask whether to append the current beat first.
- [ ] If the user agrees, append the current beat and then save.
- [ ] If the user declines, save Canon without the current beat.
- [ ] If there is no active unsaved beat, save normally.

**Technical Notes:**

This likely belongs near `CanonPanel.saveToFile(...)`, but the current beat state may live outside `CanonPanel`. Keep responsibilities clear: CanonPanel owns saving canon text; current beat model owns whether a beat is pending.

### Phase 6: Manual Validation and Regression

**Status:** Planning

- [ ] Run `gradlew.bat clean test`.
- [ ] Manually validate first beat detection.
- [ ] Manually validate same-beat rewrite behavior.
- [ ] Manually validate new-beat automatic append.
- [ ] Manually validate non-beat response behavior.
- [ ] Manually validate Canon temp autosave after automatic append.
- [ ] Manually validate save prompt for unsaved current beat.
- [ ] Manually validate manual Add to Canon still works.
- [ ] Manually validate Story Mode and Unassisted Mode still behave correctly.

**Technical Notes:**

Manual validation matters because beat detection is based on live assistant response text and Canon UI behavior.

---

## Manual Validation Plan

### Test 1: First Beat Detection

Assistant response begins:

```text
<Beat 1>
...
```

Pass:

- Response header shows `Current Beat 1`.
- Canon is not automatically updated yet because there was no previous current beat.

### Test 2: Same Beat Rewrite

Steps:

1. Receive `<Beat 1>`.
2. Receive another `<Beat 1>` rewrite.

Pass:

- Response header remains `Current Beat 1`.
- Response text is replaced with the new beat.
- Canon does not gain a duplicate Beat 1 entry.

### Test 3: New Beat Rollover

Steps:

1. Receive `<Beat 1>`.
2. Receive `<Beat 2>`.

Pass:

- Beat 1 is automatically appended to Canon.
- Canon tab updates.
- `00_Canon_Temp.md` updates if a Canon folder is configured.
- Response header shows `Current Beat 2`.

### Test 4: Non-Beat Response

Assistant response does not contain `<Beat #>` in the first two lines.

Pass:

- Response is displayed normally.
- Current beat state is unchanged.
- Canon is not automatically updated.

### Test 5: Save Canon With Unsaved Current Beat

Steps:

1. Receive a current beat that has not rolled over into Canon.
2. Click Save in Canon.

Pass:

- App asks whether to append the current beat first.
- Choosing yes appends and saves.
- Choosing no saves without appending.

### Test 6: Existing Manual Canon Behavior

Steps:

1. Display any response.
2. Click Add to Canon.

Pass:

- Manual Add to Canon still appends text.
- The button still prevents accidental double-add until a new response or clear.

---

## Success Criteria

DevCycle 013 is `Work Complete` when:

- Beat parser and current beat state are implemented and unit-tested.
- Beat markers in the first two response lines are detected.
- Same-beat responses replace the current beat.
- New beat numbers automatically append the prior beat to Canon.
- Automatic append updates Canon UI and temp file behavior.
- Non-beat responses do not affect current beat state.
- Save Canon prompts for an unsaved current beat.
- Manual Add to Canon still works.
- `gradlew.bat clean test` passes.
- Manual validation confirms the automatic beat/canon workflow.

Only the user may approve `Verified`.

---

## Notes and Risks

- The exact beat marker format may need tuning if ChatGPT emits `Day 1 Beat 2` without angle brackets. DC13 should initially require `<Beat #>` as specified.
- User edits to the current beat text should be respected when appending to Canon.
- The boundary between automatic and manual Canon addition must be clear to avoid duplicate canon entries.
- Save prompting should be helpful without becoming annoying.
- Unassisted Mode should not trigger automatic beat tracking.

---

## Open Questions

These questions arose during planning and should be resolved before or during implementation.

**Q1: Beat marker on a line with other content**
If ChatGPT emits `<Beat 3> Day 1 — Arrival` (marker and prose on the same line), does that count as a valid beat marker, or must `<Beat #>` occupy the entire line? Clarifying this prevents edge-case misses or false positives.

**Q2: Is Beat 0 a valid beat?**
The ideas file shows "Day 1 Beat 0" as an example. Must the beat number be a positive integer (≥ 1), or is zero allowed? The current contract says "positive integer," which would exclude 0 — confirm this is intentional.

**Q3: Beat number sequencing**
Should the system validate that beat numbers are sequential (e.g., warn if `<Beat 3>` arrives after `<Beat 1>` with no `<Beat 2>`)? Or should it silently accept any order and just do the rollover?

**Q4: Current beat state on app restart**
Is the current beat session-only (lost on close), or should it be persisted to disk so that reopening the app restores the last active beat?

**Q5: Canon separator format on automatic append**
When a beat is automatically appended to Canon, should the system insert any separator between entries (blank line, `---`, etc.), or append the raw beat text with no decoration?

**Q6: Relationship values and meta-lines**
The ideas file shows responses that include metadata lines like `Drake -> Morgan STP 5.2` mixed with story prose. Should the automatic Canon append include that metadata verbatim, or is filtering it out a future concern outside DC13 scope?

**Q7: Reset behavior and current beat**
When the user clicks Reset (Ctrl-X), should the current beat state be cleared along with the response panel, or preserved until a new beat or explicit Canon action replaces it?

**Q8: Redo interaction with beat tracking**
If the user clicks Redo and a previous beat response is restored to the response panel, should beat tracking fire again (potentially triggering another rollover)? Or does Redo bypass beat detection?

**Q9: Unassisted Mode — silent skip or visible indicator?**
The notes say Unassisted Mode should not trigger automatic beat tracking. Should the beat detection be completely skipped (no UI change), or should the response panel show something like "Non-tracking mode" to make the distinction visible?

**Q10: Save Canon → append → temp file order**
When the user answers "yes" to the unsaved-beat prompt during Save Canon, the beat is appended and then Canon is saved. Should `00_Canon_Temp.md` be updated as part of the append step (before the file-save dialog), or only as a side effect of the final save?

---

## Codex Responses to Open Questions

### Q1: Beat Marker on a Line With Other Content

Recommendation: count it as valid if `<Beat #>` appears anywhere in either of the first two lines.

Reasoning: the original requirement says to look for `<Beat #>` in the first two lines, not that the marker must occupy the full line. Accepting something like `<Beat 3> Day 1 - Arrival` is forgiving and matches likely model output. The parser should extract the beat number and leave the response text unchanged.

### Q2: Is Beat 0 a Valid Beat?

Recommendation: allow beat `0`.

Reasoning: the ideas file explicitly mentions `Day 1 Beat 0`. Even if most beats are 1-based, Beat 0 is useful as a setup, prologue, or scene-initialization beat. Update the contract from positive integer to non-negative integer.

### Q3: Beat Number Sequencing

Recommendation: do not enforce sequential numbering in DC13.

Reasoning: automatic Canon tracking should be robust before it is judgmental. If `<Beat 3>` arrives after `<Beat 1>`, the safest DC13 behavior is to roll over Beat 1 into Canon and make Beat 3 current. Warnings for skipped or decreasing beats can be a later quality-of-life feature.

### Q4: Current Beat State on App Restart

Recommendation: keep current beat state session-only for DC13.

Reasoning: Canon temp autosave already protects appended Canon content. Persisting the not-yet-canon current beat adds a recovery/state layer that deserves its own design. DC13 should avoid inventing another persistence file unless manual testing shows restart recovery is immediately painful.

### Q5: Canon Separator Format on Automatic Append

Recommendation: reuse the existing Canon append format exactly.

Reasoning: `CanonPanel.appendEntry(...)` already appends with the same separator used for manual Canon entries and updates the temp file. Automatic append should call that path rather than introduce a second format. Today that means entries are separated with `---` surrounded by blank lines.

### Q6: Relationship Values and Meta-Lines

Recommendation: include metadata lines verbatim in DC13.

Reasoning: filtering relationship values, day/beat headers, character lists, or other metadata requires a response schema we have not designed yet. DC13 should append the full beat response exactly as the user saw or edited it. Filtering or summarizing Canon should remain a future cycle.

### Q7: Reset Behavior and Current Beat

Recommendation: Reset should clear current beat state.

Reasoning: Reset is already a strong workflow boundary. If the response panel is cleared but current beat state remains hidden, the next beat could unexpectedly append stale content to Canon. Clearing current beat state is safer and easier to reason about.

### Q8: Redo Interaction With Beat Tracking

Recommendation: Redo should not itself trigger beat tracking; only the assistant response generated after Redo should be processed.

Reasoning: Redo is a prompt/action, not a response. If the assistant returns the same beat number, same-beat replacement handles it. If it returns a new beat number, rollover handles it. No special Redo bypass is needed as long as beat detection only runs on completed assistant responses.

### Q9: Unassisted Mode - Silent Skip or Visible Indicator?

Recommendation: silently skip automatic beat tracking in Unassisted Mode.

Reasoning: Unassisted Mode is supposed to behave like a plain ChatGPT browser wrapper. Adding a native non-tracking indicator in the response panel would work against that goal. The Configuration tab mode toggle is enough visibility.

### Q10: Save Canon, Append, Temp File Order

Recommendation: if the user chooses to append the current beat before saving, append through the normal Canon path first, including temp autosave, then continue the explicit Save flow.

Reasoning: this preserves a single Canon append pathway and keeps the Canon tab/temp file consistent even if the final save is canceled or fails. If the explicit save succeeds, the existing save behavior can delete/wipe the temp file afterward.

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
