# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Notes

- Focus on your own work. If a change not made by you breaks the build, report it to the user and stop your work. Don't
  try to revert or fix changes that are not assigned to you.

## Project Overview

Argentum Engine is a Magic: The Gathering rules engine and online play platform implemented in Kotlin using a pure
Entity-Component-System (ECS) architecture. The engine models game state immutably and exposes a pure functional API for
state transitions.

**Tech Stack:**

- **Backend:** Kotlin 2.2.20, Gradle 8.13, JDK 21, Kotest 5.9.1 (testing), Spring Boot 3.4.3
- **Frontend:** React 18.3, TypeScript 5.6, Zustand 5.0 (state), Vite 5.4 (build)

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

The project follows a strict **Domain-Driven Design (DDD)** with clear separation of concerns:

| Module           | Type            | Purpose                                            | Dependencies      |
|------------------|-----------------|----------------------------------------------------|-------------------|
| **mtg-sdk**      | Kotlin Library  | Shared contract - DSLs, data models, primitives    | None              |
| **mtg-sets**     | Kotlin Library  | Card definitions using SDK (Portal, Alpha, Custom) | mtg-sdk           |
| **rules-engine** | Kotlin Library  | Core MTG rules engine (zero server dependencies)   | mtg-sdk           |
| **game-server**  | Spring Boot App | Game orchestration, WebSocket, state masking       | rules-engine, sdk |
| **web-client**   | React App       | Browser UI (dumb terminal, no game logic)          | None              |

**Key Principle:** The engine is "pure" (no card-specific code), content is "data-driven" (no execution logic), and the
API provides an "anti-corruption layer" between engine internals and external clients.

### ECS Model

Everything is an entity identified by `EntityId`. Components are pure data bags. Systems contain all logic.

- **Entities:** Players, cards, permanents, spells, abilities, tokens, emblems, continuous effects
- **Components:** Data-only classes (e.g., `TappedComponent`, `ControllerComponent`, `DamageComponent`)
- **Systems:** `ActionProcessor` (state reducer), `StateProjector` (Rule 613 layers), `TriggerDetector` (event listener)

### Immutable State

`GameState` is immutable. Every action produces a new state plus events:

```
(GameState, GameAction) -> ExecutionResult(GameState, List<GameEvent>)
```

### Rule 613 Layer System

Base state is stored; projected state is calculated by applying continuous effects in layer order:

1. Copy effects
2. Control-changing
3. Text-changing
4. Type-changing
5. Color-changing
6. Ability-adding/removing
7. P/T modifications (7a-7e sublayers)

The `StateProjector` applies effects and handles **dependencies** (Rule 613.8) using trial application - effects within
the same layer are sorted by checking if applying one changes the outcome of another, before falling back to timestamp
ordering.

### Key Files (rules-engine)

| File                                         | Purpose                                        |
|----------------------------------------------|------------------------------------------------|
| `state/GameState.kt`                         | Immutable game state data class                |
| `core/ActionProcessor.kt`                    | Processes GameActions, produces new state      |
| `core/GameAction.kt`                         | Sealed interface of all action types           |
| `core/GameInitializer.kt`                    | Sets up initial game state                     |
| `core/TurnManager.kt`                        | Turn and phase progression                     |
| `mechanics/layers/StateProjector.kt`         | Rule 613 continuous effect projection          |
| `event/TriggerDetector.kt`                   | Detects triggered abilities from events        |
| `event/TriggerProcessor.kt`                  | Processes detected triggers                    |
| `state/components/`                          | All component types (identity, state, combat)  |
| `handlers/effects/EffectExecutorRegistry.kt` | Registry mapping effects to executors          |
| `handlers/effects/*/`                        | Effect executors (damage, life, drawing, etc.) |

### Key Files (mtg-sdk)

| File                         | Purpose                                        |
|------------------------------|------------------------------------------------|
| `model/CardDefinition.kt`    | Master card object (rules + metadata)          |
| `model/CardScript.kt`        | Container for game logic configurations        |
| `scripting/effect/Effect.kt` | Sealed interface of all effect types           |
| `scripting/cost/Cost.kt`     | Sealed interface of all cost types             |
| `dsl/CardBuilder.kt`         | DSL for defining cards via `cardDef { }`       |
| `spi/CardSetProvider.kt`     | Service provider interface for dynamic loading |

### Key Files (mtg-sets)

| File                              | Purpose                      |
|-----------------------------------|------------------------------|
| `provider/SetRegistry.kt`         | ServiceLoader implementation |
| `definitions/portal/PortalSet.kt` | Portal card definitions      |
| `definitions/alpha/AlphaSet.kt`   | Alpha card definitions       |

