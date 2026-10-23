package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Long Rest
 * {X}{G}{G}{G}
 * Sorcery
 *
 * Return X target cards with different mana values from your graveyard to your hand. If eight or
 * more cards were returned to your hand this way, your life total becomes equal to your starting
 * life total. Exile Long Rest.
 *
 * `differentNumericProperty = MANA_VALUE` keeps the X targets on pairwise-distinct mana values;
 * `moveTracked` counts only the cards that actually reached the hand ("returned this way").
 */
val LongRest = card("Long Rest") {
    manaCost = "{X}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return X target cards with different mana values from your graveyard to your hand. " +
        "If eight or more cards were returned to your hand this way, your life total becomes equal to " +
        "your starting life total. Exile Long Rest."

    spell {
        selfExile()
        targets(
            TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD),
            exactly = DynamicAmounts.xValue(),
            differentNumericProperty = CardNumericProperty.MANA_VALUE,
        )
        effect = Effects.Pipeline {
            val returned = moveTracked(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.HAND))
            ifNotEmpty(returned, minSize = 8) {
                run(Effects.SetLifeTotal(DynamicAmounts.startingLifeTotal()))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Chris Seaman"
        flavorText = "A job well done means a rest well earned."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0615e930-3b6b-4b2f-a09c-0730d491545c.jpg?1783926459"
    }
}
