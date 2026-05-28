# Codex DC2 Plan 01: Application Foundation

## Purpose

This document proposes the scope for DevCycle 002 after the successful completion of DevCycle 001.

DevCycle 001 proved the browser foundation:

- JCEF launches successfully.
- ChatGPT renders inside the embedded browser.
- DevTools are accessible.
- Manual login works.
- Session persistence works through `%LOCALAPPDATA%\ChatStory\profile`.
- `CefMessageRouter` can receive a JavaScript-to-Java ping.

DevCycle 002 should turn that successful spike into a small, maintainable application foundation. It should not implement prompt injection, native prompt sending, or response extraction yet.

## Recommended DevCycle

```text
DevCycle 002: Application Foundation
```

## Goal

Convert the DevCycle 001 JCEF spike into a structured application scaffold that future bridge work can safely build on.

The cycle should establish:

- clear application structure
- browser host separation
- basic app state management
- EDT-safe UI update conventions
- structured JS-to-Java bridge message handling
- selector resource loading
- optional target URL configuration
- focused unit tests for non-browser logic

## Desired Outcome

At the end of DevCycle 002:

- The app still launches with the documented Gradle command.
- ChatGPT still loads in the embedded browser.
- The login session still persists across restarts.
- DevTools remain easy to open.
- Spike code has been organized into maintainable classes.
- The app has a basic status/state model.
- `CefMessageRouter` messages are parsed as structured bridge messages.
- `chatgpt_selectors.json` loads successfully from resources.
- Foundation unit tests pass.

## Explicit Non-Goals

DevCycle 002 should not include:

- prompt injection
- native prompt input
- prompt send button
- response completion detection
- response extraction
- output panel
- story parsing
- XML/tagged response formats
- prompt history
- resend behavior
- MutationObserver completion detection
- packaging or installer work

Those belong in later DevCycles after the foundation is clean.

## Proposed Tasks

### Phase 1: Project Structure

**Status:** Planning

- [ ] Review the DevCycle 001 spike code and identify what should be kept, moved, or replaced.
- [ ] Formalize the Gradle project structure around the chosen JCEF dependency.
- [ ] Confirm `gradlew.bat run` still launches the application.
- [ ] Keep `BUILDING.md` accurate if any launch or setup details change.

**Technical Notes:**

DevCycle 001 code was allowed to be temporary. DevCycle 002 may restructure it freely as long as the proven JCEF setup, profile path, DevTools access, and login persistence continue to work.

### Phase 2: Application Shell

**Status:** Planning

- [ ] Implement or refine `Main.java` as the application entry point.
- [ ] Keep JCEF initialization and shutdown order explicit.
- [ ] Register a JVM shutdown hook to dispose of JCEF.
- [ ] Implement or refine `AppFrame.java` as the top-level Swing frame.
- [ ] Preserve the DevTools toolbar button or menu item.
- [ ] Add a visible status label.

**Technical Notes:**

DevTools access should remain available throughout all MVP work because selector validation and DOM debugging depend on it.

### Phase 3: Browser Layer

**Status:** Planning

- [ ] Implement `BrowserPanel.java` to own the JCEF browser component.
- [ ] Implement `BrowserClient.java` to handle browser load events.
- [ ] Keep browser profile configuration at `%LOCALAPPDATA%\ChatStory\profile`, with fallback under `user.home`.
- [ ] Confirm session persistence still works after the refactor.
- [ ] Keep default navigation to `https://chatgpt.com` unless local config provides a target URL.

**Technical Notes:**

The browser layer should isolate JCEF-specific setup from the rest of the app where practical. Later prompt injection and extraction work should build on this layer rather than reaching directly into app-frame code.

### Phase 4: State And UI Threading

**Status:** Planning

- [ ] Implement `AppState.java` as the source of truth for startup, loading, ready, and error states.
- [ ] Include at least these initial states:
  - `Starting`
  - `LoadingChatGPT`
  - `NeedsLogin`
  - `Ready`
  - `Error`
- [ ] Add state transition methods or validation so invalid transitions are easy to catch.
- [ ] Implement `UiThread.java` with a helper that wraps `SwingUtilities.invokeLater(...)`.
- [ ] Ensure JCEF callbacks update Swing UI through `UiThread`.
- [ ] Display state changes in the app status label.

