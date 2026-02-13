# Examples: Card Definitions, Effects, and Tests

## Card Definitions

### Simple Creature (Vanilla)

```kotlin
package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val HillGiant = card("Hill Giant") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Giant"
    power = 3
    toughness = 3

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Dan Frazier"
        flavorText = "Hill giants are mostly just big."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cd36579-..."
    }
}
```

### Creature with Keywords

```kotlin
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card

val WillowDryad = card("Willow Dryad") {
    manaCost = "{G}"
    typeLine = "Creature — Dryad"
    power = 1
    toughness = 1

    keywords(Keyword.FORESTWALK)

    metadata { /* ... */ }
}
```

For creatures with multiple keywords (e.g., "Flying, Reach"):
```kotlin
keywords(Keyword.FLYING, Keyword.REACH)
```

### Spell Targeting Any Target

```kotlin
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget

val LightningBolt = card("Lightning Bolt") {
    manaCost = "{R}"
    typeLine = "Instant"

    spell {
        target = Targets.Any
        effect = DealDamageEffect(3, EffectTarget.ContextTarget(0))
    }

    metadata { /* ... */ }
}
```

### X Cost Spell

```kotlin
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget

val Blaze = card("Blaze") {
    manaCost = "{X}{R}"
    typeLine = "Sorcery"

    spell {
        target = Targets.Any
        effect = DealDamageEffect(DynamicAmount.XValue, EffectTarget.ContextTarget(0))
    }

    metadata { /* ... */ }
}
```

### Creature with Triggered Ability (ETB)

```kotlin
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.targeting.TargetObject

val Gravedigger = card("Gravedigger") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        target = TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND)
    }

    metadata { /* ... */ }
}
```

### Creature with Bounce ETB

```kotlin
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone

val ManOWar = card("Man-o'-War") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Jellyfish"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = Targets.Creature
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND)
    }

    metadata { /* ... */ }
}
```

### Composite Effect (Drain)

```kotlin
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GainLifeEffect

val VampiricFeast = card("Vampiric Feast") {
    manaCost = "{5}{B}{B}"
    typeLine = "Sorcery"

    spell {
        target = Targets.Any
        // Chain effects with `then` operator
        effect = DealDamageEffect(4, EffectTarget.ContextTarget(0)) then
                GainLifeEffect(4, EffectTarget.Controller)
    }

    metadata { /* ... */ }
}
```

### Static Ability (Can't Block)

```kotlin
import com.wingedsheep.sdk.scripting.CantBlock

val JungleLion = card("Jungle Lion") {
    manaCost = "{G}"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 1

    staticAbility {
        ability = CantBlock()
    }

    metadata { /* ... */ }
}
```

### Static Ability (Restricted Blocking)

```kotlin
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWithKeyword

val CloudSpirit = card("Cloud Spirit") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 1

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWithKeyword(Keyword.FLYING)
    }

    metadata { /* ... */ }
}
```

### Activated Ability with Restrictions

```kotlin
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget

val CapriciousSorcerer = card("Capricious Sorcerer") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1

    activatedAbility {
        cost = AbilityCost.Tap
        target = Targets.Any
        effect = DealDamageEffect(1, EffectTarget.ContextTarget(0))
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
            )
        )
    }

    metadata { /* ... */ }
}
```

### Activated Mana Ability

```kotlin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects

val LlanowarElves = card("Llanowar Elves") {
    manaCost = "{G}"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
    }

    metadata { /* ... */ }
}
```

### DynamicAmount Usage

```kotlin
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.targeting.TargetOpponent

val BalanceOfPower = card("Balance of Power") {
    manaCost = "{3}{U}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = DrawCardsEffect(DynamicAmounts.handSizeDifferenceFromTargetOpponent())
    }

    metadata { /* ... */ }
}
```

### Conditional Effect

```kotlin
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.ConditionalEffect

// "If you control a creature, draw a card. Otherwise, gain 3 life."
spell {
    effect = ConditionalEffect(
        condition = Conditions.ControlCreature,
        effect = Effects.DrawCards(1),
        elseEffect = Effects.GainLife(3)
    )
}
```

### Modal Spell

```kotlin
val CharmOfChoice = card("Charm of Choice") {
    manaCost = "{1}{U}{B}"
    typeLine = "Instant"

    spell {
        modal(chooseCount = 1) {
            mode("Target creature gets -2/-2 until end of turn") {
                target = Targets.Creature
                effect = Effects.ModifyStats(-2, -2)
            }
            mode("Draw a card", Effects.DrawCards(1))
            mode("Target player discards a card") {
                target = Targets.Player
                effect = Effects.Discard(1, EffectTarget.ContextTarget(0))
            }
        }
    }

    metadata { /* ... */ }
}
```

