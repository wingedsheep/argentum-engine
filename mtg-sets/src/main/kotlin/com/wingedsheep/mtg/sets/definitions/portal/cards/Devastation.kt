package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllCreaturesEffect
import com.wingedsheep.sdk.scripting.DestroyAllLandsEffect

/**
 * Devastation
 * {5}{R}{R}
 * Sorcery
 * Destroy all creatures and lands.
 */
val Devastation = card("Devastation") {
    manaCost = "{5}{R}{R}"
    typeLine = "Sorcery"

    spell {
        effect = DestroyAllCreaturesEffect then DestroyAllLandsEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Eric Peterson"
        flavorText = "Nothing remains."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3e4f5a6-b7c8-9d0e-1f2a-3b4c5d6e7f8a.jpg"
    }
}
