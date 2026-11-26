package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Diabolic Revelation
 * {X}{3}{B}{B}
 * Sorcery
 * Search your library for up to X cards, put those cards into your hand, then shuffle.
 */
val DiabolicRevelation = card("Diabolic Revelation") {
    manaCost = "{X}{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to X cards, put those cards into your hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            count = DynamicAmounts.xValue(),
            destination = SearchDestination.HAND
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Raymond Swanland"
        flavorText = "\"There are some secrets so dark no human mind should ever contain them. Interested?\"\n—Nefarox, Overlord of Grixis"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/145d6d7b-1e87-47b7-baf3-d201458ad996.jpg?1783940497"
    }
}
