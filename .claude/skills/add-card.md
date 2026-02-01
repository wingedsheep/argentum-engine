# Add MTG Card Skill

This skill implements new Magic: The Gathering cards for the Argentum Engine.

## When to Use

Invoke this skill with `/add-card <card-name>` when:
- Adding a new card to the engine
- The user provides a card name to implement
- The user asks to implement a specific MTG card

## Arguments

- `<card-name>`: The exact name of the Magic: The Gathering card to implement (e.g., "Lightning Bolt", "Grizzly Bears")
- Optional: `--set <set-code>`: The set to add the card to (defaults to "portal" for Portal set, use "onslaught" for Onslaught, etc.)

## Workflow

### Phase 1: Fetch Card Data from Scryfall

**REQUIRED**: Always fetch the exact card data from Scryfall API before implementing.

1. Use WebFetch to get card data:
   - URL: `https://api.scryfall.com/cards/named?exact=<card-name-url-encoded>`
   - For a specific set: `https://api.scryfall.com/cards/named?exact=<card-name-url-encoded>&set=<set-code>`
   - Extract: name, mana_cost, type_line, oracle_text, power, toughness, colors, rarity, collector_number, artist, flavor_text, image_uris.normal

2. **CRITICAL - Image URI**:
   - **ALWAYS use the exact `image_uris.normal` URL from the Scryfall API response**
   - **NEVER generate, guess, or hallucinate an image URI** - they have specific hash-based paths
   - The correct format is: `https://cards.scryfall.io/normal/front/X/X/XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX.jpg`
   - If the URL doesn't match this pattern, re-fetch from Scryfall

3. Parse the oracle text to understand:
   - What type of card it is (creature, instant, sorcery, enchantment, etc.)
   - What abilities it has (spell effect, triggered, activated, static)
   - Targeting requirements
   - Keywords

### Phase 2: Check Existing Effects

Before implementing anything new, search for existing effects that can be reused.

1. **Search for effects in mtg-sdk**:
   ```bash
   grep -r "data class.*Effect\|data object.*Effect" mtg-sdk/src/main/kotlin/
   ```

2. **Check for existing executors**:
   ```bash
   ls rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/
   ```

3. **Review existing card implementations** with similar mechanics:
   ```bash
   grep -r "<keyword-or-effect>" mtg-sets/src/main/kotlin/
   ```

**Common Existing Effects** (check Effect.kt for full list):
- `DealDamageEffect`, `DealDynamicDamageEffect`, `DealXDamageEffect` - damage
- `DrawCardsEffect` - card draw
- `DiscardCardsEffect`, `DiscardRandomEffect` - discard
- `GainLifeEffect`, `LoseLifeEffect` - life changes
- `DestroyEffect` - destroy permanents
- `ExileEffect` - exile permanents
- `ReturnToHandEffect` - bounce effects
- `TapUntapEffect` - tap/untap
- `ModifyStatsEffect` - +X/+Y until end of turn
- `AddCountersEffect`, `RemoveCountersEffect` - counter manipulation
- `CreateTokenEffect` - token creation
- `SearchLibraryEffect` - tutoring
- `ScryEffect`, `SurveilEffect` - library manipulation
- `GrantKeywordUntilEndOfTurnEffect` - temporary keyword grant
- `CounterSpellEffect` - counter target spell

**Common Existing Keywords** (in Keyword.kt):
- FLYING, MENACE, FEAR, SHADOW, HORSEMANSHIP
- FIRST_STRIKE, DOUBLE_STRIKE, TRAMPLE, DEATHTOUCH, LIFELINK
- VIGILANCE, REACH, DEFENDER, INDESTRUCTIBLE
- HASTE, FLASH, HEXPROOF, SHROUD
- SWAMPWALK, FORESTWALK, ISLANDWALK, MOUNTAINWALK, PLAINSWALK
- CHANGELING, PROWESS, CONVOKE, DELVE

### Phase 3: Identify Missing Components

Compare the card's needs against existing components:

1. **Effect exists and can be used as-is** -> Skip to Phase 5
2. **Effect exists but needs parameters** -> Skip to Phase 5
3. **Effect does NOT exist** -> Phase 4 (implement new effect)
4. **Keyword does NOT exist** -> Phase 4 (add keyword)

### Phase 4: Implement Missing Backend Components

If new effects or keywords are needed:

#### 4.1 Add Effect Type (in mtg-sdk)

**File**: `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/Effect.kt`

```kotlin
@Serializable
data class MyNewEffect(
    val param1: Int,
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect
```

#### 4.2 Create Effect Executor (in rules-engine)

**File**: `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/{category}/MyNewExecutor.kt`

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

#### 4.3 Register Executor

**File**: `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/{Category}Executors.kt`

Add to the appropriate executor module.

#### 4.4 Add New Keyword (if needed)

