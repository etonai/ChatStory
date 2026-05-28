# Codex Recommendations on ClaudePlan02

## Purpose

This document compares `ClaudePlan02.md` and `CodexPlan02.md` and recommends which plan should be used as the basis for active DevCycle planning.

## Recommendation

Use `ClaudePlan02.md` as the base for the next active DevCycle plan.

`ClaudePlan02.md` is better suited for implementation because it is more operational. It includes concrete task lists, success criteria, file structure, failure codes, state handling, unit-test targets, and manual validation steps. It is closer to something that can be converted directly into `doc/planning/DevCycle001.md`.

`CodexPlan02.md` is useful as a strategic standalone plan. It explains the architecture and reasoning clearly, and it is slightly less prescriptive. That makes it easier to read, but less immediately actionable.

## Why ClaudePlan02 Is Stronger

### 1. It Is More DevCycle-Ready

ClaudePlan02 already breaks work into implementation-sized cycles:

- Phase 0 feasibility spike.
- DevCycle 001 browser scaffold.
- DevCycle 002 injection and native input.
- DevCycle 003 completion and extraction.
- DevCycle 004 hardening.

This matches the project's planning process well and makes the next step straightforward.

### 2. It Gives More Concrete Implementation Detail

ClaudePlan02 defines:

- Proposed package/file structure.
- Specific Java classes.
- Resource file locations.
- JCEF setup responsibilities.
- Bridge message envelope.
- Error codes.
- State machine transitions.
- Manual and unit-test expectations.

Those details will reduce ambiguity when implementation begins.

### 3. It Treats JCEF Setup As A First-Class Risk

Both plans identify JCEF as the core dependency, but ClaudePlan02 is more explicit about:

- Researching the exact artifact/version.
- Native DLL layout.
- Java 21 and Windows x64 compatibility.
- Future `jpackage` compatibility.
- Recording setup findings in `BUILDING.md`.

That is important because JCEF setup is likely the first major blocker.

### 4. It Includes Better Operational Debugging

ClaudePlan02 calls for:

- A DevTools button.
- A debug status bar.
- Bridge operation logging.
- Request IDs in logs.
- Specific error messages.

These are valuable because the MVP's hardest bugs will be browser, DOM, selector, and async-state issues.

### 5. It Has Stronger Validation Coverage

ClaudePlan02 includes:

- Manual tests for browser loading, login persistence, prompt send, response extraction, repeated sends, and failure recovery.
- Unit-test targets for prompt encoding, bridge message parsing, app state transitions, and resource loading.

This gives the MVP clearer proof points.

## Changes To Borrow From CodexPlan02

ClaudePlan02 should remain the base, but a few CodexPlan02 ideas are worth merging.

### 1. Keep The Strategic Goal Statement

CodexPlan02 has a clean standalone framing of the MVP goal. ClaudePlan02 already has a good goal section, but it could borrow the emphasis that the MVP exists to prove the bridge before storytelling workflows.

### 2. Clarify Phase 0 Non-Goals

ClaudePlan02 has good Phase 0 non-goals. Keep them strict:

- No native prompt input.
- No prompt injection.
- No response extraction.
- No final package structure beyond what the spike requires.
- No quality-of-life UI.

This prevents Phase 0 from expanding into the whole app.

### 3. Tune Completion Detection Defaults

ClaudePlan02 defaults the text stability window to `1500ms`.

That is safe, but it may feel slow. Recommended adjustment:

- Start with configurable constants.
- Use `1500ms` if the priority is avoiding partial extraction.
- Consider `800ms` after testing if responses feel delayed.

The important point is not the exact number; it is that the value should live in one place and be easy to tune.

### 4. Preserve CodexPlan02's Simpler DevCycle Mapping

ClaudePlan02 has both Phase 0 and DevCycle 001, which is useful but could be confusing.

Recommendation:

- Treat Phase 0 as the scope of `DevCycle001.md`.
- After Phase 0 succeeds, continue with the later DevCycles.

This aligns with `DevelopmentProcess.md`, where active work should live in DevCycle files under `doc/planning/`.

### 5. Keep The Verified Status Rule Explicit

CodexPlan02 explicitly notes that MVP work should stop at `Work Complete` until the user approves `Verified`.

That rule should be included in the active DevCycle documents.

## Suggested Next Step

Create `doc/planning/DevCycle001.md` using `ClaudePlan02.md` as the source, with this scope:

```text
DevCycle 001: JCEF Browser Viability

Goal:
Prove JCEF can launch, render ChatGPT, open DevTools, support the intended login flow, preserve session state, and document the repeatable Windows setup.
```

Do not include prompt injection, native input, or response extraction in DevCycle 001.

## Bottom Line

`ClaudePlan02.md` is the better implementation plan. It should be the base for DevCycle work.

`CodexPlan02.md` should be treated as supporting context for clarity and guardrails, especially around keeping Phase 0 narrow, tuning completion detection carefully, and respecting the project's `Verified` status rule.
