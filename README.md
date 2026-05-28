# ChatStory — Story Workstation

A native Windows desktop application for long-form collaborative storytelling with ChatGPT.

ChatStory embeds a Chromium browser (via JCEF) alongside native UI panels for structured prompt composition, story output display, and continuity management. The goal is a dedicated storytelling environment — not a browser wrapper.

## Current Status

**DevCycle 001 — JCEF Browser Viability Spike**

The application is in early development. The current build is a proof-of-concept spike that validates the core browser bridge premise: can a Java desktop application reliably host ChatGPT, preserve a login session, and exchange data with the page via DOM interaction?

## Running

See [BUILDING.md](BUILDING.md) for setup instructions and requirements.

Quick start (Java 21 JDK required):

```bat
gradlew.bat run
```

On first run, ~100 MB of Chromium native binaries are downloaded automatically.

## Planning

See `doc/planning/` for active DevCycle documents.
See `doc/planning/ideas/` for the MVP implementation plan (`ClaudePlan03.md`).
