# Adding New Cards: Complete Workflow Guide

This guide provides a practical, step-by-step workflow for implementing new cards in Argentum Engine. It focuses on the
decision-making process at each stage, helping you identify what can be reused versus what needs to be created.

---

## Overview

Adding a card follows this general flow:

1. **Analyze the card** - Understand what it does
2. **Check existing effects** - Find reusable components
3. **Identify gaps** - Determine what's missing
4. **Implement backend** - Add any new effects/mechanics
5. **Define the card** - Write the card definition
6. **Handle UI needs** - Add frontend components if needed
7. **Test** - Write scenario tests if it does something interesting / new

---

## Phase 1: Analyze the Card

Before writing any code, fully understand what the card does.

### Questions to Answer

1. **What type is it?** (Creature, Instant, Sorcery, Enchantment, etc.)
2. **What are its abilities?**
    - Spell effect (one-time when cast)?
    - Triggered ability (when X happens, do Y)?
    - Activated ability (pay cost, get effect)?
    - Static ability (continuous effect)?
3. **Does it target?** What can be targeted?
4. **Does it require player decisions?** (choosing modes, dividing damage, ordering, etc.)
5. **Are there timing restrictions?** (only during combat, only on your turn, etc.)

### Example Analysis: Forked Lightning

```
Card: Forked Lightning
{3}{R} Sorcery
"Forked Lightning deals 4 damage divided as you choose among one, two, or three target creatures."

Analysis:
- Type: Sorcery (spell effect, goes to graveyard after)
- Targets: 1-3 creatures (variable targeting)
- Effect: Deal divided damage
- Decision required: Player must distribute damage among targets
- No timing restrictions beyond sorcery speed
```

---

## Phase 2: Check Existing Effects

Before implementing anything new, search for existing effects that can be reused or combined.

### Where to Look

| Component     | Location                                                     | What to Find           |
|---------------|--------------------------------------------------------------|------------------------|
| Effects       | `mtg-sdk/src/main/kotlin/.../scripting/Effect.kt`            | Effect data classes    |
| Executors     | `rules-engine/src/main/kotlin/.../handlers/effects/`         | Effect implementations |
| Decisions     | `rules-engine/src/main/kotlin/.../core/PendingDecision.kt`   | Decision types         |
| Continuations | `rules-engine/src/main/kotlin/.../core/Continuation.kt`      | Resume handlers        |
| Target Types  | `mtg-sdk/src/main/kotlin/.../targeting/TargetRequirement.kt` | Targeting options      |

### Search Commands

```bash
# Find effects by name pattern
grep -r "data class.*Effect" mtg-sdk/src/main/kotlin/

# Find executors
ls rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/

# Find decision types
grep -r "interface.*Decision\|data class.*Decision" rules-engine/src/main/kotlin/

# Find existing cards with similar mechanics
grep -r "DealDamage\|Divide" mtg-sets/src/main/kotlin/
```

### Common Reusable Effects

| Effect                    | Description                 | Example Cards            |
|---------------------------|-----------------------------|--------------------------|
| `DealDamageEffect`        | Deal damage (fixed or dynamic) | Lightning Bolt, Blaze |
| `MoveToZoneEffect`        | Destroy/exile/bounce/etc    | Murder, Unsummon         |
| `DrawCardsEffect`         | Draw cards                  | Divination               |
| `DiscardEffect`           | Discard cards               | Mind Rot                 |
| `GainLifeEffect`          | Gain life                   | Healing Salve            |
| `ModifyStatsEffect`       | +X/+Y until end of turn     | Giant Growth             |
| `SearchLibraryEffect`     | Tutor for cards             | Demonic Tutor            |

### Common Reusable Decisions

