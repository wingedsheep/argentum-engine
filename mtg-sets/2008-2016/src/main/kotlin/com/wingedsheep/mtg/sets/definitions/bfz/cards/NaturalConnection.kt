package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Natural Connection
 * {2}{G}
 * Instant
 * Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 */
val NaturalConnection = card("Natural Connection") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Wesley Burt"
        flavorText = "\"Zendikar will not surrender, and neither can we.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e11cd83-5930-4cff-8f9b-55337ed263b7.jpg?1783938187"
    }
}
