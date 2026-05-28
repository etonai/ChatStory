# Codex Plan 01: Implementing the Story Workstation MVP

## Purpose

This document translates `ideas02.md` into an implementation plan for the first working Story Workstation MVP.

The goal is to prove the browser bridge before building higher-level storytelling tools. The first version should answer one core question:

Can a native Java desktop application host ChatGPT, preserve login state, send prompts, detect completion, extract the latest assistant response, and display that response in a native UI?

## Source Idea

Primary source:

- `doc/planning/ideas/ideas02.md`

Related long-term concept:

- `doc/planning/ideas/ideas01.txt`

## Implementation Principles

- Keep the MVP intentionally small.
- Validate embedded browser behavior first.
- Prefer DOM interaction over mouse coordinates, image matching, or screen scraping.
- Build only the minimum native UI needed to test prompt send and response extraction.
- Avoid story-specific systems until the ChatGPT bridge is reliable.
- Keep technical decisions documented so they can be turned into DevCycle tasks.

## Recommended Initial Stack

### Language

- Java 21+

### Browser Layer

- JCEF, if practical in the local build environment.
- JavaFX WebView only as a fallback if JCEF setup blocks progress.

### Native UI

- Swing for the MVP.
- JavaFX can be reconsidered after the bridge is proven.

### Build System

- Use the existing project build system if one already exists.
- If the project has no application scaffold yet, create the smallest Gradle-based Java application needed to launch the MVP.

## MVP Scope

The MVP should include:

- An embedded Chromium browser.
- Persistent browser session storage.
- Navigation to ChatGPT.
- Optional hardcoded target chat URL.
- A native prompt input area.
- A native send button.
- Programmatic prompt insertion into ChatGPT.
- Programmatic send trigger.
- Completion detection.
- Latest assistant response extraction.
- A native output display area showing the latest assistant response as plain text.

The MVP should not include:

- Multi-pane semantic input.
- Dialogue/action parsing.
- XML response protocols.
- Story-only extraction.
- Continuity tracking.
- Relationship state tracking.
- Correction workflows.
- Upload profiles.
- Session archives.
- Project selection UI.
- Prompt template management.

## Proposed Application Shape

```text
Story Workstation MVP
|
+-- App Launcher
+-- Browser Host
+-- Browser Session Storage
+-- Chat Navigation
+-- Native Input Panel
+-- Native Output Panel
+-- Browser Bridge
    |
    +-- find input
    +-- inject prompt
    +-- send prompt
    +-- detect completion
    +-- extract latest response
```

## Suggested Package Responsibilities

Exact package names can be adjusted to match the codebase once implementation starts.

```text
app
  Main application entry point.

ui
  Swing frame, input panel, output panel, buttons, status labels.

browser
  JCEF initialization, browser component creation, session configuration.

bridge
  DOM scripts and Java-side orchestration for ChatGPT input/output.

config
  Hardcoded MVP settings first; file-based settings later if needed.
```

## Development Phases

### Phase 1: Project and Browser Scaffold

Goal:

Launch a native Java window containing an embedded browser that can load ChatGPT.

Tasks:

- Confirm current project structure and build system.
- Add or confirm Java 21 configuration.
- Add JCEF dependency or local JCEF integration approach.
- Create a minimal application entry point.
- Open a native window.
- Embed the Chromium browser component.
- Navigate to `https://chatgpt.com/`.
- Confirm manual login is possible.

Success criteria:

- ChatGPT renders inside the native application.
- The browser is usable enough for manual login.
- The app can be launched repeatedly without rebuilding special local state.

Risks:

- JCEF dependency setup may be heavier than the rest of the MVP.
- ChatGPT may block or degrade behavior in embedded Chromium.
- Local Chromium profile paths must be configured carefully for persistence.

### Phase 2: Persistent Session and Target Navigation

Goal:

Preserve the logged-in browser session and optionally navigate to a hardcoded target chat.

Tasks:

- Configure a persistent browser profile/cache directory.
- Confirm login survives application restart.
- Add a hardcoded ChatGPT URL constant.
- Navigate to the hardcoded URL after startup.
- Add a basic status indicator for page loading.