| Decision                 | Use Case                | UI Component             |
|--------------------------|-------------------------|--------------------------|
| `SelectCardsDecision`    | Choose cards from a set | `ZoneSelectionUI`        |
| `YesNoDecision`          | May abilities           | `YesNoDecisionUI`        |
| `ChooseTargetsDecision`  | Multi-target selection  | `TargetingUI`            |
| `SearchLibraryDecision`  | Library search          | `LibrarySearchUI`        |
| `ReorderLibraryDecision` | Scry, ordering          | `ReorderCardsUI`         |
| `OrderObjectsDecision`   | Blocker order           | `OrderBlockersUI`        |
| `ChooseNumberDecision`   | Choose X value          | `ChooseNumberDecisionUI` |
| `DistributeDecision`     | Divide damage/counters  | `DistributeDecisionUI`   |

---

## Phase 3: Identify What's Missing

Compare your card's needs against existing components.

### Decision Tree

```
Does an effect exist for this mechanic?
├── YES → Can use as-is or with parameters?
│   ├── YES → Skip to Phase 5 (Card Definition)
│   └── NO → Need new effect variant (Phase 4)
└── NO → Need new effect + executor (Phase 4)

Does the effect require player input?
├── NO → Effect can complete synchronously
└── YES → Does a matching Decision type exist?
    ├── YES → Reuse existing decision + UI
    └── NO → Need new Decision + Continuation + UI (Phase 4 + 6)
```

### Example: Forked Lightning Gap Analysis

```
Existing:
✓ DividedDamageEffect exists in mtg-sdk
✓ DistributeDecision exists for player input
✓ TargetCreature with count/minCount for variable targeting

Missing:
✗ No DividedDamageExecutor to handle the effect
✗ No DistributeDamageContinuation to resume after decision
✗ No DistributeDecisionUI component
✗ No resume handler in ContinuationHandler
```

---

## Phase 4: Backend Implementation

If new mechanics are needed, implement them in this order:

### 4.1 Add Effect Type (if needed)

**File:** `mtg-sdk/src/main/kotlin/.../scripting/Effect.kt`

```kotlin
@Serializable
data class MyNewEffect(
    val param1: Int,
    val param2: String
) : Effect
```

**Considerations:**

- Keep effects as pure data (no logic)
- Use `@Serializable` for network transport
- Parameters should be immutable

### 4.2 Add Continuation Type (if effect pauses)

**File:** `rules-engine/src/main/kotlin/.../core/Continuation.kt`

```kotlin
@Serializable
data class MyNewContinuation(
    override val decisionId: String,
    val contextData: EntityId,
    // ... other state needed to resume
) : ContinuationFrame
```

**Considerations:**

- Store everything needed to resume execution
- Include `decisionId` to match with response
- Keep serializable for state persistence

### 4.3 Create Effect Executor

**File:** `rules-engine/src/main/kotlin/.../handlers/effects/{category}/MyNewExecutor.kt`

```kotlin
class MyNewExecutor(
    private val decisionHandler: DecisionHandler // if needs decisions
) : EffectExecutor<MyNewEffect> {

    override val effectType: KClass<MyNewEffect> = MyNewEffect::class

    override fun execute(
        state: GameState,
        effect: MyNewEffect,
        context: EffectContext
    ): ExecutionResult {
        // Simple case: complete immediately
        return ExecutionResult.success(newState, events)

        // Complex case: pause for decision
        val decision = createDecision(...)
        val continuation = MyNewContinuation(...)
        return ExecutionResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision,
            events
        )
    }
}
```

**Considerations:**

- Return `ExecutionResult.success()` for immediate completion
- Return `ExecutionResult.paused()` when needing player input
- Push continuation BEFORE returning paused result
- Use `EffectExecutorUtils` for common operations (damage, destroy, etc.)

### 4.4 Register Executor

**File:** `rules-engine/src/main/kotlin/.../handlers/effects/{Category}Executors.kt`

```kotlin
class DamageExecutors(
    private val decisionHandler: DecisionHandler
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        // ... existing executors
        MyNewExecutor(decisionHandler)  // Add here
    )
}
```

### 4.5 Add Resume Handler (if effect pauses)

