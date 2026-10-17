package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Force Spike
 * {U}
 * Instant
 *
 * Counter target spell unless its controller pays {1}.
 */
val ForceSpike = card("Force Spike") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {1}."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{1}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Bryon Wackwitz"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70e64028-ae96-4950-aa6c-9d347409fad3.jpg?1783948076"
    }
}
