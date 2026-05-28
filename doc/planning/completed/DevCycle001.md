# DevCycle 001: JCEF Browser Viability Spike

**Status:** Work Complete
**Start Date:** 2026-05-28
**Target Completion:** 2026-06-11
**Focus:** Prove that JCEF can launch on this machine, render ChatGPT, support manual login, and preserve a session across restarts.

---

## Goal

Validate the core technical premise of the Story Workstation before committing to any application architecture. This DevCycle answers one question: can a Java Swing application embed a Chromium browser, load ChatGPT, complete a real login flow, and keep that session alive across restarts?

The code produced here is intentionally minimal and may be temporary. DevCycle 002 will restructure or replace it once the JCEF path is proven.

## Desired Outcome

At the end of this DevCycle:

- A Java application launches a Swing window containing a JCEF-embedded Chromium browser
- ChatGPT loads and operates normally inside that window
- JCEF DevTools can be opened and used to inspect the live ChatGPT DOM
- The user can complete a manual login using their actual intended authentication method
- The logged-in session survives closing and relaunching the application
- A developer can repeat the launch from a documented command in `BUILDING.md`

If these five criteria are met, the browser integration layer is viable and DevCycle 002 can begin building the real application scaffold on top of it.

---

## Tasks

### Phase 1: JCEF Research and Minimum Launch

**Status:** Work Complete

- [x] Research available JCEF distributions — evaluate `dev.datlag:jcef`, the official JCEF build repository, and any other current options
- [x] Confirm the chosen distribution supports Windows x64 and Java 21
- [x] Confirm whether the artifact bundles Chromium native binaries or requires separate acquisition and placement
- [x] Confirm whether the chosen integration approach is compatible with a future `jpackage`-based Windows installer — if not, choose a different approach before proceeding
- [x] Create a minimal Gradle project (Kotlin DSL) with the JCEF dependency wired up
- [x] Write the minimum Java entry point to open a Swing `JFrame` containing a JCEF browser component
- [x] Navigate to `https://chatgpt.com` on launch and confirm it renders correctly
- [x] Add a way to open JCEF DevTools (a button, menu item, or keyboard shortcut) and confirm it works

**Phase 1 findings:**
- Distribution chosen: `me.friwi:jcefmaven:146.0.10` (Chromium 146.0.7680.179, May 2026)
- `dev.datlag:kcef` was rejected — archived as of October 2025, maintainer explicitly deprecated it
- Auto-download of ~100 MB Chromium natives succeeded on first run
- JCEF initialized successfully on this machine (Windows 11, Java 21 Temurin)
- Known harmless error at startup: `Failed opening key Software\Chromium to set usagestats; result: 5` — Chromium looking for a Google Update registry key that does not exist in embedded contexts. Safe to ignore.
- jpackage path confirmed: replace auto-download with `me.friwi:jcef-natives-windows-amd64` bundled artifact when packaging is needed

**Technical Notes:**

The distribution research is the first blocker. Some questions can only be answered by running code — minimal throwaway spike code is expected and acceptable here. The goal is to verify the dependency before committing to it as the foundation of the real application.

JCEF requires native binaries (`.dll` on Windows) to be present at runtime. The startup code should detect missing binaries and print a clear error rather than silently crashing with an opaque JVM error.

The JCEF initialization sequence is order-sensitive. `CefApp` must be initialized on the correct thread before any browser components are created. Refer to the JCEF sample code for the correct initialization pattern for the chosen distribution.

Register a JVM shutdown hook to call `CefApp.getInstance().dispose()` to prevent native library leaks on exit.

DevTools access: `CefBrowser.showDevTools(...)` opens a separate DevTools window. This must remain accessible throughout all MVP DevCycles for DOM inspection and selector validation.

---

### Phase 2: Login, Session Persistence, and Documentation

**Status:** Work Complete

- [x] Configure a persistent browser profile directory so cookies and session data survive application restarts
  - Preferred Windows path: `%LOCALAPPDATA%\ChatStory\profile`
  - Fallback if `LOCALAPPDATA` is unavailable: `{user.home}/.chatstory/profile`
- [x] Attempt a complete manual login using the actual intended authentication method
- [x] Close and relaunch the application; confirm the user remains logged in
- [x] Document all findings in `BUILDING.md`

**Phase 2 findings:**
- Session persistence confirmed: ChatGPT was already logged in on relaunch with no re-authentication required
- Profile stored at `%LOCALAPPDATA%\ChatStory\profile` — cookies and session data persisted correctly
- No additional configuration was required beyond `CefSettings.cache_path`
- `BUILDING.md` already covers setup, paths, and known issues from Phase 1

**Technical Notes:**

Session persistence depends on the JCEF `CefSettings.cache_path` being set to the profile directory before `CefApp` is initialized. If this is not set, each launch starts a fresh browser session with no cookies.