**Technical Notes:**

`AppState` should not depend on JCEF or Swing. It should be unit-testable. The fuller prompt lifecycle states, such as `InjectingPrompt`, `Sending`, `WaitingForResponse`, and `Complete`, can be added now if simple, or added in DevCycle 003 when prompt sending begins.

### Phase 5: Bridge Foundation

**Status:** Planning

- [ ] Implement `DomBridge.java` to own the `CefMessageRouter`.
- [ ] Convert the DevCycle 001 ping into a structured bridge message.
- [ ] Implement `BridgeMessage.java` for parsing JS-to-Java JSON messages.
- [ ] Define `ChatBridge.java` as the future prompt bridge interface.
- [ ] Define `ResponseListener.java` for future async callbacks.
- [ ] Include `requestId` in the bridge message model.
- [ ] Log received bridge messages with message type and request ID when present.
- [ ] Add foundational error codes, including:
  - `page_load_failed`
  - `not_on_chatgpt_page`
  - `login_required`
  - `bridge_message_invalid`

**Technical Notes:**

DevCycle 002 should not send prompts. The bridge work here is about message routing, parsing, logging, and future-proof interfaces. Prompt-specific messages like `injectResult` and `responseComplete` can be defined now, but their behavior should wait until later cycles.

### Phase 6: Selector Resource Loading

**Status:** Planning

- [ ] Add `src/main/resources/js/chatgpt_selectors.json`.
- [ ] Include initial selector groups for:
  - `promptEditor`
  - `sendButton`
  - `stopButton`
  - `assistantMessage`
- [ ] Implement resource loading for the selector JSON.
- [ ] Validate that the resource exists and can be read on startup.
- [ ] Do not rely on these selectors for behavior yet.

**Technical Notes:**

Selectors are unstable and should be treated as candidates. DevCycle 002 only needs loading and validation. Live selector confirmation belongs closer to prompt injection work in DevCycle 003.

### Phase 7: Optional Target URL Config

**Status:** Planning

- [ ] Add optional config loading from `%APPDATA%\ChatStory\config.properties`.
- [ ] Support `target.chat.url`.
- [ ] Default to `https://chatgpt.com` when config is absent.
- [ ] Ensure real chat URLs remain untracked.
- [ ] Keep or update `config.example.properties` with safe example values only.

**Technical Notes:**

This should stay small. If config handling starts to distract from the foundation, keep the existing default URL and defer richer config behavior.

### Phase 8: Foundation Tests

**Status:** Planning

- [ ] Add tests for `AppState` transitions.
- [ ] Add tests for `BridgeMessage` parsing.
- [ ] Add tests for malformed bridge messages.
- [ ] Add tests that resource files such as `chatgpt_selectors.json` can be loaded.
- [ ] Confirm all tests pass.

**Technical Notes:**

Do not attempt to automate ChatGPT browser behavior in unit tests. Keep tests focused on pure Java logic and resource loading.

## Suggested Success Criteria

DevCycle 002 should be considered `Work Complete` when:

- `gradlew.bat run` launches the refactored app.
- ChatGPT loads in the embedded browser.
- Login session still persists across restart.
- DevTools remain accessible.
- The app shows basic state/status changes.
- JCEF callbacks that touch Swing UI go through `UiThread`.
- A structured `CefMessageRouter` ping is received and parsed.
- `chatgpt_selectors.json` loads successfully.
- Optional target URL config works or is explicitly deferred.
- Foundation tests pass.
- `BUILDING.md` remains accurate.

Do not mark the DevCycle or any phase `Verified` unless the user explicitly approves that status.

## Risks

- Refactoring the spike may accidentally break the proven JCEF initialization sequence.
- JCEF initialization and shutdown order are sensitive.
- Swing updates from JCEF callbacks can cause intermittent bugs if not routed through the EDT.
- Config loading can grow beyond the intended scope if it is overdesigned.
- Selector files may tempt premature prompt-injection work; keep this cycle focused on loading resources only.

## Recommended Next DevCycle After This

If DevCycle 002 succeeds, DevCycle 003 should be:

```text
DevCycle 003: Prompt Injection and Native Input
```

That next cycle should begin with a hardcoded "Test Inject" button and prove editor injection before building the full native input panel.
