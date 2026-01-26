package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.SearchLibraryToTopEffect

/**
 * Personal Tutor
 * {U}
 * Sorcery
 * Search your library for a sorcery card, reveal it, then shuffle and put that card on top.
 */
val PersonalTutor = card("Personal Tutor") {
    manaCost = "{U}"
    typeLine = "Sorcery"

    spell {
        effect = SearchLibraryToTopEffect(CardFilter.SorceryCard)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1edc3917-fded-4773-8f8d-62bd861c1131.jpg"
    }
}
