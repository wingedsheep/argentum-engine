This document provides a comprehensive overview of the Argentum Engine's rules engine, detailing its architecture, code style, usage guidelines, and developer responsibilities.

---

# Rules Engine Documentation

## 1. Introduction

The Argentum Engine's rules engine is the core logic component responsible for simulating Magic: The Gathering gameplay. It adheres to a strict **data-driven** and **immutable state** architecture, ensuring deterministic game execution and seamless integration with other system modules.

---

## 2. Core Philosophy

The engine operates on a foundation of:

*   **Pure Logic:** Contains no card-specific knowledge; it executes generic rules based on data defined in `mtg-sdk` and `mtg-sets`.
*   **Immutability:** The `GameState` is an immutable data structure. All operations return new states, enabling features like undo/redo and reliable networking.
*   **Entity-Component-System (ECS):** Game objects (cards, players, etc.) are represented as `EntityId`s, augmented by data-rich `Component`s. Logic resides in `System`s (or Handlers).
*   **Separation of Concerns:** Distinct modules handle state management, rules application, effect execution, and event processing.

---

## 3. Architecture Overview

The rules engine is a standalone Kotlin library within the Gradle project, primarily located in the `rules-engine` module.

```text
rules-engine/
├── src/
│   └── main/
│       └── kotlin/
│           └── com/
│               └── wingedsheep/
│                   └── engine/
│                       ├── core/          # Core game loop, state transitions, event definitions
│                       ├── state/         # ECS data structures: GameState, Components, Zones
│                       ├── mechanics/     # Implementation of MTG rules (Layers, Combat, Stack)
│                       ├── handlers/      # Executes effects, costs, conditions etc.
│                       └── loader/        # Loads card sets dynamically
│
└── test/
    └── ...                            # Unit and integration tests
```

### Key Components:

*   **`GameState`:** The immutable, central data store representing the entire game state.
*   **Components:** Data bags attached to entities (players, cards) that define their attributes (e.g., `LifeTotalComponent`, `TappedComponent`).
*   **`ActionProcessor`:** The main entry point for all player actions, routing requests to the correct handler.
*   **`TurnManager`:** Manages phase and step transitions, turn order, and player availability.
*   **`StackResolver`:** Handles the game stack - spells, abilities, and their interactions.
*   **`StateProjector`:** Calculates the "effective" state of permanents by applying all continuous effects according to the Layer System (Rule 613). This is crucial for dynamic P/T, keywords, and type changes.
*   **`TriggerDetector` & `TriggerProcessor`:** Detects activated and triggered abilities based on game events and places them on the stack.
*   **Effect Executors:** Specific classes that implement the "HOW" for each `Effect` defined in `mtg-sdk`. Located in `handlers/effects/`.
*   **Cost/Condition Handlers:** Logic for validating and paying costs, and evaluating conditions.
*   **`SetLoader`:** Dynamically loads card definitions from the `mtg-sets` module using Java's `ServiceLoader`.

---

## 4. Code Style & Standards

*   **Kotlin Idioms:** Leverages Kotlin's features like data classes, sealed interfaces, extension functions, and coroutines (for asynchronous operations, though the core engine aims for synchronous processing within a turn).
*   **Immutability:** `GameState` and most internal state objects are immutable. Changes create new instances rather than modifying existing ones.
*   **Clarity & Readability:** Code is structured for clarity, with well-defined interfaces and classes.
*   **Modularity:** Handlers and executors are organized into modules (`ActionHandlerModule`, `ExecutorModule`) for better maintainability and extensibility.
*   **Testability:** Core logic is designed to be testable in isolation. Extensive unit and integration tests are crucial.
*   **`@Serializable`:** All data intended for network transfer (events, actions, state components) is annotated for KotlinX Serialization.
*   **`sealed interface`:** Used extensively for effects, actions, events, and decisions to ensure exhaustive handling by the engine's `when` statements.

---

## 5. How to Use the Rules Engine

The rules engine is typically used by the `mtg-api` module, which orchestrates the game flow and handles player interactions.

