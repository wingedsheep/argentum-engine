package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Seek the Horizon
 * {3}{G}
 * Sorcery
 * Search your library for up to three basic land cards, reveal them,
 * put them into your hand, then shuffle.
 */
val SeekTheHorizon = card("Seek the Horizon") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to three basic land cards, reveal them, put them into your hand, then shuffle."

    spell {
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 3,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Min Yum"
        flavorText = "The Temur call the flickering lights the Path of Whispers, believing that they lead the way to ancestral knowledge."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5269292-11be-4647-9074-7ce8115bf36f.jpg?1562794101"
    }
}
