package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.BecomeChosenTypeAllCreaturesEffect

/**
 * Standardize
 * {U}{U}
 * Instant
 * Choose a creature type other than Wall. Each creature becomes that type until end of turn.
 */
val Standardize = card("Standardize") {
    manaCost = "{U}{U}"
    typeLine = "Instant"
    oracleText = "Choose a creature type other than Wall. Each creature becomes that type until end of turn."

    spell {
        effect = BecomeChosenTypeAllCreaturesEffect(
            excludedTypes = listOf("Wall")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "116"
        artist = "Greg Staples"
        flavorText = "\"The only truth is that which you shape for yourself.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2c79e64-91bf-4e87-a4fd-3136ea67c5bb.jpg?1562946613"
    }
}
