# Argentum System Architecture

This document outlines the high-level architecture of the Argentum platform. The system is designed to be **modular**, **stateless**, and **secure**, treating the Game Engine as the single source of truth for all logic.

## 1. Top-Level Topology

The system is organized into a multi-module Gradle project with strict dependency rules:

1.  **`mtg-sdk` (Shared Contract)**: The primitives, data models, and DSLs. Dependencies: *None*.
2.  **`mtg-sets` (Content)**: The card definitions. Dependencies: *mtg-sdk*.
3.  **`mtg-engine` (Logic)**: The ECS rules engine. Dependencies: *mtg-sdk*.
4.  **`mtg-api` (Host Application)**: The Spring Boot server. Dependencies: *mtg-engine* and *mtg-sdk*.
5.  **`web-client` (Frontend)**: The React/Three.js visualizer. Dependencies: *None*.

---

## 2. Module Responsibilities

### A. mtg-sdk (The Vocabulary)
*Type: Kotlin Library*

Defines **what** a card is, but not **how** it works. It contains the DSL builders (`card { }`), Effect definitions (`data class DealDamage`), and Enums (`Phase`, `Zone`). It has zero dependencies.

### B. mtg-sets (The Content)
*Type: Kotlin Library*

Contains the actual card data. It uses the `mtg-sdk` to define sets like Portal, Alpha, or Custom sets.
* **Plug-in Architecture:** New sets are added here.
* **No Logic:** This module does not know about the Game Loop or Game State.

### C. mtg-engine (The Brain)
*Type: Kotlin Library*

The pure logic kernel. It is deterministic: `f(State, Action) = NewState`.
* **ECS:** Manages Entities and Components.
* **Rules:** Implements the Stack, Combat, and Layer System (CR 613).
* **Execution:** Maps SDK data objects (e.g., `Effect.DealDamage`) to logic (modifying life totals).
* **Loader:** Uses Java `ServiceLoader` to dynamically load content from `mtg-sets`.

### D. mtg-api (The Host)
*Type: Spring Boot Application*

The deployable artifact. It wraps the engine and handles the "Real World."
* **Orchestration:** Manages `GameService` instances.
* **Persistence:** Saves deck lists and user profiles to PostgreSQL.
* **Networking:** Exposes REST endpoints for deck building and WebSockets for gameplay.
* **State Masking:** Filters the `GameState` (Fog of War) before sending JSON to the client.

### E. web-client (The Dumb Terminal)
*Type: React / Three.js Application*

A visualization layer. It contains **zero** game rules.
* **Rendering:** Projects the JSON `ClientGameState` onto a table.
* **Action Capture:** Sends user intent (clicks) to `mtg-api`.

---

## 3. Data Flow

1.  **Input:** User clicks a card -> `web-client` sends `CastSpellRequest`.
2.  **API:** `mtg-api` receives request, authenticates user, finds the `GameEngine` instance.
3.  **Logic:** `mtg-engine` validates action, updates ECS state, triggers events.
4.  **Output:** `mtg-api` masks the new state and pushes `GameUpdate` JSON via WebSocket.
5.  **Render:** `web-client` updates the scene.
