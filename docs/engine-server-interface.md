# Engine <-> Server Interface Specification

This document defines the API contract between the **Host Application** (`mtg-api`) and the **Logic Kernel** (
`mtg-engine`).

## 1. Interaction Model

The Engine is included as a **Library (JAR)** within the Server. Communication occurs via **synchronous method calls**on
the JVM.

* **Statelessness:** The Engine does not hold state between calls. The Server must pass the current `GameState` into
  every request.
* **Immutability:** The Engine never modifies the `GameState` in place. It returns a *new* `GameState`.
* **Determinism:** Given `State A` and `Request B`, the Engine always produces `Result C`.

### The Core Interface

The Server interacts primarily through the `ActionProcessor` interface.

```kotlin
interface ActionProcessor {
    /**
     * Applies a player's intent to the current game state.
     * @param state The current immutable snapshot of the game.
     * @param request The intent (e.g., Cast Spell, Pass Priority).
     * @return A sealed result containing the NEW state, events, or failure reason.
     */
    fun process(state: GameState, request: GameRequest): EngineResult
}

```

---

## 2. The Data Model: `GameState`

The `GameState` is the immutable snapshot of the entire universe at a specific tick. It is an *
*Entity-Component-System (ECS)** database.

### Root Structure

```kotlin
data class GameState(
    val gameId: String,
    val turnInfo: TurnInfo,
    val entities: Map<EntityId, ComponentContainer>, // The Database
    val zones: Map<ZoneId, List<EntityId>>,          // The Locations
    val players: Map<EntityId, PlayerState>          // Player-specific aggregates
)

```

### A. Turn Information (`TurnInfo`)

Tracks the temporal state of the game.

```kotlin
data class TurnInfo(
    val turnNumber: Int,
    val activePlayerId: EntityId,   // Whose turn is it?
    val priorityPlayerId: EntityId, // Who can act *right now*?
    val phase: Phase,               // e.g., COMBAT
    val step: Step,                 // e.g., DECLARE_ATTACKERS
    val stackIsEmpty: Boolean
)

```

### B. The Entity Database (`entities`)

A flat map of every object in the game (Cards, Tokens, Emblems, Spells on Stack).

* **Key:** `EntityId` (UUID)
* **Value:** `ComponentContainer` (A collection of data facets)

**Common Components:**

* `CardIdentity`: References the Oracle ID (e.g., `por-grizzly-bears`).
* `Controller`: Who controls this object?
* `Tapped`: Boolean flag.
* `PowerToughness`: Current base stats + modifiers + counters.
* `ManaCost`: Cost to cast.

### C. Zones (`zones`)

Maps a Zone Identifier to an ordered list of Entity IDs.

```kotlin
data class ZoneId(
    val type: ZoneType,       // HAND, LIBRARY, BATTLEFIELD, STACK, GRAVEYARD, EXILE
    val ownerId: EntityId?    // Null for Shared zones (Battlefield, Stack)
)

```

### D. Player State (`PlayerState`)

Derived/cached data for quick access.

```kotlin
data class PlayerState(
    val life: Int,
    val poisonCounters: Int,
    val manaPool: ManaPool, // { W: 0, U: 2, B: 0, R: 1, G: 0, C: 0 }
    val landsPlayedThisTurn: Int,
    val hasLost: Boolean
)

```

---

## 3. Input: Game Requests

The `GameRequest` sealed interface represents every possible mutation a player can attempt.

### Common Fields

Every request includes:

* `requestId` (String/UUID): For logging and idempotency.
* `playerId` (EntityId): The actor initiating the request.
* `clientMetadata` (Map<String, String>): Optional key-value pairs for UI tracking (e.g.,
  `{"source": "drag-drop", "clientTime": "12345"}`).

### Request Types

#### A. `CastSpell`

Used to play a card from Hand, Graveyard, Exile, or Command Zone.

| Field             | Type                    | Description                                                                                                                     |
|-------------------|-------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `cardId`          | `EntityId`              | The UUID of the card being cast.                                                                                                |
| `targets`         | `List<TargetSelection>` | Targeted objects (e.g., "Bolt targets Bird").                                                                                   |
| `modeIndex`       | `Int?`                  | For modal spells (e.g., "Choose one..."). `null` if not modal.                                                                  |
| `xValue`          | `Int?`                  | The declared value for `{X}` costs (e.g., Fireball).                                                                            |
| **`chosenCosts`** | **`Map<Int, Int>`**     | **Modular Optional Costs.** Maps the index of the optional cost definition to times paid. (e.g., Kicker, Buyback, Multikicker). |
| `payment`         | `ManaPaymentStrategy?`  | (Optional) Explicit instructions on how to pay (e.g., "Use Treasure token").                                                    |

#### B. `ActivateAbility`

Used to use an activated ability of a permanent (or card in hand/graveyard).

