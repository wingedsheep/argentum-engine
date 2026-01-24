package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.SearchLibraryEffect

/**
 * Sylvan Tutor
 * {G}
 * Sorcery
 * Search your library for a creature card, reveal that card, shuffle your library,
 * then put the card on top of it.
 */
val SylvanTutor = card("Sylvan Tutor") {
    manaCost = "{G}"
    typeLine = "Sorcery"

    spell {
        effect = SearchLibraryEffect(
            filter = CardFilter.CreatureCard,
            destination = SearchDestination.TOP_OF_LIBRARY
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Terese Nielsen"
        flavorText = "The forest knows where its children hide."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d8bb6e9-c7be-43d7-8d58-97c3f7b23cc7.jpg"
    }
}
