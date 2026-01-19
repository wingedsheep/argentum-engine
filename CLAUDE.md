# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Argentum Engine is a Magic: The Gathering rules engine implemented in Kotlin using a pure Entity-Component-System (ECS)
architecture. The engine models game state immutably and exposes a pure functional API for state transitions.

**Tech Stack:** Kotlin 2.2, Gradle 8.7, JDK 21, Kotest (testing)

## Build Commands

```bash
# Build entire project
./gradlew build

# Run all tests
./gradlew test

# Run tests in rules-engine module only
./gradlew :rules-engine:test

# Run a specific test class
./gradlew :rules-engine:test --tests "CreatureStatsTest"

# Run a specific test method
./gradlew :rules-engine:test --tests "CreatureStatsTest.should apply modifier"
```

## Architecture

### ECS Model

Everything is an entity identified by `EntityId`. Components are pure data bags attached to entities. Systems contain
all logic.

- **Entities:** Players, cards, permanents, spells, abilities, tokens, emblems
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

The `StateProjector` applies all continuous effects and returns `GameObjectView` for rule checks.

### Key Files

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

### Module Structure

- **rules-engine/** - Core MTG rules library (zero server dependencies)
- **app/** - Game server (Spring Boot, planned)
- **utils/** - Shared utilities

## Card Implementation Pattern

Cards are defined as data + scripts, not class inheritance:

1. Create `CardDefinition` with name, cost, types, P/T
2. Write `CardScript` defining abilities (triggered, activated, static)
3. Register in set file (e.g., `PortalSet.kt`)
4. Test with scenario tests

## Key Concepts

- **ComponentContainer:** Immutable map holding entity's components, accessed via `container.get<TappedComponent>()`
- **Tag Components:** Data objects for binary state (e.g., `data object TappedComponent : Component`)
- **Effect Handlers:** In `ecs/script/handler/`, execute specific effects (damage, draw, destroy, etc.)
- **Continuous Effects:** Floating modifiers created by spells/abilities, applied during state projection