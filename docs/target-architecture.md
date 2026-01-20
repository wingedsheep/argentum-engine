# Target Architecture Specification: Argentum Engine

The Argentum Engine is a data-driven, modular Magic: The Gathering simulation and networking platform. It employs a pure **Entity-Component-System (ECS)** core, a **stateless decision protocol**, and a **declarative scripting layer** to ensure the game is 100% persistent, horizontally scalable, and infinitely extensible.

---

## 1. Module: Rules Engine (`rules-engine`)

**Role:** The Domain Logic Kernel.  
**Status:** Pure Kotlin 2.2 Library (Deterministic, I/O-free, Framework-agnostic).

The Rules Engine acts as a deterministic state machine. It treats game rules as a pure transformation function: `(GameState, Action) -> Result(NewGameState, Events)`.

### 1.1 Core Architecture: Entity Component System (ECS)
*   **Entities:** Every game object (Card, Player, Ability, Spell on Stack, Emblem) is a unique, stable `EntityId`.
*   **Components:** Small, immutable data bags (e.g., `LifeComponent`, `TappedComponent`, `ManaPoolComponent`).
*   **Immutable State:** The `GameState` is a serializable snapshot of all entities. Every action produces a *new* state, enabling trivial undo/redo and AI simulation.

### 1.2 State Projection & The Layer System (CR 613)
The engine distinguishes between the **Base State** (stored in components) and the **Projected State** (how the card actually looks).
*   **State Projector:** A robust engine that applies continuous effects in the strict order required by the Comprehensive Rules (Copy → Control → Text → Type → Color → Ability → P/T).
*   **GameObjectView:** A calculated, read-only view of an entity used for rule checks (e.g., "Is this creature currently a Goblin?" or "Does it have flying?").

### 1.3 Stack, Priority, and Mana
*   **Priority State Machine:** Manages the passing of priority, ensuring the active player acts first and the stack resolves only when all players pass in succession.
*   **Complex Mana Solver:** A logic engine that determines if a player can pay costs like `{1}{G/W}{P}`, handling snow mana, life-payment, and hybrid mana automatically.
*   **Replacement Pipeline (CR 614):** A middleware layer that intercepts "Proposed Actions" (e.g., Draw Card) and checks for modification effects (e.g., "instead, draw two") before they hit the state.

---

## 2. Declarative Scripting & Modular Content

The engine is designed for horizontal growth. Adding a new set does not require modifying the Rules Engine kernel.

### 2.1 Kotlin DSL Scripting
Card behaviors are defined using a declarative Kotlin DSL. This DSL converts human-readable text into atomic, serializable `Effect` and `Trigger` data structures.
*   **Modular Reusability:** Effects (e.g., `DealDamage`, `SearchLibrary`) are modular atoms. New mechanics are created by composing these atoms.
*   **Set Isolation:** Each expansion is implemented as a standalone package containing its own `CardRegistry`.
*   **Scryfall Integration:** Card metadata (name, cost, type line) is hydrated via Scryfall data, while logic is defined in the script.

### 2.2 Stateless Decision Protocol
To support persistence and crash recovery, the engine is **stateless**.
*   **Pause & Resume:** When an effect requires input (e.g., "Choose a target"), the engine returns a `GameState` containing a `PendingDecision` and a `DecisionContext` (metadata defining where the script paused).
*   **Persistence:** The entire logic state—even in the middle of a complex card's text—is saved to the database. There are no memory-resident lambdas or callbacks.

---

## 3. Module: Game Server (`game-server`)

**Role:** The Infrastructure & Orchestrator.  
**Status:** Spring Boot 4.x Application.

The server acts as a **"Pure Pipe."** It manages the reality of networking and identity while delegating all logic to the Rules Engine.

### 3.1 Responsibilities
*   **Session Management:** Hosts concurrent matches, routing incoming JSON messages to the correct engine instance.
*   **Stateless Integration:** For every message, the server loads the `GameState` from **PostgreSQL**, runs the logic, saves the new state, and broadcasts.
*   **Matchmaking & Auth:** Manages queues (Draft, Commander, Standard) using ELO-based matchmaking. Handles OAuth2/Keycloak login.
*   **Drafting Engine:** A specialized sub-system for Pods of up to 8 players, handling pack generation and passing logic.

### 3.2 The Security Layer: State Masking
*   **The Fog of War:** The server is the source of truth. Before sending an update to Player A, it runs a `StateMasker` to strip private information. Opponent hands and library orders are replaced with opaque "hidden" identifiers.

---

## 4. Module: Web Client (`web-client`)

**Role:** The Visualizer.  
**Status:** Browser-based WebGL Application.

The client is a **"Dumb Terminal."** It contains zero rule logic, making it impossible to cheat by modifying local code.

### 4.1 Visual Experience
*   **Rendering:** Built with WebGL (Three.js/Babylon.js). Cards are objects that fly, tap, and stack.
*   **Event-Driven Animations:** The client listens for `GameEvents` (e.g., `DamageDealt`, `PermanentTapped`) to trigger particle effects and sound cues.
*   **Asset Streaming:** Card art is fetched on-demand from a high-speed CDN or Scryfall API.

### 4.2 User Intent Capture
*   **Contextual Input:** The server provides a list of `legalActions` and `targetRequirements`. The client uses this to highlight valid targets and prevent illegal moves before they are even sent.
*   **Action Dispatch:** Captures user moves (e.g., dragging an arrow from a spell to a target) and sends structured `SubmitAction` or `SubmitDecision` JSON to the server.

---

## 5. Interaction Flow: The Decision-Response Loop

1.  **Match Start:** Two players match. The server initializes a new **Rules Engine**, loads decks from the DB, and shuffles.
2.  **State Broadcast:** The server masks the state for each player and pushes it via WebSocket.
3.  **Proactive Action:** Player A sends `SubmitAction(CastSpell)`.
    *   The Server loads state, identifies the action.
    *   The Rules Engine moves the card to the stack and checks for a required target.
    *   **The Engine Pauses:** It returns a `PendingDecision(ChooseTarget)`.
4.  **Persistence:** The Server saves the `GameState` (with the decision context) to PostgreSQL.
5.  **Reactive Decision:** Player A receives the request, clicks a creature, and sends `SubmitDecision(TargetID)`.
6.  **Resolution:**
    *   The Server re-hydrates the state.
    *   The Rules Engine sees the ID, resumes the script, and executes the effect.
    *   The Engine emits `GameEvents`.
    *   The Server masks the results and broadcasts the final update to all clients.