ChatGPT authentication may involve OAuth redirects, popup windows, or other flows that require JCEF to handle correctly. If the login flow fails to complete inside the embedded browser, document exactly where it fails and what the error or behavior is — this is a Phase 2 blocker if it cannot be resolved.

`BUILDING.md` should be complete enough that a developer setting up the project for the first time can follow it without prior JCEF knowledge. Write it while the setup is fresh.

---

### Stretch Goal: CefMessageRouter Ping

**Status:** Work Complete
**Note:** Implemented alongside Phase 1 and confirmed in the same run.

- [x] Add a `CefMessageRouter` to the browser client
- [x] Add a small JavaScript snippet that calls `window.cefQuery({ request: JSON.stringify({ type: "ping" }) })` on page load
- [x] Register a Java-side query handler that logs the received ping message
- [x] Confirm the round-trip works: JS sends, Java receives, result is logged

**Ping confirmed:** Console output on first run showed `[Bridge] Received from JS: {"type":"ping"}` three times (corresponding to three page load events during ChatGPT navigation). The JS→Java message channel is operational.

**API discovery note:** The correct package in jcef 146 is `org.cef.browser.CefMessageRouter` (not `org.cef.network`). DevTools is `browser.openDevTools()` (not `showDevTools`). These are documented in `BUILDING.md`.

**Technical Notes:**

The ping exists only to confirm the `CefMessageRouter` channel works before DevCycle 002 builds the full bridge protocol on top of it. A working ping removes one unknown from DevCycle 002 planning.

If the ping is not completed in this DevCycle, it becomes the first task of DevCycle 002.

---

## Open Questions

1. **Which JCEF distribution will be used?**
   Answer this during Phase 1 research. Evaluate at least two options before choosing. Document the decision and reasoning in `BUILDING.md`.

2. **Which login method should be tested?**
   Use the authentication method the user will actually use. Confirm this with the user before Phase 2 begins if there is any ambiguity.

3. **Should `BUILDING.md` be a new file or a section of `README.md`?**
   Use `README.md` if JCEF setup fits in a few short steps. Create a dedicated `BUILDING.md` if native binary setup, JVM flags, or known issues require more than a page.

---

## Notes and Risks

- **JCEF native binaries are the most likely first blocker.** Distribution availability, architecture matching, and runtime path configuration can consume significant time. Treat Phase 1 as a research task, not just a dependency addition.

- **ChatGPT login may not complete in embedded Chromium.** Some authentication flows (WebAuthn, passkeys, device verification) may behave differently or fail entirely in an embedded browser. If login does not work, document exactly what fails — this is a go/no-go decision point for the entire project.

- **Session persistence requires correct `CefSettings.cache_path` configuration.** This must be set before `CefApp` is initialized. If set after, it has no effect and the session will not persist.

- **Spike code does not need to be production quality.** DevCycle 001 code is a proof of concept. DevCycle 002 will build the real application scaffold. Do not over-engineer the spike.

- **DevCycle 001 scope is intentionally narrow.** No native input, no prompt injection, no response extraction, no state machine. Anything beyond proving the browser is out of scope and should be deferred.

---

## Completion Summary

*Fill in when DevCycle 001 closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-05-28
**Phases Completed:** All (Phase 1, Phase 2, Stretch Goal)
**Work Deferred:** None

**Accomplishments:**
- JCEF launches, initializes, and renders ChatGPT correctly on this machine
- DevTools accessible via toolbar button
- Login session persists across application restarts via `%LOCALAPPDATA%\ChatStory\profile`
- `CefMessageRouter` JS→Java ping confirmed operational (3 pings received on first run)
- Gradle 8.13 (Kotlin DSL) build working with `gradlew.bat run`
- `BUILDING.md`, `README.md`, `.gitignore`, and `config.example.properties` created

**JCEF Distribution Chosen:**
`me.friwi:jcefmaven:146.0.10` — Chromium 146.0.7680.179, auto-downloads ~100 MB natives on first run

**Login Method Tested:**
User's normal ChatGPT login (session persisted successfully)

**Session Persistence Confirmed:** Yes

**CefMessageRouter Ping Completed:** Yes — `[Bridge] Received from JS: {"type":"ping"}` confirmed in console

**Metrics:**
- Files created: 12
- JCEF artifact: me.friwi:jcefmaven:146.0.10

**Lessons / Notes:**
- `dev.datlag:kcef` is archived — do not use
- Correct JCEF 146 packages differ from older versions: `CefMessageRouter` is in `org.cef.browser` (not `org.cef.network`); DevTools uses `openDevTools()` (not `showDevTools`)
- The Chromium registry error at startup (`Failed opening key Software\Chromium`) is harmless and expected in embedded contexts
- The `CefMessageRouter` ping fired 3 times on a single ChatGPT load — corresponds to multiple navigation/redirect events; this is normal
- DevCycle 002 can proceed immediately: browser bridge foundation is solid
