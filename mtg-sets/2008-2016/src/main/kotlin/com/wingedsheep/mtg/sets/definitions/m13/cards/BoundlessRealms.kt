package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Boundless Realms
 * {6}{G}
 * Sorcery
 * Search your library for up to X basic land cards, where X is the number of lands you control,
 * put them onto the battlefield tapped, then shuffle.
 *
 * X is counted as the spell resolves (the search's selection count is evaluated at resolution).
 */
val BoundlessRealms = card("Boundless Realms") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to X basic land cards, where X is the number of lands you control, " +
        "put them onto the battlefield tapped, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = DynamicAmounts.landsYouControl(),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Cliff Childs"
        flavorText = "\"Planeswalking cannot be taught. Either you see the doors or you do not.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3c3cf16-ba81-4558-b1a6-79942a02f629.jpg?1783940474"
        ruling(
            "2012-07-01",
            "The number of basic land cards you put onto the battlefield is equal to the number of lands you control when Boundless Realms begins resolving."
        )
    }
}
