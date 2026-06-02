package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.dsl.Costs

/**
 * Harrow
 * {2}{G}
 * Instant
 * As an additional cost to cast this spell, sacrifice a land.
 * Search your library for up to two basic land cards, put them onto the battlefield, then shuffle.
 *
 * Canonical printing: Tempest (1997) is Harrow's earliest real-expansion printing.
 */
val Harrow = card("Harrow") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a land.\n" +
        "Search your library for up to two basic land cards, put them onto the battlefield, then shuffle."

    additionalCost(Costs.additional.SacrificePermanent(filter = GameObjectFilter.Land))

    spell {
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Eric David Anderson"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c207142-4880-4935-9827-b91bc7d9d643.jpg?1562053754"
    }
}
