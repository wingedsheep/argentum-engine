package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Promising Duskmage — Strixhaven: School of Mages #85 (canonical printing)
 * {2}{B} · Creature — Human Warlock · 2/3
 *
 * When this creature dies, if it had a +1/+1 counter on it, draw a card.
 *
 * An intervening-if (CR 603.4) over last-known information: the counters are gone once the
 * creature is in the graveyard, so the condition reads the +1/+1 count the dies event carried
 * ([DynamicAmounts.lastKnownPlusOneCounters]) rather than the live card.
 */
val PromisingDuskmage = card("Promising Duskmage") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 2
    toughness = 3
    oracleText = "When this creature dies, if it had a +1/+1 counter on it, draw a card."

    triggeredAbility {
        trigger = Triggers.self.dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.lastKnownPlusOneCounters(),
            ComparisonOperator.GTE,
            1
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Johan Grenier"
        flavorText = "\"Your technique is sloppy, but there is talent there. Do it again, and keep " +
            "your right arm in position this time.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ea0dff9-71c3-4d56-aa7b-e0fb9938529a.jpg?1783927362"
    }
}
