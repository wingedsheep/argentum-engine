# Target Architecture Specification

The platform consists of three decoupled modules. This design ensures that the game rules can evolve independently of
the server infrastructure or the graphical interface.

---

## 1. Module: Rules Engine (`rules-engine`)

**Role:** The Domain Logic Kernel.
**Status:** Pure Kotlin Library (No Frameworks, No I/O).

This module is a deterministic state machine. It contains the complete "brain" of Magic: The Gathering. It does not know
about players, sockets, or databases; it only knows about Game States and Actions.

### Core Architecture: Entity Component System (ECS)

* **Entities:** Every game object (Card, Player, Ability, Spell on Stack) is just a unique ID.
* **Components:** Data is attached to entities via components (e.g., `TappedComponent`, `FlyingComponent`,
  `OwnerComponent`). This composition allows for infinite flexibility in card design.
* **Immutable State:** The `GameState` object is immutable. Every action produces a *new* `GameState`. This enables
  features like "Undo," AI simulation (Monte Carlo Tree Search), and instant replay.

### Key Sub-Systems

* **The Layer System (CR 613):** A robust projection engine that calculates the "visible" state of cards by applying
  continuous effects in the correct order (Copy -> Control -> Text -> Type -> Color -> Ability -> P/T).
* **The Stack & Priority:** A strictly ordered state machine that manages the "Stack" zone, ensuring priority is passed
  correctly between players before anything resolves.
* **Event & Replacement Pipeline:** A middleware layer that intercepts events (like "Draw Card") and checks for
  modification effects (like "Dredge" or "Spirit of the Labyrinth") before applying them.
* **Complex Mana Solver:** A logic solver that can determine if a player can pay a cost like `{1}{G/W}{P}` given their
  current board state, handling snow, life-payment, and hybrid mana automatically.

### Extensibility

* **Card Scripting DSL:** Cards are defined in Kotlin scripts or DSL files. This allows new sets to be added by simply
  dropping in new script files without recompiling the core engine.
* **Scryfall Integration:** An adapter layer that hydrates card definitions (Oracle text, stats) directly from Scryfall
  data.

---

## 2. Module: Game Server (`game-server`)

**Role:** The Infrastructure & Orchestrator.
**Status:** Spring Boot 4.x Application.

This module wraps the Rules Engine and provides the infrastructure for multiplayer gaming. It handles the "reality" of
running a game service.

### Core Responsibilities

* **Game Session Management:** It hosts thousands of concurrent `GameEngine` instances in memory. It routes incoming
  user actions to the correct engine instance.
* **Identity & Persistence:**
* **PostgreSQL:** Stores user profiles, deck lists, match history, and card collection data.
* **Keycloak:** Handles OAuth2 login (Google) and user sessions.


* **Matchmaking:** A queuing system that pairs players based on format (Standard, Draft, Commander) and skill level (
  ELO).

### The Security Layer: State Masking

The server acts as the source of truth and prevents cheating.

* **The Problem:** The `GameState` contains all info, including the opponent's hand and the order of the library.
* **The Solution:** Before sending an update to *Player A*, the server runs a **State Masker**. This removes or
  obfuscates all private information belonging to *Player B*. The client literally never receives the data for the
  opponent's hand.

### Drafting Engine

A specialized sub-system for Booster Drafts.

* It manages "Pods" of up to 8 players. You can also draft with friends if you create a room.
* It handles the logic of passing packs (left/right).
* It generates card pools from the set data and converts the final pool into a deck validation checklist.

---

## 3. Module: Web Client (`web-client`)

**Role:** The Visualizer.
**Status:** Browser-based Application (WebGL).

The client is a "dumb terminal." It makes no decisions and calculates no rules. Its only job is to display the state it
receives and capture user intent.

### Visual Experience (MTG Arena-like)

* **Tabletop:** Built with WebGL (Three.js/Babylon.js). Cards are visual objects that can fly, tap, and stack.
* **Asset Streaming:** Card art is streamed on-demand from a CMS or Scryfall to keep the initial load time low.
* **Animations:** The client interprets `GameEvents` (e.g., `DamageDealt`, `CardDrawn`) to trigger particle effects,
  sound effects, and card movements.

### User Interaction

* **Contextual Input:** The client knows what mode the engine is in (e.g., "Declare Attackers"). It highlights valid
  objects (untapped creatures) and disables invalid ones.
* **Action Dispatch:** When a player makes a move (drags an arrow from a Spell to a Target), the client sends a
  structured JSON message to the server: `TargetSelected { source: 101, target: 202 }`.

---

# System Interaction Flow

Here is how the end-state system handles a standard gameplay loop:

1. **Match Start:** Two players match via the **Game Server**. The server initializes a new **Rules Engine** instance,
   loads the players' decks from **PostgreSQL**, and shuffles them.
2. **State Broadcast:** The server masks the initial state for each player and pushes it via WebSocket. The **Web Client
   ** renders the opening hand.
3. **Player Action:** Player A plays a Land. The Client sends `PlayLand(CardID)` to the server.
4. **Verification:** The Server passes the action to the **Rules Engine**.

* The Engine checks: *Is it their turn? Is the stack empty? Have they played a land yet?*
* If Valid: The Engine updates the state (moves card Hand -> Battlefield).
* If Invalid: The Engine returns an error, which the server relays to the client.


5. **Update Cycle:**

* The Engine emits events: `ZoneChange(Hand -> Battlefield)`.
* The Server masks the new state and broadcasts the events + state delta to both clients.


6. **Visuals:**

* Player A's client animates the card moving from hand to play.
* Player B's client animates a "face down" card moving from the opponent's hand area to the battlefield, revealing it as
  it lands.
