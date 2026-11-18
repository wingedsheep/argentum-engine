package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Akroan Mastiff
 * {3}{W}
 * Creature — Dog
 * 2/2
 *
 * {W}, {T}: Tap target creature.
 */
val AkroanMastiff = card("Akroan Mastiff") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    power = 2
    toughness = 2
    oracleText = "{W}, {T}: Tap target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Zoltan Boros"
        flavorText = "Even when many of the soldiers who guarded Akros were called away, its safety was never in doubt."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e21938b-46b1-4b2f-8269-0cd0e998cddc.jpg?1783939463"
    }
}
