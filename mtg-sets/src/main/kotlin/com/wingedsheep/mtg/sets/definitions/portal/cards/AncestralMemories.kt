package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LookAtTopCardsEffect

/**
 * Ancestral Memories
 * {2}{U}{U}{U}
 * Sorcery
 * Look at the top seven cards of your library. Put two of them into your hand
 * and the rest into your graveyard.
 */
val AncestralMemories = card("Ancestral Memories") {
    manaCost = "{2}{U}{U}{U}"
    typeLine = "Sorcery"

    spell {
        effect = LookAtTopCardsEffect(
            count = 7,
            keepCount = 2,
            restToGraveyard = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf9b613c-61bf-4c2d-9c90-2949e442aea5.jpg"
    }
}
