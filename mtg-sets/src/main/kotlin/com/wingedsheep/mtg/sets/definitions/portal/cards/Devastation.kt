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
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71cce019-162c-4969-89ac-1cf94148a032.jpg"
    }
}
