package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Buried Alive
 * {2}{B}
 * Sorcery
 * Search your library for up to three creature cards, put them into your graveyard, then shuffle.
 */
val BuriedAlive = card("Buried Alive") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to three creature cards, put them into your graveyard, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature,
            count = 3,
            destination = SearchDestination.GRAVEYARD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Brian Horton"
        flavorText = "\"Is it worse to walk while dead, or to be buried alive? I have witnessed both.\"\n—Crovax"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56b92eb5-72b0-46b4-8b16-8a7a7ac80f56.jpg?1783946736"
        ruling("2004-10-04", "You can look in your library and then choose to find any number from zero to three creatures after you look at it.")
    }
}
