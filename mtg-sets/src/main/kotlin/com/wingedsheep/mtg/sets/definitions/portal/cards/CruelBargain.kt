package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.LoseHalfLifeEffect

/**
 * Cruel Bargain
 * {B}{B}{B}
 * Sorcery
 * Draw four cards. You lose half your life, rounded up.
 */
val CruelBargain = card("Cruel Bargain") {
    manaCost = "{B}{B}{B}"
    typeLine = "Sorcery"

    spell {
        effect = CompositeEffect(
            listOf(
                DrawCardsEffect(4),
                LoseHalfLifeEffect(roundUp = true)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79e0e2c6-4e7e-4a0f-8c36-d3cdff77ed1a.jpg"
    }
}
