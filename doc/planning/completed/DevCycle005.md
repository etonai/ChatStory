# DevCycle 005: Inline Scene Input Parser

**Status:** Verified
**Start Date:** 2026-05-28
**Target Completion:** 2026-07-16
**Focus:** Parse the native input box into ordered player-character dialogue and direction segments, then send an unambiguous structured prompt to ChatGPT.

---

## Goal

Build the first story-input parser while keeping the UI and send flow simple. DC5 keeps a single native input box and interprets text outside angle brackets as spoken player-character dialogue, while text inside angle brackets becomes direction/action/instruction. After parser preview validation, DC5 will also use the parsed segments to send an unambiguous `SCENE_INPUT_SEQUENCE` prompt to ChatGPT.

## Desired Outcome

At the end of DevCycle 005:

- The app has a tested parser for inline scene input.
- Text outside `<...>` parses as `PLAYER_CHARACTER_SAYS`.
- Text inside `<...>` parses as `DIRECTION`.
- Parsed parts preserve the user's original order.
- Surrounding whitespace is trimmed from parsed parts.
- Empty parts are ignored.
- A missing closing `>` is auto-closed at the end of input.
- A stray closing `>` without an opening `<` is treated as normal dialogue text.
- Parser output can be inspected in a simple native preview or test-facing surface.
- The right pane uses tabs: a blank default `MAIN` tab and a second parsed-input tab for debugging.
- Pressing Enter in the native input sends the current text.
- Pressing Ctrl+Enter inserts a newline in the native input.
- Pressing Ctrl+Shift+Enter sends the entire current input as `DIRECTION`, even if it contains no angle brackets.
- ChatGPT receives a structured prompt built from parsed segments rather than the raw input text.
- ChatGPT receives a compact structured prompt without a repeated explanatory rules block.
- Existing DC4 send, response extraction, output panel, and focus behavior remain intact.

---

## Explicit Non-Goals

DevCycle 005 does not include:

- adding multiple input boxes
- adding a full structured response format
- parsing assistant responses
- story continuity tracking
- character profile management
- correction workflows
- persistence or session archive
- escaping literal angle brackets unless needed by implementation feedback
- broader input editor features beyond the DC5 keybinding changes
- story-memory or continuity prompt context beyond the current parsed input

DC5 remains parser-first, but it now includes the first prompt-construction step that sends the parsed sequence to ChatGPT.

---

## Parser Contract

### Segment Types

- `PLAYER_CHARACTER_SAYS`: literal spoken dialogue from the player's character inside the scene.
- `DIRECTION`: action, stage direction, tone guidance, scene instruction, or model-facing instruction.

### Rules

- Text outside angle brackets becomes `PLAYER_CHARACTER_SAYS`.
- Text inside angle brackets becomes `DIRECTION`.
- Preserve segment order exactly.
- Trim leading and trailing whitespace for each segment.
- Ignore empty segments after trimming.
- If `<` appears without a later `>`, treat the rest of input as a `DIRECTION`.
- If `>` appears without a matching `<`, keep it as ordinary dialogue text.
- Adjacent segments of the same type may be kept separate for order fidelity in DC5; merging can be considered later if useful.

### Examples

Input:

```text
Hi <I walk over to the bar. Jane looks upset.> How are you? <Jane looks up at me
```

Parsed output:

```text
1. PLAYER_CHARACTER_SAYS: "Hi"
2. DIRECTION: "I walk over to the bar. Jane looks upset."
3. PLAYER_CHARACTER_SAYS: "How are you?"
4. DIRECTION: "Jane looks up at me"
```

Input:

```text
What is the problem?
```

Parsed output:

```text
1. PLAYER_CHARACTER_SAYS: "What is the problem?"
```

Input:

```text
Hi > there
```

Parsed output:

```text
1. PLAYER_CHARACTER_SAYS: "Hi > there"
```

---

## Structured Prompt Contract

The prompt sent to ChatGPT should be generated from the parsed segments, not from the raw input text. Keep the prompt compact: send the structured sequence without a repeated explanatory rules block.

Prompt structure:

