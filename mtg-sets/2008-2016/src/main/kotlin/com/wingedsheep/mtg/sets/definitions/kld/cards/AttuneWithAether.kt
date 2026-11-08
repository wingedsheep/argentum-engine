package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Attune with Aether — Kaladesh #145
 * {G} · Sorcery
 *
 * Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * You get {E}{E} (two energy counters).
 */
val AttuneWithAether = card("Attune with Aether") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, reveal it, put it into your hand, then shuffle. " +
        "You get {E}{E} (two energy counters)."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = Filters.BasicLand,
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        ) then Effects.GetEnergy(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Wesley Burt"
        flavorText = "\"I've never seen a world where aether weaves through every aspect of nature.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32b0707d-241e-4ced-9251-b16af4fef2cb.jpg?1783937182"
        ruling(
            "2024-06-07",
            "If an effect says you get one or more {E}, you get that many energy counters. To pay one or more " +
                "{E}, you lose that many energy counters. You can't pay more energy counters than you have."
        )
    }
}
