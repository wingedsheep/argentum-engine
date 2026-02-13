package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ReduceFaceDownCastingCost

/**
 * Dream Chisel
 * {2}
 * Artifact
 * Face-down creature spells you cast cost {1} less to cast.
 */
val DreamChisel = card("Dream Chisel") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Face-down creature spells you cast cost {1} less to cast."

    staticAbility {
        ability = ReduceFaceDownCastingCost(amount = 1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "308"
        artist = "Ron Spears"
        flavorText = "Itself a product of Ixidor's tortured psyche, the chisel brings his darkest dreams to life."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e34f1f13-713b-46e8-a516-bcff577e5549.jpg"
    }
}