**File:** `rules-engine/src/main/kotlin/.../handlers/ContinuationHandler.kt`

Add case in `resume()`:

```kotlin
is MyNewContinuation -> resumeMyNewEffect(stateAfterPop, continuation, response)
```

Add handler function:

```kotlin
private fun resumeMyNewEffect(
    state: GameState,
    continuation: MyNewContinuation,
    response: DecisionResponse
): ExecutionResult {
    // Validate response type
    if (response !is ExpectedResponse) {
        return ExecutionResult.error(state, "Expected X response")
    }

    // Process the response
    // ...

    // Continue execution
    return checkForMoreContinuations(newState, events)
}
```

**Important:** If the effect causes damage or other state changes that could kill creatures, state-based actions are
checked automatically after `SubmitDecision` completes (as of the Forked Lightning implementation).

---

## Phase 5: Card Definition

Define the card in `mtg-sets`.

### 5.1 Create Card File

**File:** `mtg-sets/src/main/kotlin/.../definitions/{set}/cards/MyCard.kt`

```kotlin
package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MyNewEffect
import com.wingedsheep.sdk.targeting.TargetCreature

val MyCard = card("My Card") {
    manaCost = "{2}{R}"
    typeLine = "Instant"

    spell {
        target = TargetCreature(count = 2, minCount = 1)
        effect = MyNewEffect(param1 = 3, param2 = "value")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Artist Name"
        flavorText = "Flavor text here."
        imageUri = "https://..."
    }
}
```

### 5.2 Add to Set

**File:** `mtg-sets/src/main/kotlin/.../definitions/{set}/{Set}Set.kt`

```kotlin
val PortalSet = cardSet("POR", "Portal") {
    // ... existing cards
    add(MyCard)
}
```

### Targeting Options Reference

| Requirement                               | Use Case            |
|-------------------------------------------|---------------------|
| `TargetCreature()`                        | Single creature     |
| `TargetCreature(count = 3)`               | Exactly 3 creatures |
| `TargetCreature(count = 3, minCount = 1)` | 1 to 3 creatures    |
| `TargetCreature(optional = true)`         | Up to 1 creature    |
| `TargetPermanent(filter = ...)`           | Filtered permanents |
| `TargetPlayer()`                          | Target player       |
| `TargetCreatureOrPlayer()`                | Any target          |

---

## Phase 6: Frontend Implementation

If a new decision type was added, create UI components.

### 6.1 Add TypeScript Types

**File:** `web-client/src/types/messages.ts`

```typescript
export interface MyNewDecision extends PendingDecisionBase {
    readonly type: 'MyNewDecision'
    readonly customField: number
    readonly options: readonly EntityId[]
}
```

Add to union:

```typescript
export type PendingDecision =
    |
...
|
MyNewDecision
```

### 6.2 Export Type

**File:** `web-client/src/types/index.ts`

```typescript
  MyNewDecision,
```

### 6.3 Add Store Method

**File:** `web-client/src/store/gameStore.ts`

Interface:

```typescript
submitMyNewDecision: (data: MyResponseData) => void
```

Implementation:

```typescript
submitMyNewDecision: (data) => {
    const {pendingDecision, playerId} = get()
    if (!pendingDecision || !playerId) return

    const action = {
        type: 'SubmitDecision' as const,
        playerId,
        response: {
            type: 'MyNewResponse' as const,
            decisionId: pendingDecision.id,
            ...data,
        },
    }
    ws?.send(createSubmitActionMessage(action))
},
```

### 6.4 Create UI Component

**File:** `web-client/src/components/decisions/MyNewDecisionUI.tsx`

