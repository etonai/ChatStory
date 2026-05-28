# DevCycle 006: Story Mode Toggle and Dark Mode

**Status:** Planning
**Start Date:** 2026-05-28
**Target Completion:** 2026-07-23
**Focus:** Add Story Mode vs Unassisted Mode behavior and make the app default to a darker theme with a light-mode option.

---

## Goal

DC6 separates the app's enhanced story workflow from plain ChatGPT browser usage. Story Mode keeps the DC5 parser, structured prompt sending, assistant response tracking, and native output panels active. Unassisted Mode sends raw input directly to ChatGPT and avoids native response tracking so the app behaves more like a simple ChatGPT browser wrapper. DC6 also makes the app less visually harsh by defaulting native UI surfaces to dark mode while allowing a light mode option.

## Desired Outcome

At the end of DevCycle 006:

- The right pane has a `Configuration` tab.
- The app has a visible Story Mode / Unassisted Mode toggle in the `Configuration` tab.
- Story Mode remains the default mode.
- Story Mode keeps the DC5 behavior:
  - parse input
  - show parsed input in the debug tab
  - send compact `SCENE_INPUT_SEQUENCE` prompts
  - track assistant responses
  - display the latest response in the native output panel
- Unassisted Mode sends raw user input directly to ChatGPT.
- Unassisted Mode does not use parser-transformed prompts.
- Unassisted Mode does not update or rely on the native assistant response panel.
- Existing browser visibility and manual ChatGPT interaction remain available in both modes.
- Native app panels default to a dark mode appearance.
- The user can switch between dark and light native UI modes from the `Configuration` tab.
- Theme switching does not break input focus, send behavior, response extraction, or parser preview.

---

## Explicit Non-Goals

DevCycle 006 does not include:

- context file upload
- canon continuity storage
- Add to Canon behavior
- assistant response right-click correction actions
- redo button
- prompt history
- persistence of mode/theme preferences across app restarts unless trivial
- dark mode inside the embedded ChatGPT browser page
- custom full design system

Context, Canon, correction menus, and Redo should be planned in later DevCycles.

---

## Mode Contract

### Story Mode

Story Mode is the enhanced workflow built so far.

In Story Mode:

- The native input is parsed into `PLAYER_CHARACTER_SAYS` and `DIRECTION`.
- The parsed preview remains available in the right-pane parsed-input tab.
- The sent prompt is the compact structured `SCENE_INPUT_SEQUENCE`.
- The app waits for assistant response completion.
- The latest assistant response appears in the native output panel.
- DC4/DC5 behavior should remain intact.

### Unassisted Mode

Unassisted Mode is plain ChatGPT usage through the app.

In Unassisted Mode:

- The native input text is sent raw.
- No `SCENE_INPUT_SEQUENCE` prompt wrapping is applied.
- Angle brackets have no special parsing effect on what is sent.
- The parser preview may remain visible for debugging, but it should not control send behavior.
- The app should not wait for or extract assistant responses into the native output panel.
- After the user message is confirmed, the app should return to a send-enabled state.

---

## Theme Contract

### Dark Mode

Dark mode should be the default native app theme.

It should apply to:

- app frame background
- toolbar
- status label area
- native input panel
- output panel
- right-pane tabs
- parsed input preview
- buttons where practical
- text areas and scroll panes

### Light Mode

Light mode should remain available through a UI control in the right-pane `Configuration` tab near the mode toggle.

Light mode should be readable and roughly match the current Swing look, without needing extensive polish.

---

## Tasks

### Phase 1: App Mode Model

**Status:** Planning

- [ ] Add an app mode model with `STORY` and `UNASSISTED`.
- [ ] Default to `STORY`.
- [ ] Make mode observable by UI components that need it.
- [ ] Keep mode independent from `AppState` unless state-machine changes are needed.
- [ ] Add unit tests for mode defaults or mode holder behavior if a testable model is introduced.

**Technical Notes:**

Prefer a small model separate from `AppState`, because `AppState` currently represents lifecycle/send states rather than product mode. Avoid entangling mode with browser load states.

### Phase 2: Mode Controls

**Status:** Planning

- [ ] Add a `Configuration` tab to the right pane.
- [ ] Add a Story Mode / Unassisted Mode toggle to the `Configuration` tab.
- [ ] Show the current mode clearly.
- [ ] Leave the `MAIN` tab available for future non-configuration UI.
- [ ] Keep the parsed-input tab as a debug tab.
- [ ] Ensure switching modes does not clear the current input unless there is a strong reason.

**Technical Notes:**

Use the existing right-pane tabs from DC5. Keep the control UI simple; this is operational UI, not a landing page. The `MAIN` tab should not become a settings dump; configuration controls belong in the `Configuration` tab.

### Phase 3: Story Mode Send Path

**Status:** Planning

- [ ] Preserve DC5 Story Mode structured prompt sending.
- [ ] Preserve parser preview behavior.
- [ ] Preserve Enter, Ctrl+Enter, and Ctrl+Shift+Enter behavior.
- [ ] Preserve assistant response extraction and output panel updates.
- [ ] Add or update tests around prompt construction if mode changes affect it.

**Technical Notes:**

Story Mode should feel unchanged from verified DC5 behavior.

### Phase 4: Unassisted Mode Send Path

**Status:** Planning

