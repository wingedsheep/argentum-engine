package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllCreaturesWithColorEffect

/**
 * Virtue's Ruin
 * {2}{B}
 * Sorcery
 * Destroy all white creatures.
 */
val VirtuesRuin = card("Virtue's Ruin") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        effect = DestroyAllCreaturesWithColorEffect(Color.WHITE)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Adam Rex"
        flavorText = "Virtue crumbles before the relentless march of darkness."
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d0e1f2a-7b8c-9d0e-1f2a-3b4c5d6e7f8a.jpg"
    }
}