```text
SCENE_INPUT_SEQUENCE:
1. PLAYER_CHARACTER_SAYS: "Hi"
2. DIRECTION: "I walk over to the bar. Jane looks upset."
3. PLAYER_CHARACTER_SAYS: "How are you?"
```

DC5 may refine the exact wording during implementation, but should avoid verbose instructions unless manual testing shows they are needed.

---

## Tasks

### Phase 1: Parser Model

**Status:** Work Complete

- [x] Add a small parser package or class for scene input parsing.
- [x] Define a segment type enum with `PLAYER_CHARACTER_SAYS` and `DIRECTION`.
- [x] Define an immutable parsed segment value object or record.
- [x] Define parser return shape as an ordered list of parsed segments.
- [x] Keep parser code independent of Swing and JCEF.

**Technical Notes:**

Prefer a pure Java parser that can be unit-tested without app startup. This should probably live under `com.chatstory.input` or another similarly narrow package.

### Phase 2: Parser Behavior

**Status:** Work Complete

- [x] Parse all-dialogue input.
- [x] Parse all-direction input.
- [x] Parse mixed dialogue/direction input.
- [x] Preserve ordering across repeated alternating segments.
- [x] Trim surrounding whitespace.
- [x] Ignore empty parsed segments.
- [x] Auto-close an unclosed `<...` direction segment at end of input.
- [x] Treat stray `>` as dialogue text.
- [x] Decide and document behavior for nested `<` inside a direction segment.

**Technical Notes:**

The parser should be forgiving enough for fast writing. Missing `>` should not block the user. Stray `>` should not throw an error.

Nested `<` inside a direction segment is treated as literal direction text. The next `>` closes the direction segment. Literal angle-bracket escaping remains deferred.

### Phase 3: Test Coverage

**Status:** Work Complete

- [x] Add unit tests for all-dialogue input.
- [x] Add unit tests for all-direction input.
- [x] Add unit tests for mixed ordered input.
- [x] Add unit tests for missing closing `>`.
- [x] Add unit tests for stray closing `>`.
- [x] Add unit tests for whitespace trimming and empty segment suppression.
- [x] Add unit tests for adjacent or repeated angle-bracket segments.
- [x] Run `gradlew.bat clean test`.

**Technical Notes:**

Tests should lock down the parser contract before DC6 uses the parsed output to build prompts.

### Phase 4: Preview or Inspection Surface

**Status:** Work Complete

- [x] Show parsed output in a simple native preview surface, preferably the right-side UI testing panel.
- [x] Put the right-side surface behind tabs.
- [x] Make `MAIN` the default selected tab and leave it blank for now.
- [x] Add a second tab for parsed input.
- [x] Keep the existing single input box unchanged.
- [x] Update preview as the input changes if trivial; otherwise update on send or via a simple button.
- [x] Make preview clearly distinguish `PLAYER_CHARACTER_SAYS` from `DIRECTION`.
- [x] Do not block send on parser output in DC5.

**Technical Notes:**

The right-side empty panel from DC4 exists for UI testing and is a good place to inspect parser output without changing the primary workflow. The default tab should be `MAIN` and blank for now; parsed input should live in a second tab for debugging.

### Phase 5: Input Keybindings

**Status:** Work Complete

- [x] Change native input keybindings so Enter sends the current input.
- [x] Change native input keybindings so Ctrl+Enter inserts a newline.
- [x] Add Ctrl+Shift+Enter to send the entire input as `DIRECTION`.
- [x] Ensure Ctrl+Shift+Enter works even when the input contains no `<...>` markers.
- [x] Decide how the direction-only override is represented in parser preview or send metadata.
- [x] Preserve button-based Send behavior.
- [x] Preserve Send disabled behavior for empty input and non-ready app states.
- [x] Add unit coverage if the keybinding behavior can be tested cleanly without brittle Swing tests.
- [x] Manually validate Enter, Ctrl+Enter, and Ctrl+Shift+Enter behavior.

**Technical Notes:**