### Key Files (game-server)

| File                                | Purpose                                  |
|-------------------------------------|------------------------------------------|
| `session/GameSession.kt`            | Runtime game management and state        |
| `session/PlayerSession.kt`          | Per-player connection and session state  |
| `masking/StateMasker.kt`            | Hides private information (Fog of War)   |
| `masking/MaskedGameState.kt`        | Sanitized state for clients              |
| `websocket/GameWebSocketHandler.kt` | WebSocket message handling               |
| `protocol/ServerMessage.kt`         | Server-to-client message types           |
| `protocol/ClientMessage.kt`         | Client-to-server message types           |
| `dto/ClientDTO.kt`                  | Data transfer objects for client         |
| `dto/ClientStateTransformer.kt`     | Transforms engine state to client format |

## Card Implementation Pattern

Cards are defined as data + scripts using the **cardDef** DSL, not class inheritance:

1. Create `CardDefinition` with name, cost, types, P/T
2. Write `CardScript` defining abilities using the DSL
3. Register in set file (e.g., `PortalSet.kt`)
4. Test with scenario tests

Example card script:

```kotlin
val GiantGrowth = cardDef("Giant Growth") {
    manaCost = "{G}"
    typeLine = "Instant"

    spell {
        effect = Effects.ModifyStats(3, 3, Duration.EndOfTurn)
        target = TargetFilter.Creature
    }
}
```

**Composite Effects** - Use `then` operator for multi-step abilities:

```kotlin
spell {
    effect = Effects.ReturnToHand(TargetFilter.Permanent)
        .then(Effects.Discard(1, DiscardMethod.CHOOSE))
}
```

**Conditional Effects** - Use branching for "if you do" abilities:

```kotlin
triggeredAbility {
    trigger = Triggers.EntersBattlefield
    effect = Effects.Branch(
        condition = Conditions.TopCardMatches(TargetFilter.Land),
        ifTrue = Effects.PutTopCardOnBattlefield,
        ifFalse = Effects.DrawCards(1)
    )
}
```

**Reflexive Triggers** - Use for "When you do" abilities (see Heart-Piercer Manticore):

```kotlin
triggeredAbility {
    trigger = Triggers.EntersBattlefield
    effect = Effects.ReflexiveTrigger(
        action = Effects.Sacrifice(TargetFilter.Creature.other()),
        optional = true,
        reflexiveEffect = Effects.DealDamage(
            amount = Values.SacrificedCreaturePower,
            target = TargetFilter.Any
        )
    )
}
```

## Key Concepts

- **ComponentContainer:** Immutable map holding entity's components, accessed via `container.get<TappedComponent>()`
- **Tag Components:** Data objects for binary state (e.g., `data object TappedComponent : Component`)
- **Effect Executors:** In `handlers/effects/`, execute specific effects (damage, draw, destroy, etc.)
- **Continuous Effects:** Floating modifiers created by spells/abilities, applied during state projection
- **CompositeEffect:** Chain effects with `then` operator for multi-step abilities
- **Replacement Effects:** Interceptors that modify actions before execution (e.g., Doubling Season)
- **Dependency System:** Effects in same layer sorted by trial application to determine dependencies (Rule 613.8)
- **State Projector:** Calculates derived state by applying Rule 613 layers to base state

## Web Client Architecture

The web client follows the **dumb terminal** pattern:

- **No game rules** - Server validates all actions
- **No state computation** - Server sends complete game state
- **Intent capture only** - Client sends player clicks/selections

**Tech Stack:** React 18.3, Zustand 5.0 (state management), WebSocket (networking)

**Data Flow:**

1. Server sends `stateUpdate` with game state, events, and legal actions
2. Client renders game board with card positions
3. User clicks card → Client checks legal actions via hooks
4. Client sends `submitAction` with GameAction
5. Server validates, updates state, broadcasts new state

**Key Files (web-client):**

| File                            | Purpose                                  |
|---------------------------------|------------------------------------------|
| `store/gameStore.ts`            | Zustand store for game state             |
| `store/selectors.ts`            | Derived state selectors                  |
| `network/websocket.ts`          | WebSocket connection management          |
| `network/messageHandlers.ts`    | Server message processing                |
| `components/game/GameBoard.tsx` | Main game board layout                   |
| `components/ui/GameUI.tsx`      | HUD elements (life, mana, phases)        |
| `components/decisions/`         | Decision UI (scry, library search, etc.) |
| `components/combat/`            | Combat UI (arrows, blockers)             |
| `components/targeting/`         | Targeting arrows and overlays            |
| `hooks/useInteraction.ts`       | Card interaction logic                   |
| `hooks/useTargeting.ts`         | Targeting flow management                |
| `hooks/useLegalActions.ts`      | Legal action queries                     |