Success criteria:

- User logs in once.
- Restarting the application keeps the user logged in.
- The app can open the intended ChatGPT page or chat without manual navigation.

Risks:

- Session persistence behavior may differ depending on JCEF configuration.
- ChatGPT authentication may require browser capabilities not enabled by default.

### Phase 3: Native Input and Prompt Injection

Goal:

Send native text input into ChatGPT through DOM interaction.

Tasks:

- Add a native text input area.
- Add a send button.
- Support Enter or Ctrl+Enter only if it can be done without disrupting multiline input.
- Implement JavaScript to find the ChatGPT input element.
- Inject prompt text into the input element.
- Dispatch the required input/change events.
- Trigger send through a DOM button or form action.
- Disable send while a request is in progress.
- Show basic error feedback if the input cannot be found.

Success criteria:

- Text typed in the native input appears correctly in ChatGPT.
- The app can send a prompt without mouse interaction.
- Failed selector lookup produces an understandable status message.

Risks:

- ChatGPT DOM selectors may change.
- The input field may be contenteditable rather than a simple textarea.
- React-controlled input may require specific event dispatching.

### Phase 4: Response Completion Detection

Goal:

Detect when ChatGPT has finished responding.

Tasks:

- Identify reliable DOM signals for active generation.
- Track the latest assistant message node.
- Poll or observe DOM mutations after sending.
- Detect when the response text has stabilized.
- Add a timeout or manual retry path.
- Keep implementation simple and inspectable.

Success criteria:

- The app can distinguish "response still generating" from "response complete" often enough for MVP testing.
- The native UI does not read partial output as final unless the operation times out or is cancelled.

Risks:

- ChatGPT generation UI may change.
- Streaming text may briefly pause before continuing.
- There may be multiple assistant messages, regenerated responses, or hidden DOM nodes.

### Phase 5: Latest Response Extraction

Goal:

Extract the latest assistant response and display it in the native output panel.

Tasks:

- Implement JavaScript to locate assistant message containers.
- Extract plain text from the latest assistant response.
- Return the extracted text to Java.
- Display the result in a native output area.
- Auto-scroll the native output area to the newest content.
- Preserve the raw text without story parsing.

Success criteria:

- After a prompt completes, the latest assistant response appears in the native output panel.
- The output panel shows plain text only.
- The extraction does not include user prompt text or unrelated page UI.

Risks:

- Assistant message selectors may be unstable.
- Markdown formatting may not map cleanly to plain text.
- Hidden controls or labels may leak into extracted text.

### Phase 6: MVP Hardening

Goal:

Make the prototype reliable enough to evaluate.

Tasks:

- Add minimal logging for browser bridge operations.
- Add visible status states: loading, ready, sending, waiting, complete, error.
- Add retry behavior for failed DOM lookup.
- Add a copy response button if trivial.
- Add prompt history only if it does not distract from bridge validation.
- Document known selector assumptions.
- Document manual test steps.

Success criteria:

- A user can run the app, log in, send a prompt, and see the assistant response in the native output panel.
- Common failures produce actionable messages.
- The MVP remains small and focused.

Risks:

- Quality-of-life additions can distract from the core bridge.
- Too much abstraction too early can slow selector iteration.

## Browser Bridge Strategy

The browser bridge should be isolated behind a small Java API.

Example conceptual API:

```java
interface ChatBridge {
    void sendPrompt(String prompt);
    boolean isReady();
    boolean isGenerating();
    String getLatestAssistantResponse();
}
```

The implementation can use injected JavaScript for the first MVP. The JavaScript should be stored as readable source strings or resource files, not scattered across UI event handlers.

## Selector Strategy

Start with simple selector experiments, but keep them centralized.

Candidate operations:

- Find the prompt editor.
- Set editor content.
- Dispatch input events.
- Find the send button.
- Find assistant messages.
- Detect active generation.

Selectors should be considered unstable. The implementation should make it easy to update them without touching the rest of the application.

## Configuration Strategy

For the MVP, keep configuration minimal.

Initial values may be hardcoded:

