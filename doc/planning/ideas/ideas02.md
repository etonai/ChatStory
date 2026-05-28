# Story Workstation MVP Plan

# Objective

Create the smallest possible functional version of the Story Workstation application.

The purpose of the MVP is to validate that a native desktop application can reliably:

* host ChatGPT in an embedded browser
* connect to a persistent logged-in session
* navigate to a specific chat
* send prompts programmatically
* receive and display responses separately from the browser UI

This MVP intentionally avoids advanced functionality until the browser interaction layer is proven reliable.

---

# Core MVP Philosophy

The MVP should:

* minimize engineering complexity
* validate browser integration first
* prove reliable message send/receive behavior
* establish the architectural foundation for future systems

The MVP is NOT intended to include:

* continuity systems
* relationship tracking
* story parsing
* correction workflows
* upload management
* metadata extraction
* state engines
* XML protocols
* multi-pane semantic composition

Those systems can be layered on after the browser bridge is stable.

---

# Recommended Technology Stack

## Language

Java 21+

---

## Browser Framework

JCEF (Java Chromium Embedded Framework)

Reasoning:

* Chromium compatibility
* modern web app support
* reliable rendering of ChatGPT
* better long-term maintainability
* easier future DOM integration

---

## UI Framework

Swing or JavaFX

Initial recommendation:

* Swing for simplicity
* JavaFX optional later

The UI should remain extremely lightweight during MVP development.

---

# MVP Architecture

```text
Story Workstation MVP
├─ Embedded Chromium Browser
├─ Hardcoded Chat URL
├─ Native Input Window
├─ Native Output Window
└─ Minimal DOM Interaction Layer
```

---

# Initial Workflow

## Application Startup

When the application launches:

1. Start embedded Chromium browser
2. Navigate to ChatGPT
3. User logs in manually if necessary
4. Browser session persists between launches
5. Application navigates automatically to:

    * a hardcoded project
    * a hardcoded chat

Example target:

```text
https://chatgpt.com/c/project-id/chat-id
```

The initial version should not support dynamic project selection.

---

# User Interface

## Initial Layout

```text
+--------------------------------------------------+
|                 Story Output                     |
|                                                  |
|  Assistant response appears here                 |
|                                                  |
|                                                  |
+--------------------------------------------------+

+--------------------------------------------------+
| Input                                            |
|                                                  |
| (I sit beside her) "How was rehearsal?"          |
|                                                  |
+--------------------------------------------------+

[ Send ]
```

---

# Input Behavior

User types into the native input box.

Example:

```text
(I sit beside her) "How was rehearsal?"
```

User presses:

* Enter
  or
* Send button

---

# Wrapper Responsibilities

The application then:

1. Finds the ChatGPT input textarea
2. Injects the user text
3. Triggers message send
4. Waits for assistant completion
5. Extracts assistant response text
6. Displays the response in the native output window

---

# Initial Output Goals

The output window should initially display:

* only the latest assistant message
* plain text only
* no formatting requirements
* no parsing requirements

No metadata extraction is required yet.

No XML or tagged response structure is required yet.

---

# Critical Unknowns To Validate

The MVP exists primarily to validate several technical unknowns.

---

## Unknown #1

Can ChatGPT operate reliably inside embedded Chromium?

---

## Unknown #2

Can the wrapper reliably:

* locate the input field
* inject text
* trigger sends
* detect completion
* extract assistant responses

---

## Unknown #3

Can browser login/session persistence work reliably?

---

# Browser Interaction Strategy

## Important Principle

Avoid fragile automation.

Do NOT rely on:

* simulated mouse movement
* pixel matching
* coordinate clicking
* screen scraping

Instead:

* use DOM interaction
* use stable selectors
* interact directly with browser page structures

---

# Required Browser Interactions

The MVP only requires four browser operations.

---

## 1. Find Input Textarea

Example concept:

```javascript
document.querySelector("textarea")
```

---

## 2. Insert User Text

Programmatically set textarea value and dispatch input events.

---

## 3. Trigger Send

Possible methods:

* submit form
* click send button
* simulate Enter key

---

## 4. Extract Latest Assistant Message

Read the latest assistant response node from the DOM.

---

# Minimal Development Phases

## Phase 1 — Embedded Browser

Goals:

* JCEF window
* load ChatGPT
* persistent login
* navigate to hardcoded chat

Success Criteria:

* ChatGPT works normally inside application

---

## Phase 2 — Native Input

Goals:

* add native input textbox
* send prompts programmatically

Success Criteria:

* prompts appear correctly in ChatGPT

---

## Phase 3 — Response Extraction

Goals:

* detect assistant response completion
* extract latest assistant response
* display in native output panel

Success Criteria:

* output window mirrors latest assistant response

---

## Phase 4 — Basic Quality of Life

Possible additions:

* auto-scroll
* resend
* retry
* prompt history
* editable prompt buffer
* response copy button

---

# Explicit Non-Goals For MVP

The MVP should NOT initially include:

* semantic panes
* dialogue/action separation
* XML parsing
* story-only rendering
* relationship tracking
* correction engine
* continuity systems
* upload profiles
* session archival
* state management
* metadata visualization
* project browser
* prompt templates
* canon database

These systems depend on proving the browser bridge first.

---

# Success Criteria

The MVP is considered successful if:

1. The application can reliably load ChatGPT
2. The user can remain logged in persistently
3. The wrapper can send prompts
4. The wrapper can retrieve responses
5. The native UI can display the generated text separately from the browser interface

If these capabilities work reliably, the Story Workstation concept is technically viable.

---

# Long-Term Perspective

The MVP is not intended to resemble the final system.

It exists only to establish a reliable communication bridge between:

* the native desktop application
* the embedded browser
* the ChatGPT web application

Once that bridge is stable, higher-level storytelling systems can be developed incrementally.