## Adding New Content

### Adding a New Card

1. Create file in `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/`
2. Define card using `cardDef { }` DSL
3. Add to set definition in `definitions/{set}/YourSet.kt`
4. Engine automatically loads via ServiceLoader

Example:

```kotlin
// File: definitions/custom/cards/ThunderStrike.kt
val ThunderStrike = cardDef("Thunder Strike") {
    manaCost = "{1}{R}"
    typeLine = "Instant"

    spell {
        effect = Effects.DealDamage(3)
        target = TargetFilter.Any
    }

    metadata {
        rarity = Rarity.COMMON
        flavorText = "Zap!"
    }
}

// File: definitions/custom/MyNewSet.kt
val MyNewSet = cardSet("MNS", "My New Set") {
    add(ThunderStrike)
}

// File: provider/SetRegistry.kt
object SetRegistry {
    val allSets = listOf(
        PortalSet,
        MyNewSet  // <-- Register here
    )
}
```

### Adding a New Mechanic

1. **SDK:** Add effect type to `mtg-sdk/.../scripting/effect/Effect.kt`
2. **Engine:** Create executor in `rules-engine/.../handlers/effects/` implementing `EffectExecutor`
3. **Engine:** Register executor in `EffectExecutorRegistry.kt`
4. **Sets:** Use it in card scripts

Example (Scry):

```kotlin
// 1. In mtg-sdk/scripting/effect/Effect.kt
@Serializable
sealed interface Effect {
    @Serializable
    data class Scry(val amount: Int) : Effect
}

// 2. In rules-engine/handlers/effects/library/ScryExecutor.kt
class ScryExecutor : EffectExecutor {
    override fun execute(state: GameState, effect: Effect, context: EffectContext): ExecutionResult {
        val scry = effect as Effects.Scry
        // Implementation
    }
}

// 3. In rules-engine/handlers/effects/EffectExecutorRegistry.kt
val executors = mapOf(
    Effects.Scry::class to ScryExecutor()
)

// 4. In mtg-sets card definition
triggeredAbility {
    trigger = Triggers.EntersBattlefield
    effect = Effects.Scry(2)
}
```

## Documentation

Detailed architecture docs in `docs/`:

| Document                                 | Topic                                         |
|------------------------------------------|-----------------------------------------------|
| `api-guide.md`                           | Step-by-step guide for adding cards/mechanics |
| `architecture-overview.md`               | High-level system architecture                |
| `engine-architecture.md`                 | Module structure and design patterns          |
| `the-perfect-mtg-ecs-architecture.md`    | Core ECS philosophy and immutable state       |
| `card-definition-guide.md`               | Comprehensive card DSL reference              |
| `continuous-effect-dependency-system.md` | Rule 613.8 dependency resolution              |
| `managing-complex-and-rare-abilities.md` | Patterns for complex card abilities           |
| `engine-server-interface.md`             | Engine ↔ API contract specification           |
| `player-input.md`                        | Async I/O and decision protocol               |
| `data-contracts.md`                      | JSON payloads between client and server       |
| `web-client-architecture.md`             | Frontend architecture and WebSocket API       |

## Testing Strategy

**Test Pyramid:**

- **Unit Tests:** Component logic, handler execution (Kotest)
- **Integration Tests:** Multi-action sequences, layer system correctness
- **Scenario Tests:** Full card interactions, edge cases
- **E2E Tests:** WebSocket communication, client-server integration (future)

Run tests:

```bash
just test              # All tests
just test-rules        # Engine only
just test-class CardInteractionTest
```

## Development Workflow

1. **Add Card:** Define in `mtg-sets` using DSL
2. **New Effect?** Add to `mtg-sdk`, implement executor in `rules-engine`
3. **Test:** Write scenario test for card interaction
4. **Run:** Start server (`just server`) and client (`just client`)
5. **Play:** Connect at http://localhost:5173

## Key Constraints

- **Never** modify base components in-place - always return new state
- **Never** put card-specific logic in engine - use handlers and DSL
- **Never** compute legal actions in client - server sends them
- **Always** handle layer dependencies correctly (trial application)
- **Always** emit events for animations, never mutate silently
