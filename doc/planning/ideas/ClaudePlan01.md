# ClaudePlan01: Story Workstation MVP

**Status:** Planning
**Created:** 2026-05-28
**Source Document:** `ideas02.md`
**Focus:** Validate the browser bridge — prove that a Java desktop application can host ChatGPT, send prompts, and retrieve responses reliably.

---

## Goal

Build the smallest possible functional version of the Story Workstation that proves the core technical premise. This plan covers the full MVP as described in `ideas02.md`: an embedded Chromium browser hosting ChatGPT, a native input box, and a native output panel that displays the latest assistant response.

No storytelling features are included. The sole purpose is to validate that the browser integration layer works reliably enough to build on.

## Desired Outcome

At the end of this plan:

- The application launches and loads ChatGPT inside an embedded Chromium window
- The user can log in once and remain logged in across restarts
- The user can type a prompt into a native input box and send it
- The application detects when the assistant finishes responding
- The latest assistant response appears in a native output panel separate from the browser UI

If all five of these work reliably, the Story Workstation concept is technically viable and Phase 1 of the full product can begin.

---

## Technology Stack

### Language
Java 21+

### Build System
Gradle (Kotlin DSL)
- Enables straightforward JCEF dependency wiring
- Better IDE integration than Maven for multi-module projects down the road

### Browser Framework
JCEF (Java Chromium Embedded Framework)

**Reasoning:**
- Full Chromium engine — ChatGPT renders and behaves as it does in Chrome
- Supports `CefMessageRouter` for bidirectional JS↔Java communication
- Persistent cookie/session storage via `CefCookieManager` with a profile directory
- Better long-term DOM integration support than JavaFX WebView

**Distribution approach:**
Use the `jcef-maven` artifact published by `dev.datlag` or the official JCEF binary distribution. The native binaries (`.dll`, `.so`, `.dylib`) must ship alongside the JAR. On Windows, these go in the working directory or on the `java.library.path`.

### UI Framework
Swing

Swing is sufficient for the MVP. It has no additional runtime dependencies and integrates directly with JCEF's `CefBrowser.getUIComponent()`. JavaFX can be considered for a later polish cycle.

---

## Project Structure

```
ChatStory/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   └── main/
│       ├── java/
│       │   └── com/chatstory/
│       │       ├── Main.java                  ← entry point
│       │       ├── AppFrame.java              ← top-level Swing JFrame
│       │       ├── browser/
│       │       │   ├── BrowserPanel.java      ← JCEF browser component
│       │       │   ├── BrowserClient.java     ← CefClient handler
│       │       │   └── DomBridge.java         ← JS↔Java message router
│       │       └── ui/
│       │           ├── InputPanel.java        ← native input textarea + Send button
│       │           └── OutputPanel.java       ← native response display
│       └── resources/
│           └── js/
│               ├── inject_text.js
│               ├── trigger_send.js
│               └── extract_response.js
```

---

## Phase 1 — Embedded Browser

**Status:** Planning

### Tasks

- [ ] Initialize Gradle project with Java 21 source compatibility
- [ ] Add JCEF dependency and configure native library extraction on startup
- [ ] Implement `BrowserPanel` — wraps `CefBrowser` and exposes `getUIComponent()`
- [ ] Implement `BrowserClient` — extends `CefClientAdapter`, handles load events
- [ ] Implement `AppFrame` — Swing `JFrame` containing only the browser panel for now
- [ ] On startup, navigate to `https://chatgpt.com`
- [ ] Configure a persistent profile directory (e.g., `~/.chatstory/profile/`) so cookies survive restarts
- [ ] Verify ChatGPT renders and is fully functional inside the embedded window
- [ ] Confirm that a manual login session persists across application restarts

### Technical Notes

**JCEF initialization:**
```java
CefSettings settings = new CefSettings();
settings.cache_path = System.getProperty("user.home") + "/.chatstory/profile";
CefApp cefApp = CefApp.getInstance(settings);
CefClient client = cefApp.createClient();
CefBrowser browser = client.createBrowser("https://chatgpt.com", false, false);
```

**Shutdown:**
Register a `CefApp.getInstance().dispose()` call in the JVM shutdown hook to prevent native library leaks.

**Windows note:**
JCEF native DLLs must be in the working directory. The startup code should check for their presence and print a clear error if missing rather than silently crashing.