### Equipment

```kotlin
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

val ShortSword = card("Short Sword") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    equipAbility("{1}")

    metadata { /* ... */ }
}
```

### Aura

```kotlin
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

val GriffinGuide = card("Griffin Guide") {
    manaCost = "{2}{W}"
    typeLine = "Enchantment — Aura"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EnchantedCreature)
    }

    metadata { /* ... */ }
}
```

### Parameterized Keyword Ability

```kotlin
import com.wingedsheep.sdk.scripting.KeywordAbility

val ProtectedKnight = card("Protected Knight") {
    manaCost = "{1}{W}{W}"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)
    keywordAbility(KeywordAbility.ProtectionFromColor(Color.BLACK))

    metadata { /* ... */ }
}
```

### Additional Cost

```kotlin
import com.wingedsheep.sdk.scripting.AdditionalCost

val NaturalOrder = card("Natural Order") {
    manaCost = "{2}{G}{G}"
    typeLine = "Sorcery"

    additionalCost(AdditionalCost.SacrificePermanent(
        filter = GameObjectFilter.Creature.withColor(Color.GREEN)
    ))

    spell {
        effect = Effects.SearchLibrary(
            filter = Filters.Creature.withColor(Color.GREEN),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata { /* ... */ }
}
```

---

## New Effect Implementation

### 1. Effect Type (in mtg-sdk)

Add to the appropriate file in `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/effect/`:

```kotlin
@Serializable
data class MyNewEffect(
    val param1: Int,
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect
```

### 2. Effect Executor (in rules-engine)

Create in `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/{category}/`:

```kotlin
class MyNewExecutor : EffectExecutor<MyNewEffect> {
    override val effectType = MyNewEffect::class

    override fun execute(
        state: GameState,
        effect: MyNewEffect,
        context: EffectContext
    ): ExecutionResult {
        // Implementation - return new immutable state + events
        return ExecutionResult.success(newState, events)
    }
}
```

### 3. Register in Module

Add to the appropriate `{Category}Executors.kt`:

```kotlin
class MyCategoryExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        // ... existing executors ...
        MyNewExecutor(),
    )
}
```

### 4. Add DSL Facade (optional but recommended)

Add to `Effects.kt`:

```kotlin
fun MyNew(param1: Int, target: EffectTarget = EffectTarget.ContextTarget(0)): Effect =
    MyNewEffect(param1, target)
```

---

## Scenario Test Template

```kotlin
package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class MyCardScenarioTest : ScenarioTestBase() {

    init {
        context("MyCard functionality") {
            test("does X when Y") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "My Card")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast the spell
                val castResult = game.castSpell(1, "My Card")
                withClue("Should cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve
                game.resolveStack()

                // Verify results
                withClue("Expected outcome") {
                    // assertions
                }
            }
        }
    }
}
```

### Test with Target Selection

```kotlin
test("deals damage to target creature") {
    val game = scenario()
        .withPlayers("Player1", "Opponent")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    val target = game.findPermanent("Hill Giant")
    game.castSpell(1, "Lightning Bolt", target.id)
    game.resolveStack()

    game.isInGraveyard(2, "Hill Giant") shouldBe true
}
```

### Test with Decision

```kotlin
test("optional ETB trigger") {
    val game = scenario()
        .withPlayers("Player1", "Opponent")
        .withCardInHand(1, "Gravedigger")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardInGraveyard(1, "Hill Giant")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    game.castSpell(1, "Gravedigger")
    game.resolveStack()

    // Handle optional trigger - answer yes
    game.answerYesNo(true)

    // Select target from graveyard
    val graveyardCard = game.findGraveyardCard(1, "Hill Giant")
    game.selectTargets(listOf(graveyardCard.id))
    game.resolveStack()

    game.handSize(1) shouldBe 1  // Hill Giant returned to hand
}
```

---

## E2E Test Template (Playwright)

E2E tests run against the full stack (server + client) in a real browser. Create these when a card introduces
a new frontend/UI mechanic (new decision type, overlay, targeting flow, zone interaction, etc.).

**File**: `e2e-scenarios/tests/{set}/{card-name}.spec.ts`

