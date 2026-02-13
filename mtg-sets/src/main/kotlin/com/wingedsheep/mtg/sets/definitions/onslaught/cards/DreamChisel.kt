package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.FaceDownSpellCostReduction

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
        ability = FaceDownSpellCostReduction(CostReductionSource.Fixed(1))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "308"
        artist = "Ron Spears"
        flavorText = "Ixidor's first creation was a chisel, and with it he carved a world from his dreams."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e89610e9-f1d3-4332-901a-2598bf01d61d.jpg?1562950378"
    }
}
