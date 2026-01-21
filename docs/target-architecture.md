# Argentum System Architecture

This document outlines the high-level architecture of the Argentum platform. The system is designed to be **modular**, *
*stateless**, and **secure**, treating the Game Engine as the single source of truth for all logic.

## 1. Top-Level Topology

The system consists of three distinct deployable artifacts (or modules):

1. **`rules-engine` (Shared Library)**: The Pure Logic Kernel.
2. **`game-server` (Backend Application)**: The Orchestrator & Host.
3. **`web-client` (Frontend Application)**: The Visualizer.

---

## 2. Module Responsibilities

### A. rules-engine (The Brain)

*Type: Kotlin Library (JAR)*

This is a pure logic library. It has **no** knowledge of databases, networks, users, or time. It is deterministic:
`f(State, Action) = NewState`.

* **Logic:** Contains the Entity-Component-System (ECS), The Stack, Combat calculations, and Layer System (CR 613).
* **Validation:** It is the **sole authority** on what a player can do. It exposes a `LegalActionCalculator`.
* **Modularity:** It defines the `CardRegistry` interface. It does **not** contain all cards. Sets (e.g., `set-portal`,
  `set-alpha`) are separate modules that implement `CardRegistry` and are loaded by the engine.

### B. game-server (The Host)

*Type: Spring Boot Application*

This handles the "Real World." It wraps the `rules-engine` and manages persistence and networking.

* **Drafting:** Manages the low-frequency state of Draft Lobbies (pack generation, passing logic, saving decks). Uses
  REST APIs.
* **Gameplay:** Hosts the active in-memory `GameState`. Manages the WebSocket connections.
* **State Masking:** Before sending state to the Client, it runs `StateMasker` to hide opponent hands and library
  cards (Fog of War).
* **User Mgmt:** Handles Authentication and matchmaking (connecting Friends).

### C. web-client (The Dumb Terminal)

*Type: React / Three.js Application*

This is a visualization layer. It contains **zero** game rules.

* **Rendering:** Projects the JSON `ClientGameState` onto a 3D table.
* **Animation:** Listens for an `events` array to trigger visual cues (particles, movement).
* **Action Capture:** Renders `legalActions` provided by the server. If the server says "Card A is playable", the client
  draws a green glow. If the user clicks it, the client sends a message. It does **not** calculate if the move is valid.

---

## 3. Modularity Strategy

To prevent the engine from becoming a monolith of 25,000 cards, we use a Registry pattern.

1. **`rules-engine-core`**: Contains base mechanics (Flying, Trample, Instant, Land).
2. **`set-xxx` modules**: (e.g., `set-portal`) depend on core. They contain Kotlin files defining `CardDefinition` and
   Scripts.
3. **Loader**: On startup, `game-server` scans the classpath for classes implementing `CardRegistry` and registers them
   into the Engine.

```kotlin
interface CardRegistry {
    val setCode: String
    fun getCards(): List<CardDefinition>
}
```

This allows us to ship updates (new sets) by simply dropping a new JAR file into the server classpath without
recompiling the core engine.

```

---

### 2. `docs/data-contracts.md`
*Defines exactly what data moves between the layers.*

```markdown

```

```

---

### 3. `docs/api-guide.md`
*A quick developer guide on how to add new features.*

```markdown
# Developer Guide: How to Extend Argentum

## 1. Adding a New Card Set
You do **not** modify `rules-engine` core to add cards.

1.  Create a new Gradle module: `sets/set-alpha`.
2.  Implement `CardRegistry`:
    ```kotlin
    object AlphaSet : CardRegistry {
        override val setCode = "LEA"
        override fun getCards() = listOf(
            BlackLotus.definition,
            GiantGrowth.definition
        )
    }
    ```
3.  Implement Card Scripts using the DSL:
    ```kotlin
    val BlackLotus = card("Black Lotus") {
        type("Artifact")
        activatedAbility("{T}, Sacrifice {self}") {
            effect(AddMana.ofAnyColor(3))
        }
    }
    ```
4.  Add the module as a dependency to `game-server`. The loader will find it automatically.

## 2. Adding a New Mechanic (e.g., Scry)
This requires modifying `rules-engine`.

1.  **Define the Effect:** Add `ScryEffect(amount: Int)` to `Effect.kt`.
2.  **Write the Handler:** Create `ScryHandler.kt`.
    *   This handler needs to return a `PendingDecision` because the user needs to choose top/bottom orders.
3.  **Register the Handler:** Add to `EffectHandlerRegistry`.
4.  **Update Client (Optional):** If `Scry` requires a unique UI (like ordering cards visually), update `web-client` to handle the `DecisionRequest` for reordering.

## 3. Deployment Strategy
For "Me and my friends":

1.  **Build:** Run `./gradlew build`. This produces `game-server.jar`.
2.  **Build Frontend:** Run `npm run build` in `web-client`.
3.  **Dockerize:** Create a Docker Compose file.
    *   `postgres`: Database for decks/users.
    *   `app`: Runs the `game-server.jar`.
    *   `nginx`: Serves the static `web-client` files and proxies `/api` and `/ws` to the app container.