### Basic Card E2E Test

```typescript
import { test, expect } from '../../fixtures/scenarioFixture'

test.describe('Sparksmith', () => {
  test('tap to deal damage based on goblin count', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Goblin Player',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Sparksmith', tapped: false, summoningSickness: false },
          { name: 'Goblin Sky Raider', tapped: false, summoningSickness: false },
        ],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // Activated ability: click card → action menu → target → confirm
    await p1.clickCard('Sparksmith')
    await p1.selectAction('damage to target')
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // Opponent has responses → must pass manually
    await player2.gamePage.pass()

    // Verify result
    await p1.expectNotOnBattlefield('Glory Seeker')
    await p1.expectLifeTotal(player1.playerId, 18)
  })
})
```

### E2E Test with Combat

```typescript
test('creature attacks and deals damage', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1Name: 'Attacker',
    player2Name: 'Defender',
    player1: {
      battlefield: [{ name: 'Hill Giant', summoningSickness: false }],
      library: ['Mountain'],
    },
    player2: {
      library: ['Mountain'],
    },
    phase: 'PRECOMBAT_MAIN',
    activePlayer: 1,
  })

  const p1 = player1.gamePage
  await p1.pass()              // Move to combat
  await p1.attackWith('Hill Giant')
  await player2.gamePage.noBlocks()

  await p1.expectLifeTotal(player2.playerId, 17)
})
```

### E2E Test with Step Stops (Upkeep)

Use `player1StopAtSteps` / `player2StopAtSteps` to prevent auto-pass at specific steps:

```typescript
test('triggers during upkeep', async ({ createGame }) => {
  const { player1 } = await createGame({
    player1: {
      graveyard: ['Undead Gladiator'],
      hand: ['Swamp'],
      battlefield: [{ name: 'Swamp' }, { name: 'Swamp' }],
      library: ['Mountain'],
    },
    player2: { library: ['Mountain'] },
    phase: 'BEGINNING',
    step: 'UPKEEP',
    activePlayer: 1,
    player1StopAtSteps: ['UPKEEP'],  // Prevents auto-pass through upkeep
  })

  // Player now has priority during upkeep
  await player1.gamePage.expectGhostCardInHand('Undead Gladiator')
})
```

### Key GamePage Methods

| Category | Methods |
|----------|---------|
| **Card interaction** | `clickCard(name)`, `selectCardInHand(name)`, `selectAction(label)` |
| **Targeting** | `selectTarget(name)`, `confirmTargets()`, `skipTargets()` |
| **Priority** | `pass()`, `resolveStack(stackItemText)` |
| **Decisions** | `answerYes()`, `answerNo()`, `selectNumber(n)`, `selectOption(text)`, `selectXValue(x)` |
| **Combat** | `attackAll()`, `attackWith(name)`, `declareAttacker(name)`, `declareBlocker(b, a)`, `confirmBlockers()`, `noBlocks()` |
| **Overlays** | `selectCardInZoneOverlay(name)`, `selectCardInDecision(name)`, `confirmSelection()`, `failToFind()` |
| **Assertions** | `expectOnBattlefield(name)`, `expectNotOnBattlefield(name)`, `expectInHand(name)`, `expectHandSize(n)`, `expectLifeTotal(id, n)`, `expectGraveyardSize(id, n)`, `expectStats(name, "3/3")` |
| **Morph** | `castFaceDown(name)`, `turnFaceUp(name)` |
| **Damage** | `increaseDamageAllocation(name, times)`, `castSpellFromDistribution()`, `allocateDamage(name, amount)`, `confirmDamage()` |

### Important E2E Patterns

- **Auto-pass**: Opponent auto-passes when they have no legal responses. Only call `p2.pass()` when P2 has instant-speed cards.
- **Library padding**: Always give both players at least one card in their library to prevent draw-from-empty losses.
- **Face-down creatures**: Alt text is `"Card back"`, not the card name. Use `clickCard('Card back')`.
- **Activated abilities**: Action buttons show full ability text, not "Activate". Use partial match: `selectAction('damage to target')`.
- **Aura/sacrifice targeting**: Uses ChooseTargets modal — need `confirmTargets()` after `selectTarget()`.

---

## Set Registration

```kotlin
import com.wingedsheep.mtg.sets.definitions.portal.cards.*

object PortalSet {
    val allCards = listOf(
        // ... existing cards ...
        NewCard,  // <-- Add here alphabetically or by collector number
    )
}
```