| Field          | Type                    | Description                                               |
|----------------|-------------------------|-----------------------------------------------------------|
| `sourceId`     | `EntityId`              | The entity having the ability.                            |
| `abilityIndex` | `Int`                   | 0-based index of the ability on the card (top to bottom). |
| `targets`      | `List<TargetSelection>` | Required targets for the ability.                         |
| `chosenCosts`  | `Map<Int, Int>`         | For optional additional costs in abilities.               |
| `payment`      | `ManaPaymentStrategy?`  | Explicit payment instructions.                            |

#### C. `PlayLand`

Used to perform the special action of playing a land.

| Field    | Type       | Description            |
|----------|------------|------------------------|
| `cardId` | `EntityId` | The land card to play. |

#### D. `PassPriority`

Used to yield the turn or allow the top stack item to resolve.

| Field    | Type | Description                |
|----------|------|----------------------------|
| *(None)* |      | Only common fields needed. |

#### E. `SubmitDecision`

Used to answer a `PendingDecision` (resuming a paused engine).

| Field         | Type             | Description                                              |
|---------------|------------------|----------------------------------------------------------|
| `decisionId`  | `String`         | Must match the ID from `EngineResult.PausedForDecision`. |
| `choiceIndex` | `Int?`           | For multiple-choice questions (e.g., "Yes/No", "Color"). |
| `selectedIds` | `List<EntityId>` | For entity selection (e.g., "Discard 2 cards").          |

---

## 4. Output: Engine Results

The engine returns a sealed `EngineResult` object. It **never throws exceptions** for game logic violations.

### A. `Success`

The action was valid and completed.

```kotlin
data class Success(
    val newState: GameState,      // The updated immutable state
    val events: List<GameEvent>   // List of side effects for animation/logs
) : EngineResult

```

### B. `Failure`

The action violated a game rule. The state is unchanged.

```kotlin
data class Failure(
    val reason: FailureReason,    // Enum: MANA, TARGET, TIMING, RULE
    val message: String           // Human-readable debug message
) : EngineResult

```

### C. `PausedForDecision`

The engine started processing but hit a point requiring user input (e.g., resolving a Tutor, Scrying).

```kotlin
data class PausedForDecision(
    val partialState: GameState,  // State frozen in the middle of resolution
    val decision: PendingDecision // Data describing what input is needed
) : EngineResult

```

**The `PendingDecision` Data Structure:**

| Field         | Type             | Description                                           |
|---------------|------------------|-------------------------------------------------------|
| `id`          | `String`         | UUID for correlation.                                 |
| `playerId`    | `EntityId`       | Who must answer?                                      |
| `type`        | `DecisionType`   | `SELECT_CARDS`, `CHOOSE_MODE`, `ORDER_BLOCKERS`, etc. |
| `prompt`      | `String`         | Text to display (e.g., "Choose a card to discard").   |
| `options`     | `List<EntityId>` | Valid candidates (e.g., IDs of cards in hand).        |
| `min` / `max` | `Int`            | Constraints (e.g., "Select exactly 2").               |

### D. `GameOver`

The state machine has reached a terminal state.

```kotlin
data class GameOver(
    val finalState: GameState,
    val winnerId: EntityId?, // Null if Draw
    val reason: String       // "Life total", "Decked", "Poison", "Conceded"
) : EngineResult

```

---

## 5. Side Effects: Game Events

Events describe **what happened** during the transition. The Server forwards these to the Client to drive animations.

### Core Events

* **`ZoneChange`**: Card moved (Hand -> Stack, Stack -> Battlefield).
* **`LifeTotalChanged`**: Life points modified.
* **`DamageDealt`**: Source, Target, Amount, IsCombat.
* **`CountersChanged`**: Entity, Type, Delta.
* **`Tapped`/`Untapped**`: Entity status change.
* **`SpellCast`**: Spell added to stack.
* **`TurnPhaseChanged`**: Step progression.

---

## 6. Example Interaction Trace

**Scenario:** Player 1 casts *Burst Lightning* with Kicker targeting Player 2.

1. **Client** sends JSON (via WebSocket).
2. **Server** constructs `GameRequest`:

```kotlin
val req = CastSpell(
    playerId = "p1",
    cardId = "card-burst-lightning",
    targets = [TargetSelection(0, ["p2"])], // 0 = First target definition
    chosenCosts = { 0: 1 } // Pay "Option 0" (Kicker) exactly 1 time
)

```

3. **Engine** validates:

* Priority held? Yes.
* Card in hand? Yes.
* Target legal? Yes.
* Mana available? Needs {R} (Base) + {4} (Kicker) = {4}{R}. Checks pool.


4. **Engine** computes:

* Moves card to Stack.
* Auto-taps lands for {4}{R}.
* Applies `Kicked` status to the spell entity on stack.


5. **Engine** returns `Success`:

```kotlin
Success(
    newState = GameState(stack = [Entity(BurstLightning, isKicked = true)], ...
),
events = [
    ZoneChange("card-burst-lightning", HAND, STACK),
    Tapped("land-mountain-1"), ...,
SpellCast("card-burst-lightning")
]
)

```