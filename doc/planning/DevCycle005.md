# DevCycle 005: Inline Scene Input Parser

**Status:** Planning
**Start Date:** 2026-05-28
**Target Completion:** 2026-07-16
**Focus:** Parse the native input box into ordered player-character dialogue and direction segments.

---

## Goal

Build the first story-input parser while keeping the UI and send flow simple. DC5 keeps a single native input box and interprets text outside angle brackets as spoken player-character dialogue, while text inside angle brackets becomes direction/action/instruction. This creates a clearer foundation for future prompt construction without changing the ChatGPT send contract yet.

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
- Pressing Enter in the native input sends the current text.
- Pressing Ctrl+Enter inserts a newline in the native input.
- Pressing Ctrl+Shift+Enter sends the entire current input as `DIRECTION`, even if it contains no angle brackets.
- Existing DC4 send, response extraction, output panel, and focus behavior remain intact.

---

## Explicit Non-Goals

DevCycle 005 does not include:

- changing the prompt sent to ChatGPT
- adding multiple input boxes
- adding a full structured response format
- parsing assistant responses
- story continuity tracking
- character profile management
- correction workflows
- persistence or session archive
- escaping literal angle brackets unless needed by implementation feedback
- broader input editor features beyond the DC5 keybinding changes

DC5 is parser-first. The structured prompt that uses this parser should be planned after the parser behavior is stable.

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

## Tasks

### Phase 1: Parser Model

**Status:** Planning

- [ ] Add a small parser package or class for scene input parsing.
- [ ] Define a segment type enum with `PLAYER_CHARACTER_SAYS` and `DIRECTION`.
- [ ] Define an immutable parsed segment value object or record.
- [ ] Define parser return shape as an ordered list of parsed segments.
- [ ] Keep parser code independent of Swing and JCEF.

**Technical Notes:**

Prefer a pure Java parser that can be unit-tested without app startup. This should probably live under `com.chatstory.input` or another similarly narrow package.

### Phase 2: Parser Behavior

**Status:** Planning

- [ ] Parse all-dialogue input.
- [ ] Parse all-direction input.
- [ ] Parse mixed dialogue/direction input.
- [ ] Preserve ordering across repeated alternating segments.
- [ ] Trim surrounding whitespace.
- [ ] Ignore empty parsed segments.
- [ ] Auto-close an unclosed `<...` direction segment at end of input.
- [ ] Treat stray `>` as dialogue text.
- [ ] Decide and document behavior for nested `<` inside a direction segment.

**Technical Notes:**

The parser should be forgiving enough for fast writing. Missing `>` should not block the user. Stray `>` should not throw an error.

### Phase 3: Test Coverage

**Status:** Planning

- [ ] Add unit tests for all-dialogue input.
- [ ] Add unit tests for all-direction input.
- [ ] Add unit tests for mixed ordered input.
- [ ] Add unit tests for missing closing `>`.
- [ ] Add unit tests for stray closing `>`.
- [ ] Add unit tests for whitespace trimming and empty segment suppression.
- [ ] Add unit tests for adjacent or repeated angle-bracket segments.
- [ ] Run `gradlew.bat clean test`.

**Technical Notes:**

Tests should lock down the parser contract before DC6 uses the parsed output to build prompts.

### Phase 4: Preview or Inspection Surface

**Status:** Planning

- [ ] Show parsed output in a simple native preview surface, preferably the right-side UI testing panel.
- [ ] Keep the existing single input box unchanged.
- [ ] Update preview as the input changes if trivial; otherwise update on send or via a simple button.
- [ ] Make preview clearly distinguish `PLAYER_CHARACTER_SAYS` from `DIRECTION`.
- [ ] Do not block send on parser output in DC5.

**Technical Notes:**

The right-side empty panel from DC4 exists for UI testing and is a good place to inspect parser output without changing the primary workflow. Keep this plain and reversible.

### Phase 5: Documentation and Manual Validation

**Status:** Planning

- [ ] Change native input keybindings so Enter sends the current input.
- [ ] Change native input keybindings so Ctrl+Enter inserts a newline.
- [ ] Add Ctrl+Shift+Enter to send the entire input as `DIRECTION`.
- [ ] Ensure Ctrl+Shift+Enter works even when the input contains no `<...>` markers.
- [ ] Decide how the direction-only override is represented in parser preview or send metadata.
- [ ] Preserve button-based Send behavior.
- [ ] Preserve Send disabled behavior for empty input and non-ready app states.
- [ ] Add unit coverage if the keybinding behavior can be tested cleanly without brittle Swing tests.
- [ ] Manually validate Enter, Ctrl+Enter, and Ctrl+Shift+Enter behavior.

**Technical Notes:**

The current behavior uses Ctrl+Enter to send. DC5 should invert this: Enter sends, Ctrl+Enter inserts a newline. Ctrl+Shift+Enter should send the whole input as `DIRECTION`, regardless of angle brackets. This is a send-time override, not a change to the default parser contract. These changes should be implemented in the native `InputPanel` only and should not affect browser focus handling or response extraction.

### Phase 6: Documentation and Manual Validation

**Status:** Planning

- [ ] Update this DevCycle document with final parser decisions.
- [ ] Manually validate the parser preview with representative story input.
- [ ] Manually validate the input keybinding change.
- [ ] Confirm DC4 send and response extraction still work after parser changes.
- [ ] Record any deferred parser questions for DC6.

**Technical Notes:**

Manual validation should focus on confidence that the parser matches the writing convention, not on prompt quality. Prompt quality belongs in a later cycle.

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

---

## Success Criteria

DevCycle 005 is `Work Complete` when:

- The parser contract is implemented in pure Java.
- Parser unit tests cover dialogue, direction, mixed order, missing `>`, stray `>`, trimming, and empty segments.
- Parser output can be inspected from the native app.
- Enter sends the current native input.
- Ctrl+Enter inserts a newline in the native input.
- Ctrl+Shift+Enter sends the entire current input as `DIRECTION`.
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
- DC5 should not start prompt-format work, even though `PLAYER_CHARACTER_SAYS` is intentionally chosen for future prompt clarity.

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
