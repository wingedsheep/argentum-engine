package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PreventCycling

/**
 * Stabilizer
 * {2}
 * Artifact
 * Players can't cycle cards.
 */
val Stabilizer = card("Stabilizer") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Players can't cycle cards."

    staticAbility {
        ability = PreventCycling
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "David Martin"
        flavorText = "\"Hold that thought.\" —Pemlin, Riptide survivor"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b72dbe81-96d0-4b0d-97a7-c59345f081e8.jpg?1562533763"
    }
}
