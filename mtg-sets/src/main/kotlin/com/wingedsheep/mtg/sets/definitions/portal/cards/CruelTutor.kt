package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.scripting.SearchLibraryToTopEffect

/**
 * Cruel Tutor
 * {2}{B}
 * Sorcery
 * Search your library for a card, then shuffle and put that card on top. You lose 2 life.
 */
val CruelTutor = card("Cruel Tutor") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        effect = CompositeEffect(
            listOf(
                SearchLibraryToTopEffect(CardFilter.AnyCard),
                LoseLifeEffect(2)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Brian Snoddy"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/255c66db-f3ef-4d1f-979c-8c0c0d6c8e76.jpg"
    }
}