The current behavior uses Ctrl+Enter to send. DC5 should invert this: Enter sends, Ctrl+Enter inserts a newline. Ctrl+Shift+Enter should send the whole input as `DIRECTION`, regardless of angle brackets. This is a send-time override, not a change to the default parser contract. These changes should be implemented in the native `InputPanel` only and should not affect browser focus handling or response extraction.

The direction-only override is represented by updating the parser preview as a single `DIRECTION` segment immediately before send. The prompt text sent to ChatGPT now uses the structured prompt builder added in DC5.

### Phase 6: Structured Prompt Construction

**Status:** Work Complete

- [x] Add a pure Java prompt builder that consumes parsed segments.
- [x] Use compact labels for `PLAYER_CHARACTER_SAYS` and `DIRECTION`.
- [x] Remove repeated explanatory rules from the sent prompt.
- [x] Keep prompt semantics visible through segment labels.
- [x] Preserve parsed segment order in a numbered `SCENE_INPUT_SEQUENCE`.
- [x] Ensure Ctrl+Shift+Enter direction-only override feeds the prompt builder as one `DIRECTION` segment.
- [x] Add unit tests for prompt construction.
- [x] Wire `InputPanel` sends to send the structured prompt to ChatGPT.
- [x] Keep the parser preview visible for debugging.

**Technical Notes:**

Prompt building is handled by a pure Java `ScenePromptBuilder`. The preview continues showing parsed segments, while the actual sent text is now a compact structured prompt.

### Phase 7: Documentation and Manual Validation

**Status:** Work Complete

- [x] Update this DevCycle document with final parser decisions.
- [x] Manually validate the parser preview with representative story input.
- [x] Manually validate the input keybinding change.
- [x] Manually validate that ChatGPT receives and responds to the structured prompt.
- [x] Manually validate that `PLAYER_CHARACTER_SAYS: "What is the problem?"` is treated as in-scene dialogue, not a direct user question.
- [x] Confirm DC4 send and response extraction still work after parser changes.
- [x] Record any deferred parser questions for DC6.

**Technical Notes:**

Manual validation should focus on confidence that the parser matches the writing convention and that ChatGPT receives the structured prompt. Broader prompt quality tuning belongs in a later cycle.

Parser and structured prompt implementation are complete, automated tests pass, and the user approved DC5 verification after manual validation in the running app.

---

## Manual Validation Plan

### Test 1: Dialogue Only

Input:

```text
What is the problem?
```

Pass:

- One segment appears as `PLAYER_CHARACTER_SAYS`.
- The text remains `What is the problem?`.

### Test 2: Direction Only

Input:

```text
<I walk over to the bar. Jane looks upset.>
```

Pass:

- One segment appears as `DIRECTION`.
- The outer angle brackets are not included in the parsed text.

### Test 3: Mixed Ordered Input

Input:

```text
Hi <I walk over to the bar. Jane looks upset.> How are you? <Jane looks up at me>
```

Pass:

- Four segments appear in the original order.
- Dialogue and direction are correctly distinguished.

### Test 4: Missing Closing Bracket

Input:

```text
Hi <I walk over to the bar
```

Pass:

- `Hi` appears as `PLAYER_CHARACTER_SAYS`.
- `I walk over to the bar` appears as `DIRECTION`.
- No validation error is required.

### Test 5: Stray Closing Bracket

Input:

```text
Hi > there
```

Pass:

- One `PLAYER_CHARACTER_SAYS` segment appears with the text `Hi > there`.

### Test 6: DC4 Regression Check

Steps:

1. Send a simple prompt through the native input.
2. Wait for the assistant response.

Pass:

- Native send still works.
- The app still enters `WaitingForResponse`.
- The latest assistant response still appears in `OutputPanel`.

### Test 7: Enter Sends

Steps:

1. Type a short input into the native input window.
2. Press Enter.

Pass:

- The input is sent.
- The input clears after prompt submission.
- The app enters the normal DC4 response-waiting flow.

### Test 8: Ctrl+Enter Inserts Newline

Steps:

1. Type a line into the native input window.
2. Press Ctrl+Enter.
3. Type a second line.

Pass:

- Ctrl+Enter inserts a newline inside the input window.
- The input is not sent until Enter or the Send button is used.