```typescript
import {useState} from 'react'
import {useGameStore} from '../../store/gameStore'
import type {MyNewDecision} from '../../types'
import type {ResponsiveSizes} from '../../hooks/useResponsive'

interface Props {
    decision: MyNewDecision
    responsive: ResponsiveSizes
}

export function MyNewDecisionUI({decision, responsive}: Props) {
    const submitMyNewDecision = useGameStore((s) => s.submitMyNewDecision)
    const [selection, setSelection] = useState(...)

    const handleConfirm = () => {
        submitMyNewDecision(selection)
    }

    return (
        <div style = {
    { /* full-screen overlay */
    }
}>
    {/* UI for making the decision */
    }
    <button onClick = {handleConfirm} > Confirm < /button>
        < /div>
)
}
```

**Reusable UI Components:**

- `ZoneSelectionUI` - Card grid with selection
- `getCardImageUrl()` - Fetch card images
- `ResponsiveSizes` - Responsive sizing

### 6.5 Add Routing

**File:** `web-client/src/components/decisions/DecisionUI.tsx`

```typescript
import {MyNewDecisionUI} from './MyNewDecisionUI'

// In the component:
if (pendingDecision.type === 'MyNewDecision') {
    return <MyNewDecisionUI decision = {pendingDecision}
    responsive = {responsive}
    />
}
```

---

## Phase 7: Testing

### 7.1 Create Scenario Test

**File:** `game-server/src/test/kotlin/.../scenarios/MyCardScenarioTest.kt`

```kotlin
class MyCardScenarioTest : ScenarioTestBase() {
    init {
        context("My Card basic functionality") {
            test("does X when Y") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "My Card")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast the card
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "My Card").first(),
                        listOf(ChosenTarget.Permanent(game.findPermanent("Grizzly Bears")!!))
                    )
                )

                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve
                game.resolveStack()

                // If decision needed:
                game.getPendingDecision().shouldBeInstanceOf<MyNewDecision>()
                game.submitMyDecision(...)

                // Verify results
                withClue("Expected outcome") {
                    // assertions
                }
            }
        }
    }
}
```

### 7.2 Run Tests

```bash
# Run specific test
just test-class MyCardScenarioTest

# Run all game-server tests
just test-server

# Run all tests
just test
```

### 7.3 Manual Testing

```bash
# Start server
just server

# Start client (separate terminal)
just client

# Open http://localhost:5173
```

---

## Quick Reference Checklist

### Simple Card (existing effects only)

- [ ] Create card file in `mtg-sets/.../cards/`
- [ ] Add to set file

### Card with New Effect (no decisions)

- [ ] Add effect type to `mtg-sdk/.../Effect.kt`
- [ ] Create executor in `rules-engine/.../handlers/effects/`
- [ ] Register in appropriate `*Executors.kt` module
- [ ] Create card definition
- [ ] Write scenario test

### Card with New Decision

- [ ] Add effect type to `mtg-sdk`
- [ ] Add decision type to `rules-engine/.../PendingDecision.kt`
- [ ] Add continuation type to `rules-engine/.../Continuation.kt`
- [ ] Create executor with decision handling
- [ ] Register executor
- [ ] Add resume handler in `ContinuationHandler.kt`
- [ ] Add TypeScript types in `web-client/src/types/messages.ts`
- [ ] Export types in `web-client/src/types/index.ts`
- [ ] Add store method in `web-client/src/store/gameStore.ts`
- [ ] Create UI component in `web-client/src/components/decisions/`
- [ ] Add routing in `DecisionUI.tsx`
- [ ] Create card definition
- [ ] Write scenario test
- [ ] Run TypeScript type check (`npm run typecheck`)

---

## Common Pitfalls

1. **Forgetting to check SBAs**: State-based actions (creature death from damage) are checked after spell resolution and
   after decision submission. If adding a new entry point, ensure SBAs are checked.

2. **Not validating decision response type**: Always check `response !is ExpectedType` before casting.

3. **Missing continuation data**: Store all context needed to resume in the continuation. You can't access the original
   effect or context after pausing.

4. **TypeScript export**: Remember to export new types from `types/index.ts`.

5. **Testing only happy path**: Test edge cases like single target vs multiple targets, minimum selections, etc.
