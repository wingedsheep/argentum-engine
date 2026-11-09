package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Killing Glare
 * {X}{B}
 * Instant
 *
 * Destroy target creature with power X or less.
 *
 * The cap is `powerAtMostDynamic(xValue())` on the target filter: X is announced before targets
 * (CR 601.2b–c), so the chosen X is checked when the target is chosen and again on resolution —
 * a creature pumped above X in response makes the spell fizzle.
 */
val KillingGlare = card("Killing Glare") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature with power X or less."

    spell {
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.powerAtMostDynamic(DynamicAmounts.xValue()))
        )
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Peter Mohrbacher"
        flavorText = "A casual glance into a dark corner may be your last."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7a4d87d-b844-4f20-8b14-4fd32c53dea5.jpg?1783940130"
        ruling(
            "2013-01-24",
            "The power of the target creature is checked both as you target it and as Killing Glare " +
                "resolves. If its power is greater than the value chosen for X when Killing Glare tries " +
                "to resolve, it won't resolve and none of its effects will happen."
        )
    }
}
