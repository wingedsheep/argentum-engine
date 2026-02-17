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

### Atomic Effects (Preferred for Library/Zone Manipulation)

**Always prefer atomic pipeline effects over monolithic effect executors.** Library manipulation, zone movement, and
collection-based mechanics should be composed from small, reusable primitives chained into pipelines. This avoids
one-off executors and makes adding new cards trivial.

The atomic primitives are:

- **Gather** — collect cards into a named collection (no zone change)
- **Select** — present a choice to split a collection
- **Move** — physically move a named collection to a zone
- **RevealUntil** — reveal cards from library until a filter matches
- **ForEachTarget / ForEachPlayer** — iterate a sub-pipeline per target or player

Use `EffectPatterns` factory methods (exposed via `Effects`) instead of writing custom executors:

```kotlin
// Scry 2 → Gather(top 2) → Select(up to 2 for bottom) → Move(bottom) → Move(top)
effect = Effects.Scry(2)

// Surveil 2 → same as scry but selected cards go to graveyard
effect = Effects.Surveil(2)

// Mill 3 → Gather(top 3) → Move(graveyard)
effect = Effects.Mill(3)

// Search library → Gather(library, filter) → Select → Move(hand) → Shuffle
effect = Effects.SearchLibrary(filter = GameObjectFilter.BasicLand)

// Look at top 7, keep 2 → Gather(top 7) → Select(exactly 2) → Move(hand) → Move(rest to graveyard)
effect = EffectPatterns.lookAtTopAndKeep(count = 7, keepCount = 2)

// Reveal until nonland, deal damage equal to its mana value
effect = EffectPatterns.revealUntilNonlandDealDamage(EffectTarget.ContextTarget(0))

// Wheel (each player shuffles hand into library, draws that many)
effect = EffectPatterns.wheelEffect(Player.Each)
```

See `EffectPatterns.kt` for the full list of pre-built pipelines. If your card's mechanic fits the
gather/select/move pattern, compose it from these primitives rather than creating a new executor.

### Composite Effects

Use `then` operator for multi-step abilities:

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
- **Atomic Effects:** Prefer composing library/zone mechanics from Gather → Select → Move primitives via `EffectPatterns`
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

**Prefer composing from atomic effects** when possible. Many mechanics can be built by combining existing pipeline
primitives (Gather, Select, Move, RevealUntil, ForEachPlayer/Target) in `EffectPatterns.kt` without writing any new
executor code. Only create a new effect type and executor when the mechanic truly needs new logic.

**Option A: Compose from atomic effects (preferred)**

Add a new factory method to `EffectPatterns.kt` and wire it through `Effects.kt`:

```kotlin
// In EffectPatterns.kt — compose from existing primitives
fun myMechanic(count: Int) = CompositeEffect(listOf(
    GatherCardsEffect(source = CardSource.TopOfLibrary(count), storeAs = "looked"),
    SelectFromCollectionEffect(from = "looked", selection = SelectionMode.ChooseUpTo(1), ...),
    MoveCollectionEffect(from = "selected", destination = CardDestination.ToZone(Zone.HAND)),
    MoveCollectionEffect(from = "remainder", destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom))
))

// In Effects.kt — expose as a convenient function
fun MyMechanic(count: Int) = EffectPatterns.myMechanic(count)
```

