# Examples: Card Definitions, Effects, and Tests

## Card Definitions

### Simple Creature with Keywords

```kotlin
package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val GiantSpider = card("Giant Spider") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 4

    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Rob Alexander"
        flavorText = "Its web spans the gap..."
        imageUri = "https://cards.scryfall.io/normal/front/..."
    }
}
```

### Spell with Target Effect

```kotlin
val LightningBolt = card("Lightning Bolt") {
    manaCost = "{R}"
    typeLine = "Instant"

    spell {
        target = AnyTarget()
        effect = DealDamageEffect(3, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        // ...
    }
}
```

### Creature with Triggered Ability

```kotlin
val Gravedigger = card("Gravedigger") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = OnEnterBattlefield()
        optional = true
        target = TargetCardInGraveyard(filter = GraveyardCardFilter.CreatureInYourGraveyard)
        effect = ReturnFromGraveyardEffect(
            destination = SearchDestination.HAND
        )
    }

    metadata { ... }
}
```

### X Cost Spell

```kotlin
val Blaze = card("Blaze") {
    manaCost = "{X}{R}"
    typeLine = "Sorcery"

    spell {
        target = AnyTarget()
        effect = DealXDamageEffect(EffectTarget.ContextTarget(0))
    }

    metadata { ... }
}
```

## New Effect Implementation

### Effect Type (in mtg-sdk)

```kotlin
@Serializable
data class MyNewEffect(
    val param1: Int,
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect
```

### Effect Executor (in rules-engine)

```kotlin
class MyNewExecutor : EffectExecutor<MyNewEffect> {
    override val effectType = MyNewEffect::class

    override fun execute(
        state: GameState,
        effect: MyNewEffect,
        context: EffectContext
    ): ExecutionResult {
        // Implementation
        return ExecutionResult.success(newState, events)
    }
}
```

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