### Test 9: Ctrl+Shift+Enter Sends As Direction

Steps:

1. Type plain text with no angle brackets into the native input window.
2. Press Ctrl+Shift+Enter.

Pass:

- The input is sent.
- For parser/preview purposes, the entire input is treated as `DIRECTION`.
- The input clears after prompt submission.

Example input:

```text
Jane looks upset and avoids eye contact.
```

Expected parsed/send interpretation:

```text
1. DIRECTION: "Jane looks upset and avoids eye contact."
```

### Test 10: Structured Prompt Is Sent

Input:

```text
Hi <I walk over to the bar. Jane looks upset.> How are you?
```

Pass:

- ChatGPT receives a prompt containing `SCENE_INPUT_SEQUENCE`.
- The sequence contains `PLAYER_CHARACTER_SAYS` and `DIRECTION` items in the correct order.
- The raw angle-bracket shorthand is not the only thing sent to ChatGPT.

### Test 11: Question Dialogue Is Not Treated As User Question

Input:

```text
What is the problem?
```

Pass:

- ChatGPT treats `What is the problem?` as dialogue spoken by the player's character.
- ChatGPT does not answer by explaining the story problem directly to the human user.

### Test 12: Right Pane Tabs

Steps:

1. Launch the app.
2. Inspect the right-side pane.
3. Switch from `MAIN` to the parsed-input tab.

Pass:

- `MAIN` is the default selected tab.
- `MAIN` is blank for now.
- Parsed input is available on the second tab.

---

## Success Criteria

DevCycle 005 is `Work Complete` when:

- The parser contract is implemented in pure Java.
- Parser unit tests cover dialogue, direction, mixed order, missing `>`, stray `>`, trimming, and empty segments.
- Parser output can be inspected from the native app.
- The parsed-input preview is available on a non-default right-pane tab.
- Enter sends the current native input.
- Ctrl+Enter inserts a newline in the native input.
- Ctrl+Shift+Enter sends the entire current input as `DIRECTION`.
- ChatGPT receives a structured prompt generated from parsed segments.
- Dialogue questions should be treated as in-scene speech, not direct questions to ChatGPT.
- Existing DC4 prompt sending and response extraction still work.
- `gradlew.bat clean test` passes.
- The DevCycle document records any parser behavior decisions that were clarified during implementation.

Only the user may approve `Verified`.

---

## Notes and Risks

- Angle brackets may eventually need escaping for literal `<` or `>` in dialogue, but DC5 should avoid over-designing that unless it becomes painful immediately.
- Nested `<` inside direction text needs a documented behavior before parser tests are complete.
- Parser preview should not make the UI feel like a new multi-window workflow.
- Enter-to-send is faster but easier to trigger accidentally; Ctrl+Enter provides the explicit multiline path.
- Ctrl+Shift+Enter creates a direction-only override path; make sure this remains visibly understandable in the preview or validation flow.
- Prompt construction should improve clarity without adding verbose explanatory text to every send.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-05-28
**Phases Completed:** All planned DC5 phases are complete.
**Work Deferred:** Broader prompt-quality tuning and continuity context are deferred to a future cycle.
**Verified Date:** 2026-05-28
**Verification:** User explicitly approved marking DC5 verified after validating parsing, compact structured prompt sending, right-pane tabs, and input behavior.

**Accomplishments:**
- Added pure Java scene input parser.
- Added parser segment type and immutable segment record.
- Added parser preview in the right-side panel.
- Added right-pane tabs with blank `MAIN` tab and parsed-input debug tab.
- Added Enter/Ctrl+Enter/Ctrl+Shift+Enter input keybindings.
- Added parser unit tests.
- Added structured prompt construction from parsed segments.

**Metrics:**
- Files modified: 10
- Tests passing: `gradlew.bat clean test`

**Lessons / Notes:**
DC5 began as parser-only, then expanded after parser preview validation to include the first structured prompt sent to ChatGPT. Initial structured prompts included an explanatory rules block; the user determined that was unnecessary, so the prompt was reduced to the compact `SCENE_INPUT_SEQUENCE`.
