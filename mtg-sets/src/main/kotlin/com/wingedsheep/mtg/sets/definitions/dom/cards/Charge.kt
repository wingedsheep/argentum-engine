package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Charge
 * {W}
 * Instant
 * Creatures you control get +1/+1 until end of turn.
 */
val Charge = card("Charge") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +1/+1 until end of turn."

    spell {
        effect = GroupPatterns.modifyStatsForAll(
            1, 1,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Zezhou Chen"
        flavorText = "\"Honor rides before us. All we have to do is catch up.\" —Danitha Capashen"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/000eded9-854c-408a-aadf-c26209e27432.jpg?1562730460"
    }
}