**Success Criteria:**
- ChatGPT loads and operates normally inside the application window
- Logging in once persists across restarts (cookies stored in profile directory)

---

## Phase 2 — Native Input

**Status:** Planning

### Tasks

- [ ] Implement `InputPanel` — `JTextArea` for multi-line input + `JButton` ("Send")
- [ ] Add `InputPanel` below the browser panel in `AppFrame`
- [ ] Implement `DomBridge` — registers a `CefMessageRouter` for JS-to-Java callbacks
- [ ] Load and execute `inject_text.js` when Send is triggered
- [ ] Load and execute `trigger_send.js` after text is injected
- [ ] Verify that prompts appear correctly in the ChatGPT input field and are sent

### Technical Notes

**Layout:**
```
AppFrame (BorderLayout)
├── NORTH/CENTER: BrowserPanel (takes most vertical space)
└── SOUTH: InputPanel (fixed height, ~120px)
```

**Injecting text into ChatGPT's input:**

ChatGPT uses a `contenteditable` div, not a plain `<textarea>`. A plain `element.value = ...` assignment does not work. The correct approach:

```javascript
// inject_text.js
(function(text) {
    const editor = document.querySelector('[contenteditable="true"][data-id]')
                || document.querySelector('div[contenteditable="true"]');
    if (!editor) return 'ERROR: editor not found';
    editor.focus();
    document.execCommand('selectAll', false, null);
    document.execCommand('insertText', false, text);
    return 'OK';
})('{TEXT}');
```

The `{TEXT}` token is replaced server-side in Java before the script is executed via `CefBrowser.executeJavaScript(...)`.

**Triggering send:**

```javascript
// trigger_send.js
(function() {
    const btn = document.querySelector('button[data-testid="send-button"]');
    if (!btn) return 'ERROR: send button not found';
    btn.click();
    return 'OK';
})();
```

**Selector fragility note:**
ChatGPT's DOM structure changes occasionally. The selectors above are representative. Before committing to specific selectors, inspect the live DOM in the embedded browser using JCEF's DevTools support (`CefBrowser.showDevTools(...)`). Record the confirmed selectors in this document once validated.

**Success Criteria:**
- User types into the native input box, presses Send, and the prompt appears in ChatGPT and is submitted

---

## Phase 3 — Response Extraction

**Status:** Planning

### Tasks

- [ ] Implement completion detection — poll for the "Stop generating" button disappearing, or observe DOM mutations via JS
- [ ] Implement `extract_response.js` to read the latest assistant message text
- [ ] Pass extracted text from JS back to Java via `CefMessageRouter` or `executeJavaScript` return value
- [ ] Implement `OutputPanel` — scrollable `JTextArea` for displaying assistant response
- [ ] Add `OutputPanel` to `AppFrame` (between browser and input panels, or in a split pane)
- [ ] Wire extracted response into `OutputPanel` on the Swing EDT
- [ ] Clear `OutputPanel` when a new prompt is sent; populate it when response is complete

### Technical Notes

**Completion detection strategy:**

The most reliable signal that ChatGPT has finished generating is the disappearance of the "Stop generating" button. Poll every 500ms after sending:

```javascript
// Returns true if generation is still in progress
(function() {
    const stopBtn = document.querySelector('button[aria-label="Stop generating"]')
                 || document.querySelector('[data-testid="stop-button"]');
    return stopBtn !== null;
})();
```

When the poll returns `false`, wait an additional 200ms and then extract.

Alternative: use a `MutationObserver` registered via JS that posts a message to the `CefMessageRouter` when the response container's `aria-live` region stops updating. This is more efficient than polling but harder to implement in Phase 3. Polling is acceptable for MVP.

**Extracting the response:**

```javascript
// extract_response.js
(function() {
    const messages = document.querySelectorAll('[data-message-author-role="assistant"]');
    if (!messages.length) return '';
    const last = messages[messages.length - 1];
    return last.innerText || last.textContent || '';
})();
```

**Returning values from JS to Java:**

`CefBrowser.executeJavaScript()` does not return a value. Use `CefMessageRouter` with a query/response handler, or use a workaround where the JS sets a known DOM attribute and a follow-up JS call reads it back:

