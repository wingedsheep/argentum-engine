# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Argentum Engine is a Magic: The Gathering rules engine and online play platform implemented in Kotlin using a pure
Entity-Component-System (ECS) architecture. The engine models game state immutably and exposes a pure functional API for
state transitions.

**Tech Stack:**

- **Backend:** Kotlin 2.2, Gradle 8.7, JDK 21, Kotest (testing), Spring Boot 4.x
- **Frontend:** React 18+, TypeScript 5, Zustand, Vite

## Build Commands

Using `just` (recommended):

```bash
just build          # Build entire project
just test           # Run all tests
just test-rules     # Run rules-engine tests only
just test-server    # Run game-server tests only
just test-class CreatureStatsTest  # Run specific test class
just server         # Start game server
just client         # Start web client dev server
just client-install # Install web client dependencies
just clean          # Clean build artifacts
```

Using Gradle directly:

```bash
./gradlew build
./gradlew test
./gradlew :rules-engine:test
./gradlew :rules-engine:test --tests "CreatureStatsTest"
./gradlew :rules-engine:test --tests "CreatureStatsTest.should apply modifier"
./gradlew :game-server:bootRun
```

Web client:

```bash
cd web-client && npm install
cd web-client && npm run dev        # Dev server at localhost:5173
cd web-client && npm run build      # Production build
cd web-client && npm run typecheck  # Type checking
```

## Architecture

### Module Structure

| Module           | Type            | Purpose                                      |
|------------------|-----------------|----------------------------------------------|
| **rules-engine** | Kotlin Library  | Core MTG rules (zero server dependencies)    |
| **game-server**  | Spring Boot App | Game orchestration, WebSocket, state masking |
| **web-client**   | React App       | browser UI (dumb terminal, no game logic)    |
| **utils**        | Kotlin Library  | Shared utilities                             |

### ECS Model

Everything is an entity identified by `EntityId`. Components are pure data bags attached to entities. Systems contain
all logic.

- **Entities:** Players, cards, permanents, spells, abilities, tokens, emblems, continuous effects
- **Components:** Data-only classes in `ecs/components/` (e.g., `TappedComponent`, `ControllerComponent`,
  `DamageComponent`)
- **Systems:** `ActionHandler` (state reducer), `StateProjector` (Rule 613 layers), `TriggerDetector` (event listener)

### Immutable State

`GameState` is immutable. Every action produces a new state plus events:

```
(GameState, GameAction) -> (GameState, List<GameEvent>)
```

### Rule 613 Layer System

Base state is stored; projected state is calculated by applying modifiers in layer order:

1. Copy effects
2. Control-changing
3. Text-changing
4. Type-changing
5. Color-changing
6. Ability-adding/removing
7. P/T modifications (7a-7e sublayers)

The `StateProjector` applies all continuous effects and returns `GameObjectView` for rule checks. Effects within the
same layer are sorted by dependencies (Rule 613.8) using trial application before timestamp ordering.

### Key Files (rules-engine)

| File                           | Purpose                                             |
|--------------------------------|-----------------------------------------------------|
| `ecs/GameState.kt`             | Immutable game state data class                     |
| `ecs/action/ActionHandler.kt`  | Processes GameActions, produces new state + events  |
| `ecs/action/GameAction.kt`     | Sealed interface of all action types                |
| `ecs/layers/StateProjector.kt` | Rule 613 continuous effect projection               |
| `ecs/event/TriggerDetector.kt` | Detects triggered abilities from events             |
| `ecs/components/`              | All component types (identity, state, combat, etc.) |
| `ability/CardScript.kt`        | Card behavior definitions                           |
| `sets/portal/PortalSet.kt`     | Portal set card registry                            |

## Card Implementation Pattern

Cards are defined as data + scripts, not class inheritance:

1. Create `CardDefinition` with name, cost, types, P/T
2. Write `CardScript` defining abilities using the DSL
3. Register in set file (e.g., `PortalSet.kt`)
4. Test with scenario tests

Example card script:

```kotlin
val script = cardScript("Giant Growth") {
    val creature = targets(TargetCreature)
    spell(
        ModifyStatsEffect(3, 3, ContextTarget(creature.index))
    )
}
```

Use `then` operator for composite effects:

```kotlin
ReturnToHandEffect(target) then DiscardCardsEffect(1, EffectTarget.TargetController)
```

## Key Concepts

- **ComponentContainer:** Immutable map holding entity's components, accessed via `container.get<TappedComponent>()`
- **Tag Components:** Data objects for binary state (e.g., `data object TappedComponent : Component`)
- **Effect Handlers:** In `ecs/script/handler/`, execute specific effects (damage, draw, destroy, etc.)
- **Continuous Effects:** Floating modifiers created by spells/abilities, applied during state projection
- **CompositeEffect:** Chain effects with `then` operator for multi-step abilities
- **Replacement Effects:** Interceptors that modify actions before execution (e.g., Doubling Season)
- **Dependency System:** Effects in same layer sorted by dependencies via trial application (Rule 613.8)

## Web Client Architecture

The web client follows the **dumb terminal** pattern:

- **No game rules** - Server validates all actions
- **No state computation** - Server sends complete game state
- **Intent capture only** - Client sends player clicks/selections

Data flow: Server sends `stateUpdate` with game state, events, and legal actions. Client renders state and sends
`submitAction` when user interacts.

## Documentation

Detailed architecture docs in `docs/`:

| Document                                 | Topic                                   |
|------------------------------------------|-----------------------------------------|
| `the-perfect-mtg-ecs-architecture.md`    | Core ECS philosophy and design          |
| `card-scripts-and-composite-actions.md`  | Card DSL and effect composition         |
| `continuous-effect-dependency-system.md` | Layer dependencies (Rule 613.8)         |
| `managing-complex-and-rare-abilities.md` | Patterns for complex card abilities     |
| `player-input.md`                        | Async I/O and decision protocol         |
| `web-client-architecture.md`             | Frontend architecture and WebSocket API |
| `target-architecture.md`                 | System topology and modularity          |
| `data-contracts.md`                      | JSON payloads between client and server |
| `api-guide.md`                           | Guide for adding cards/mechanics        |