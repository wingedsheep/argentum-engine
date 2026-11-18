package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Nissa's Expedition
 * {4}{G}
 * Sorcery
 * Convoke
 * Search your library for up to two basic land cards, put them onto the battlefield tapped,
 * then shuffle.
 *
 * Canonical printing: Magic 2015, the card's earliest real-expansion printing.
 * Reprinted in C21 and SCD as `Printing` rows.
 */
val NissasExpedition = card("Nissa's Expedition") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText =
        "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."

    keywords(Keyword.CONVOKE)

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b6c5cdd-9075-4bb7-88ae-9dc77a637700.jpg?1783939165"
    }
}
