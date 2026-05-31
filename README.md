# ChatStory — Story Workstation

A native Windows desktop application for long-form collaborative storytelling with ChatGPT.

ChatStory embeds a Chromium browser (via JCEF) alongside native UI panels for structured prompt composition, story output display, and continuity management. The goal is a dedicated storytelling environment — not a browser wrapper.

## Current Status

**DevCycle 010 — Canon Editing, Tab Highlight Fix, Context File Viewer**

DC 010 marks the start of substantive user testing. Initial functionality is in place — the core send pipeline, story mode, canon management, context file injection, and correction actions — along with a round of usability improvements. Development from this point is informed by real use.

### Completed DevCycles

| Cycle | Focus |
|-------|-------|
| DC 001 | JCEF browser viability spike |
| DC 002 | App state machine and browser lifecycle |
| DC 003 | Prompt injection pipeline |
| DC 004 | Response extraction and streaming display |
| DC 005 | Scene prompt builder (structured story input) |
| DC 006 | Story mode toggle and dark theme |
| DC 007 | Save responses as canon |
| DC 008 | Context file injection |
| DC 009 | Correction actions (right-click) and Redo/Reset toolbar buttons |
| DC 010 | Editable canon panel, tab contrast fix, context file viewer with copy |

## Running

See [BUILDING.md](BUILDING.md) for setup instructions and requirements.

Quick start (Java 21 JDK required):

```bat
gradlew.bat run
```

On first run, ~100 MB of Chromium native binaries are downloaded automatically.

## Keyboard Shortcuts

These shortcuts work globally from anywhere in the app.

| Shortcut | Action |
|----------|--------|
| `Ctrl+Shift+M` | Switch to the Main tab |
| `Ctrl+Shift+P` | Switch to the Picture tab |
| `Ctrl+Shift+S` | Send the current input |
| `Ctrl+Shift+C` | Send Context |
| `Ctrl+Shift+R` | Redo |
| `Ctrl+Shift+F` | Fetch (force-read the current response from the browser) |
| `Ctrl+Shift+X` | Reset (force app state back to Ready) |

## Planning

See `doc/planning/` for active DevCycle documents.
See `doc/planning/ideas/` for the MVP implementation plan (`ClaudePlan03.md`).

## Known Issues

- **Hang on scroll after send** — The application can hang if the user scrolls in the browser window immediately after sending a prompt. This appears to be a conflict between the browser's scroll handling and the response-reading or UI-update pipeline. Workaround: avoid scrolling in the browser while a response is in progress.

## License

This project is licensed under the [MIT License](LICENSE).

## AI-Assisted Development Notes

This project is being built with AI coding assistance. Capacity usage recorded at notable milestones:

| Milestone | Codex weekly usage | Claude weekly usage |
|-----------|-------------------|---------------------|
| After DC 010 | ~25% | ~3–6% (usage had reset) |
