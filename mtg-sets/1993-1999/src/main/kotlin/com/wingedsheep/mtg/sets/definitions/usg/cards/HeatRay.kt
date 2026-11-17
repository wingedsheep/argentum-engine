package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Heat Ray
 * {X}{R}
 * Instant
 * Heat Ray deals X damage to target creature.
 */
val HeatRay = card("Heat Ray") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Heat Ray deals X damage to target creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "199"
        artist = "Brian Snõddy"
        flavorText = "It's not known whether the Thran built the device to forge their wonders or to defend them."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a27f90e-d156-439c-b5e5-6d53bd510fe7.jpg"
    }
}
