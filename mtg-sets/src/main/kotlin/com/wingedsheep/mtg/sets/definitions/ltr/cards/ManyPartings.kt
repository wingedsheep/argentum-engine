package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Many Partings
 * {G}
 * Sorcery
 *
 * Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * Create a Food token. (It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 */
val ManyPartings = card("Many Partings") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, reveal it, put it into your hand, then shuffle. " +
        "Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        ) then Effects.CreateFood(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Dmitry Burmak"
        flavorText = "\"For you in all the lands of the West there will ever be a welcome, dearest friend.\"\n—Aragorn"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d179dbbe-9c79-4dbc-955a-5209a3e2745a.jpg?1686969470"
    }
}
