package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseHalfLifeEffect

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
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96837a9e-dd68-4ce8-b760-0e1c22837164.jpg"
    }
}
