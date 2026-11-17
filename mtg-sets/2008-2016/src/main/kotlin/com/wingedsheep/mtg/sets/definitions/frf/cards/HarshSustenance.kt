package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Harsh Sustenance
 * {1}{W}{B}
 * Instant
 * Harsh Sustenance deals X damage to any target and you gain X life, where X is the number of
 * creatures you control.
 *
 * X is counted as the spell resolves. State-based actions don't run mid-resolution, so a creature
 * of yours dealt lethal damage by the first half is still counted for the life gain.
 */
val HarshSustenance = card("Harsh Sustenance") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "Harsh Sustenance deals X damage to any target and you gain X life, where X is the number of creatures you control."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), t) then
            Effects.GainLife(DynamicAmounts.creaturesYouControl())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Cynthia Sheppard"
        flavorText = "The Shifting Wastes provide refuge to those who know where to look for it."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/770d60a0-23fc-4224-873c-2e5549b3a816.jpg?1783938674"
    }
}