- ChatGPT home URL.
- Optional target chat URL.
- Browser profile/cache directory.

After the bridge is proven, these can move into a local config file.

## Manual Validation Plan

### Test 1: Browser Loads

Steps:

1. Launch the application.
2. Confirm ChatGPT appears in the embedded browser.
3. Log in manually if needed.

Pass condition:

- ChatGPT is usable inside the application.

### Test 2: Login Persists

Steps:

1. Log in.
2. Close the application.
3. Relaunch the application.

Pass condition:

- The user remains logged in.

### Test 3: Prompt Sends

Steps:

1. Type a short prompt in the native input.
2. Click Send.

Pass condition:

- The prompt appears in ChatGPT and is submitted.

### Test 4: Response Extracts

Steps:

1. Send a short prompt.
2. Wait for completion.

Pass condition:

- The latest assistant response appears in the native output area.

### Test 5: Repeated Sends

Steps:

1. Send three short prompts in the same chat.
2. Observe each extracted response.

Pass condition:

- Each latest response is captured correctly without requiring manual browser interaction.

## Suggested DevCycle Breakdown

### DevCycle 001: Browser Scaffold

Focus:

- Create the Java application shell and embedded browser.

Likely tasks:

- Confirm or create Java build.
- Integrate JCEF.
- Launch native window.
- Load ChatGPT.
- Document JCEF setup notes.

### DevCycle 002: Session Persistence and Navigation

Focus:

- Make the embedded browser useful across launches.

Likely tasks:

- Configure persistent browser profile path.
- Validate login persistence.
- Add hardcoded target URL.
- Add basic loading/status state.

### DevCycle 003: Native Input to ChatGPT

Focus:

- Prove prompt injection and send.

Likely tasks:

- Build input UI.
- Centralize bridge JavaScript.
- Inject prompt text.
- Trigger send.
- Handle selector failure.

### DevCycle 004: Response Extraction

Focus:

- Detect completion and mirror the latest assistant response.

Likely tasks:

- Locate assistant messages.
- Detect generation completion.
- Extract latest response text.
- Display native output.
- Add manual validation notes.

### DevCycle 005: MVP Hardening

Focus:

- Make the bridge reliable enough for repeated local use.

Likely tasks:

- Improve status reporting.
- Add retry behavior.
- Add minimal logging.
- Document known limitations.
- Decide whether MVP is ready for higher-level Story Workstation planning.

## Open Questions

1. **Should the first implementation use JCEF immediately, or begin with JavaFX WebView as a temporary spike?**

   Recommendation: Start with JCEF unless setup becomes the main blocker. The MVP is specifically about validating a realistic browser bridge, and JCEF is closer to the intended long-term browser layer.

2. **Should the hardcoded chat URL be committed?**

   Recommendation: No. Commit a placeholder or local config mechanism. Real project/chat URLs should remain local.

3. **Should JavaScript bridge code live in Java strings or resource files?**

   Recommendation: Use resource files once scripts become more than a few lines. Early tiny snippets can start inline, but selectors and extraction scripts should be centralized quickly.

4. **Should the app hide the embedded browser once native output works?**

   Recommendation: No. Keep the browser visible during the MVP so failures can be inspected directly.

5. **Should response extraction preserve Markdown?**

   Recommendation: No for MVP. Extract plain text only. Formatting can be revisited after bridge reliability is proven.

## Completion Definition For The MVP

The MVP implementation plan is complete when:

- The app launches a native desktop window.
- ChatGPT loads in an embedded browser.
- Login persists across app restarts.
- A hardcoded ChatGPT page or chat can be opened.
- Native input can send a prompt into ChatGPT.
- The app can wait for the assistant response to complete.
- The latest assistant response appears as plain text in the native output area.
- Known limitations and selector assumptions are documented.

## Future Work After MVP

Only after the MVP succeeds should planning move toward:

- Structured dialogue/action panes.
- Story-only response parsing.
- XML or tagged response protocols.
- Correction prompts.
- File bundle upload profiles.
- Session archive storage.
- Continuity and canon tools.
- Timeline, relationship, and state visualization.