**File**: `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt`

```kotlin
NEW_KEYWORD("New Keyword"),
```

### Phase 5: Create Card Definition

**File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/{CardName}.kt`

Use the `card` DSL. Examples:

**Simple Creature with Keywords**:
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

**Spell with Target Effect**:
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

**Creature with Triggered Ability**:
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
            filter = CardFilter.CreatureCard,
            destination = SearchDestination.HAND
        )
    }

    metadata { ... }
}
```

**X Cost Spell**:
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

### Phase 6: Register Card in Set

**File**: `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/{Set}Set.kt`

Add the import and card to the `allCards` list:

```kotlin
import com.wingedsheep.mtg.sets.definitions.portal.cards.*

object PortalSet {
    val allCards = listOf(
        // ... existing cards ...
        NewCard,  // <-- Add here alphabetically or by collector number
    )
}
```

### Phase 7: Create Scenario Test (Required for New Effects/Keywords)

If this card uses a NEW effect or keyword, create a scenario test.

**File**: `game-server/src/test/kotlin/com/wingedsheep/gameserver/scenarios/{CardName}ScenarioTest.kt`

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

**Test Helper Methods Available**:
- `scenario().withPlayers(name1, name2)` - Create 2-player game
- `.withCardInHand(playerNum, cardName)` - Add card to hand
- `.withCardOnBattlefield(playerNum, cardName, tapped?, summoningSickness?)` - Add permanent
- `.withLandsOnBattlefield(playerNum, landName, count)` - Add lands
- `.withCardInGraveyard(playerNum, cardName)` - Add to graveyard
- `.withCardInLibrary(playerNum, cardName)` - Add to library
- `.withLifeTotal(playerNum, life)` - Set life total
- `.inPhase(phase, step)` - Set game phase
- `.withActivePlayer(playerNum)` - Set active player
- `.build()` - Create TestGame

- `game.castSpell(playerNum, spellName, targetId?)` - Cast spell
- `game.castSpellTargetingPlayer(playerNum, spellName, targetPlayerNum)` - Cast targeting player
- `game.castXSpell(playerNum, spellName, xValue, targetId?)` - Cast X spell
- `game.resolveStack()` - Resolve stack (pass priority)
- `game.passPriority()` - Pass priority once
- `game.findPermanent(name)` - Find permanent by name
- `game.getLifeTotal(playerNum)` - Get life total
- `game.handSize(playerNum)` - Get hand size
- `game.graveyardSize(playerNum)` - Get graveyard size
- `game.isOnBattlefield(cardName)` - Check if on battlefield
- `game.isInGraveyard(playerNum, cardName)` - Check if in graveyard
- `game.hasPendingDecision()` - Check for pending decision
- `game.selectTargets(entityIds)` - Submit target selection
- `game.skipTargets()` - Skip optional targets
- `game.answerYesNo(choice)` - Submit yes/no response
- `game.selectCards(cardIds)` - Submit card selection
- `game.submitDistribution(map)` - Submit distribution (divided damage)

### Phase 8: Run Tests and Verify

```bash
# Run specific test
just test-class MyCardScenarioTest

# Run all game-server tests
just test-server

# Verify the build
just build
```

## Checklist Summary

**Simple Card (existing effects only)**:
- [ ] Fetch card data from Scryfall (use exact API URL)
- [ ] **Verify image URI is from Scryfall response** (never hallucinate)
- [ ] Create card file in `mtg-sets/.../cards/`
- [ ] Add to set's `allCards` list

**Card with New Effect**:
- [ ] Fetch card data from Scryfall (use exact API URL)
- [ ] **Verify image URI is from Scryfall response** (never hallucinate)
- [ ] Add effect type to `mtg-sdk/.../Effect.kt`
- [ ] Create executor in `rules-engine/.../handlers/effects/`
- [ ] Register in appropriate `*Executors.kt`
- [ ] Create card definition
- [ ] Add to set file
- [ ] Write scenario test
- [ ] Run tests

**Card with New Keyword**:
- [ ] Fetch card data from Scryfall (use exact API URL)
- [ ] **Verify image URI is from Scryfall response** (never hallucinate)
- [ ] Add keyword to `mtg-sdk/.../Keyword.kt`
- [ ] Update keyword handlers if needed
- [ ] Create card definition
- [ ] Add to set file
- [ ] Write scenario test
- [ ] Run tests

## Important Notes

1. **Always fetch from Scryfall first** - Never guess card text or stats
2. **NEVER hallucinate image URIs** - Always use the exact `image_uris.normal` from Scryfall API response
3. **Check existing effects** - Most common effects already exist
4. **Use immutable patterns** - Never modify state in place
5. **Test new mechanics** - All new effects/keywords need tests
6. **Follow naming conventions** - CardName should match file name
7. **Keep effects data-only** - Logic goes in executors, not effect data classes
