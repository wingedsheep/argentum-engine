package com.wingedsheep.mtg.sets.definitions.all.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reprisal
 * {1}{W}
 * Instant
 *
 * Destroy target creature with power 4 or greater. It can't be regenerated.
 *
 * "Power 4 or greater" restricts what may be chosen, so it lives on the target requirement
 * (`powerAtLeast(4)`) and is checked again on resolution. "It can't be regenerated" is
 * `Effects.Destroy(noRegenerate = true)` — the can't-be-regenerated marker, then the destroy.
 */
val Reprisal = card("Reprisal") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature with power 4 or greater. It can't be regenerated."

    spell {
        val creature = target(TargetFilter.Creature.powerAtLeast(4))
        effect = Effects.Destroy(creature, noRegenerate = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13a"
        artist = "Randy Asplund-Faith"
        flavorText = "\"The meek shall fight as one, and they shall overcome even the greatest of foes.\"\n" +
            "—Halvor Arensson, Kjeldoran Priest"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/179f50be-6658-42f4-b9b9-c97c7d3f239a.jpg?1783947200"
    }
}