1.  **Initialization:**
    *   Use `SetLoader` to discover and load all available card sets.
    *   Instantiate `GameInitializer` with the `CardRegistry`.
    *   Call `initializeGame` with a `GameConfig` to create the initial `GameState`.

    ```kotlin
    val cardRegistry = CardRegistry()
    cardRegistry.register(AllSets.getAllCards()) // Load from mtg-sets

    val initializer = GameInitializer(cardRegistry)
    val initResult = initializer.initializeGame(GameConfig(
        players = listOf(PlayerConfig("Alice", deck1), PlayerConfig("Bob", deck2))
    ))
    var gameState = initResult.state
    ```

2.  **Processing Actions:**
    *   When a player action is received (e.g., from the web client), it's translated into a `GameAction`.
    *   Pass the current `GameState` and the `GameAction` to `ActionProcessor.process()`.

    ```kotlin
    val actionProcessor = ActionProcessor(cardRegistry = cardRegistry /* other dependencies */)
    val gameAction: GameAction = ... // Received from client

    val result = actionProcessor.process(gameState, gameAction)

    // Handle the result (Success, PausedForDecision, Failure, GameOver)
    gameState = when (result) {
        is ExecutionResult.Success -> result.newState
        is ExecutionResult.PausedForDecision -> {
            // Send decision to client
            sendDecisionToClient(result.decision, result.state)
            return // Wait for response
        }
        is ExecutionResult.Failure -> {
            // Handle error, inform client
            sendErrorToClient(result.message)
            return // Don't change state
        }
        is ExecutionResult.GameOver -> {
            // Handle game over
            handleGameOver(result)
            return
        }
    }
    ```

3.  **Handling Paused States:**
    *   If `process()` returns `PausedForDecision`, send the `PendingDecision` to the client.
    *   When the client response arrives, create a `SubmitDecision` action and process it.
    *   The `ContinuationHandler` will then resume the interrupted effect/ability.

4.  **Event Handling:**
    *   The `process()` method returns a list of `GameEvent`s describing state changes.
    *   The `mtg-api` module broadcasts these events to the web client for animation and UI updates.

---

## 6. Developer Responsibilities

### Adding New Cards:

*   Define cards in the `mtg-sets` module using the DSL provided by `com.wingedsheep.mtg.sdk.dsl`.
*   Leverage existing `Effect` and `Ability` types from the SDK.
*   If a new mechanic requires backend logic:
    *   **Define SDK Types:** Add new `Effect`, `Cost`, `Condition`, or `Trigger` types in `mtg-sdk`.
    *   **Implement Executor/Handler:** Create the corresponding logic in `rules-engine/src/main/kotlin/.../handlers/` (e.g., `MyNewEffectExecutor.kt`).
    *   **Register Executor/Handler:** Add the new executor/handler to the appropriate registry (`EffectExecutorRegistry`, `ActionHandlerRegistry`).
    *   **Define Card:** Implement the card in `mtg-sets` using the new SDK types.
    *   **Test:** Write scenario tests in `rules-engine/src/test/kotlin/.../scenarios/` to verify the new card/mechanic.

### Modifying Core Rules:

*   Requires deep understanding of the engine's architecture.
*   Changes impacting layers (Rule 613) involve modifying `StateProjector`.
*   Changes impacting stack resolution might involve `StackResolver` or custom `EffectExecutor`s.
*   New game actions require adding `GameAction` types, `ActionHandler`s, and registering them.
*   New triggered abilities require modifying `TriggerDetector` and potentially adding new `Trigger` types in the SDK.

### Testing:

*   Write comprehensive unit tests for individual components, handlers, and executors.
*   Write integration tests using `ScenarioTestBase` to simulate complex game interactions.
*   Ensure a high level of test coverage, especially for concurrency, edge cases, and rule interactions.

---

## 7. Persistence & Networking Considerations

*   **Immutability:** The immutable `GameState` simplifies serialization and network transmission. State updates can be sent as diffs or full state snapshots.
*   **`PendingDecision` & `ContinuationFrame`:** The engine's ability to pause execution and serialize the context (via `PendingDecision` and `ContinuationFrame`) is crucial for networked games, allowing clients to make decisions and the server to resume execution without blocking.
*   **Event Sourcing:** `GameEvent`s provide an audit trail of state changes, enabling game history replay and easier debugging.