- [ ] Send raw native input text in Unassisted Mode.
- [ ] Do not wrap raw input in `SCENE_INPUT_SEQUENCE`.
- [ ] Do not wait for assistant response extraction in Unassisted Mode.
- [ ] Return to send-enabled state after user message confirmation.
- [ ] Keep the browser visible as the source of truth for ChatGPT's response.
- [ ] Preserve failure recovery for injection/send errors.

**Technical Notes:**

This may require a bridge method or send option that performs DC3-style injection/send without DC4 response extraction. Be careful not to regress Story Mode's response-tracking path.

### Phase 5: Native Theme Model and Controls

**Status:** Planning

- [ ] Add a native theme model or controller with `DARK` and `LIGHT`.
- [ ] Default to `DARK`.
- [ ] Add a Dark / Light control in the right-pane `Configuration` tab.
- [ ] Apply theme changes to existing native panels.
- [ ] Ensure theme switching can happen at runtime without restarting the app.

**Technical Notes:**

Do not try to theme the embedded ChatGPT web page in DC6. Focus on Swing/native UI surfaces.

### Phase 6: Dark Mode Styling

**Status:** Planning

- [ ] Define restrained dark colors for native surfaces.
- [ ] Style text areas, labels, buttons, tabs, split panes, and scroll panes where practical.
- [ ] Keep contrast readable.
- [ ] Avoid a single-hue purple/blue-heavy palette.
- [ ] Ensure disabled buttons and status text remain legible.
- [ ] Keep light mode functional.

**Technical Notes:**

Prefer a small reusable theme application helper over hand-styling each component in unrelated places. Keep this lightweight.

### Phase 7: Tests and Manual Validation

**Status:** Planning

- [ ] Run `gradlew.bat clean test`.
- [ ] Manually validate Story Mode still parses and sends structured prompts.
- [ ] Manually validate Story Mode still extracts responses.
- [ ] Manually validate Unassisted Mode sends raw input.
- [ ] Manually validate Unassisted Mode does not update native response extraction.
- [ ] Manually validate switching between modes.
- [ ] Manually validate dark mode default.
- [ ] Manually validate light mode switching.
- [ ] Manually validate Enter, Ctrl+Enter, and Ctrl+Shift+Enter still work.

**Technical Notes:**

Manual validation matters here because mode behavior depends on live ChatGPT browser behavior.

---

## Manual Validation Plan

### Test 1: Story Mode Default

Steps:

1. Launch the app.
2. Inspect the right-pane tabs.

Pass:

- Story Mode is selected by default.
- A `Configuration` tab is available.
- Parsed Input remains available as a debug tab.

### Test 2: Story Mode Structured Send

Input:

```text
Hi <Jane looks upset.> What is the problem?
```

Pass:

- The browser receives a compact `SCENE_INPUT_SEQUENCE`.
- The parsed-input tab shows ordered `PLAYER_CHARACTER_SAYS` and `DIRECTION` segments.
- The native output panel updates with the assistant response.

### Test 3: Unassisted Raw Send

Steps:

1. Switch to Unassisted Mode.
2. Send:

```text
Hi <this should stay raw>
```

Pass:

- The browser receives the raw text, including angle brackets.
- No `SCENE_INPUT_SEQUENCE` wrapper is sent.
- The native output panel is not updated from response extraction for that send.

### Test 4: Mode Switching

Steps:

1. Send once in Story Mode.
2. Switch to Unassisted Mode and send once.
3. Switch back to Story Mode and send once.

Pass:

- Each send uses the correct mode behavior.
- Send button state recovers after each send.
- No stale response is shown for Unassisted Mode.

### Test 5: Dark Mode Default

Steps:

1. Launch the app.

Pass:

- Native UI panels appear in dark mode by default.
- Text remains readable.
- Buttons and tabs are usable.

### Test 6: Light Mode Switch

Steps:

1. Switch from dark mode to light mode.
2. Switch back to dark mode.

Pass:

- Native UI updates without restart.
- Text remains readable in both modes.
- Browser content is not expected to change.

---

## Success Criteria

DevCycle 006 is `Work Complete` when:

- Story Mode / Unassisted Mode toggle exists in the right-pane `Configuration` tab.
- Story Mode preserves verified DC5 behavior.
- Unassisted Mode sends raw input.
- Unassisted Mode avoids native response extraction/tracking.
- Mode switching works without restart.
- Native app defaults to dark mode.
- Native app can switch to light mode.
- `gradlew.bat clean test` passes.
- Manual validation confirms both mode behavior and theme behavior.

Only the user may approve `Verified`.

---

## Future Cycle Notes

- Context files should likely become their own right-pane tab after the mode/theme controls are stable.
- Canon continuity should likely convert the left pane into tabs with Assistant Response and Canon.
- Correction actions and Redo should wait until story-mode/canon semantics are clearer.
- Persisting theme and mode preferences can be added later if needed.

---

## Notes and Risks

- JCEF focus and send behavior can be delicate; avoid broad changes to the bridge.
- Unassisted Mode should not accidentally disable native send while waiting for a response it is not tracking.
- Dark mode can become visually noisy if over-designed; keep colors restrained and readable.
- Swing Look and Feel limitations may make some native widgets imperfectly themed.
- Story Mode must not regress while adding Unassisted Mode.

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