```java
// Preferred: CefMessageRouter with registered handler
router.addHandler(new CefMessageRouterHandlerAdapter() {
    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame,
                           long queryId, String request,
                           boolean persistent, CefQueryCallback callback) {
        // request contains the extracted text
        SwingUtilities.invokeLater(() -> outputPanel.setText(request));
        callback.success("");
        return true;
    }
}, false);
```

The JS side calls `window.cefQuery({ request: extractedText })`.

**OutputPanel placement:**

```
AppFrame (BorderLayout or JSplitPane)
├── TOP: BrowserPanel (resizable)
├── MIDDLE: OutputPanel (scrollable, ~30% height)
└── BOTTOM: InputPanel (fixed)
```

A `JSplitPane` between browser and output panels gives the user resize control.

**Success Criteria:**
- After sending a prompt and waiting for completion, the latest assistant response appears in the native output panel
- The panel updates correctly on subsequent sends

---

## Phase 4 — Basic Quality of Life

**Status:** Planning

### Tasks

- [ ] Auto-scroll `OutputPanel` to bottom when new content arrives
- [ ] Add "Copy" button to `OutputPanel` that copies response text to clipboard
- [ ] Add prompt history — Up/Down arrow keys in `InputPanel` cycle through sent prompts
- [ ] Add "Resend" button that re-submits the last prompt
- [ ] Clear `InputPanel` after successful send
- [ ] Show a status label ("Sending...", "Waiting for response...", "Done") between the panels

### Technical Notes

These are all pure Java/Swing tasks with no new JCEF complexity. Implement after Phase 3 is stable.

Prompt history can be a simple `ArrayDeque<String>` with a cursor index. Limit to 50 entries for MVP.

---

## Open Questions

1. **Which JCEF distribution artifact to use?**
   Recommendation: Start with the `dev.datlag:jcef` Maven artifact which includes a Gradle-friendly native extraction mechanism. If that proves problematic, fall back to manually downloading JCEF binaries from the official JCEF build repository and placing them in the project.

2. **ChatGPT DOM selector stability**
   Recommendation: Before writing Phase 2 and 3 JS, open the embedded browser, enable DevTools (`CefBrowser.showDevTools`), and manually inspect the live ChatGPT DOM to confirm the correct selectors. Document confirmed selectors here before coding against them. Plan for selectors to change and isolate them in the JS resource files so they can be updated without recompiling.

3. **Session persistence on first run**
   The user must log in manually on first launch. The profile directory approach handles persistence from that point forward. Recommendation: On first launch, navigate directly to `https://chatgpt.com` and show a status message instructing the user to log in. After login is detected (poll for a known logged-in DOM element), navigate to the target chat URL.

4. **Target chat URL**
   The MVP hardcodes a chat URL per `ideas02.md`. Recommendation: Store this in a simple `~/.chatstory/config.properties` file. Even though dynamic project selection is out of scope, a hardcoded Java string is harder to change than a config file.

---

## Notes and Risks

- **JCEF native binaries on Windows:** The application will not start without the correct JCEF native DLLs for the target architecture. Build and test on the deployment machine early. The JCEF startup sequence is strict about library path and init order.

- **ChatGPT DOM changes:** OpenAI updates the ChatGPT frontend regularly. The JS interaction layer is the most fragile part of this system. Isolate all selectors in resource files, not hardcoded strings. Plan for at least one "selector update" maintenance cycle before the MVP is considered stable.

- **`contenteditable` injection:** Simple `value` assignment does not work on React-controlled inputs. The `document.execCommand('insertText', ...)` approach works in Chromium but is technically deprecated. If it stops working, the alternative is dispatching synthetic `InputEvent` or `KeyboardEvent` objects. Test this early in Phase 2.

- **Response completion detection:** Polling is simple but introduces a fixed latency. If ChatGPT changes its DOM structure (e.g., removes the stop button), detection breaks silently. Add a timeout fallback (e.g., 3 minutes) so the application does not hang indefinitely.

- **Swing EDT threading:** All `OutputPanel` and `InputPanel` updates triggered from JCEF callbacks must be dispatched via `SwingUtilities.invokeLater(...)`. JCEF callbacks run on CEF threads, not the Swing EDT. Failure to do this causes intermittent rendering bugs that are hard to reproduce.

---

## Completion Summary

*Fill in when all four phases are complete.*

**Completion Date:** —
**Phases Completed:** —
**Work Deferred:** —

**Accomplishments:**
-

**Lessons / Notes:**
-