**Option B: New effect executor (only when atomic effects don't suffice)**

1. **SDK:** Add effect type to `mtg-sdk/.../scripting/effects/`
2. **Engine:** Create executor in `rules-engine/.../handlers/effects/` implementing `EffectExecutor`
3. **Engine:** Register executor in the appropriate `ExecutorModule`
4. **Sets:** Use it in card scripts

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
- **E2E Tests:** Full-stack browser tests using Playwright (`e2e-scenarios/`)

Run tests:

```bash
just test              # All tests
just test-rules        # Engine only
just test-class CardInteractionTest
```

### E2E Tests (Playwright)

E2E tests run against the full stack (server + client) in a real browser. They live in `e2e-scenarios/` and use the
`createGame` fixture to set up board state via the dev scenario API, then interact with the UI through the `GamePage`
page object.

**Prerequisites:** Server running (`just server`), client running (`just client`).

**Running E2E tests:**

```bash
cd e2e-scenarios && npx playwright test                              # All E2E tests
cd e2e-scenarios && npx playwright test tests/onslaught/sparksmith   # Specific test
```

**Key Files:**

| File | Purpose |
|------|---------|
| `e2e-scenarios/fixtures/scenarioFixture.ts` | Test fixture — creates game via API, opens browser pages for both players |
| `e2e-scenarios/helpers/gamePage.ts` | `GamePage` page object — all UI interaction and assertion methods |
| `e2e-scenarios/helpers/scenarioApi.ts` | `ScenarioRequest` interface — board state configuration sent to server |
| `e2e-scenarios/helpers/selectors.ts` | CSS selectors for game board elements (`HAND`, `BATTLEFIELD`, `cardByName`) |
| `e2e-scenarios/tests/{set}/` | Test files organized by card set |

**Test structure:**

```typescript
import { test, expect } from '../../fixtures/scenarioFixture'

test('card does X', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1Name: 'Player1',
    player2Name: 'Opponent',
    player1: {
      hand: ['Lightning Bolt'],
      battlefield: [{ name: 'Mountain', tapped: false }],
      library: ['Mountain'],
    },
    player2: {
      battlefield: [{ name: 'Glory Seeker' }],
      library: ['Mountain'],
    },
    phase: 'PRECOMBAT_MAIN',
    activePlayer: 1,
  })

  const p1 = player1.gamePage
  await p1.clickCard('Lightning Bolt')    // Click card to open action menu
  await p1.selectAction('Cast')           // Click action button
  await p1.selectTarget('Glory Seeker')   // Select target
  await p1.confirmTargets()               // Confirm targeting
  // Auto-resolves if opponent has no responses
  await p1.expectNotOnBattlefield('Glory Seeker')
})
```

**ScenarioRequest config:**

- `player1` / `player2`: `{ hand, battlefield, graveyard, library, lifeTotal }` — battlefield items can specify
  `{ name, tapped?, summoningSickness? }`
- `phase`: `'BEGINNING'` | `'PRECOMBAT_MAIN'` | `'COMBAT'` | `'POSTCOMBAT_MAIN'` | `'ENDING'`
- `step`: Step name (e.g., `'UPKEEP'`, `'DECLARE_ATTACKERS'`)
- `activePlayer` / `priorityPlayer`: `1` or `2`
- `player1StopAtSteps` / `player2StopAtSteps`: Step names where auto-pass is disabled (e.g., `['UPKEEP']`)

**GamePage helpers (key methods):**

| Category | Methods |
|----------|---------|
| **Card interaction** | `clickCard(name)`, `selectCardInHand(name)`, `selectAction(label)` |
| **Targeting** | `selectTarget(name)`, `confirmTargets()`, `skipTargets()` |
| **Priority** | `pass()`, `resolveStack(stackItemText)` |
| **Decisions** | `answerYes()`, `answerNo()`, `selectNumber(n)`, `selectOption(text)`, `selectXValue(x)` |
| **Combat** | `attackAll()`, `attackWith(name)`, `declareAttacker(name)`, `declareBlocker(blocker, attacker)`, `confirmBlockers()`, `noBlocks()` |
| **Overlays** | `selectCardInZoneOverlay(name)`, `selectCardInDecision(name)`, `confirmSelection()`, `failToFind()` |
| **Assertions** | `expectOnBattlefield(name)`, `expectNotOnBattlefield(name)`, `expectInHand(name)`, `expectNotInHand(name)`, `expectHandSize(n)`, `expectLifeTotal(id, n)`, `expectGraveyardSize(id, n)`, `expectStats(name, "3/3")`, `expectTapped(name)` |
| **Morph** | `castFaceDown(name)`, `turnFaceUp(name)` |
| **Ghost cards** | `expectGhostCardInHand(name)`, `expectNoGhostCardInHand(name)` |
| **Damage distribution** | `increaseDamageAllocation(name, times)`, `castSpellFromDistribution()`, `allocateDamage(name, amount)`, `confirmDamage()` |

**Important patterns:**

- **Auto-pass:** When a player has no legal responses, they auto-pass. No explicit `p2.pass()` needed unless P2
  actually has instant-speed responses.
- **Library cards:** Always give both players at least one card in their library to prevent draw-from-empty losses.
- **Face-down creatures:** Alt text is `"Card back"`, not the card name. Use `clickCard('Card back')`.
- **Activated ability buttons:** Show full ability description text, not "Activate". Use partial text match like
  `selectAction('damage to target')`.
- **Aura/sacrifice targeting:** Uses the ChooseTargets modal — need `confirmTargets()` after `selectTarget()`.
- **Stop at steps:** Use `player1StopAtSteps: ['UPKEEP']` to prevent auto-passing through steps like upkeep.

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
- **Always** prefer atomic pipeline effects (Gather/Select/Move via `EffectPatterns`) over monolithic executors for library/zone mechanics
