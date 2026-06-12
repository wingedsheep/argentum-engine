package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Map the Frontier
 * {3}{G}
 * Sorcery
 * Search your library for up to two basic land cards and/or Desert cards, put them onto the
 * battlefield tapped, then shuffle.
 */
val MapTheFrontier = card("Map the Frontier") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two basic land cards and/or Desert cards, put them onto the battlefield tapped, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("Desert"),
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Darrell Riche"
        flavorText = "Pioneers with a distaste for crowds and cities are welcomed by the Outcasters, who celebrate solitude and find endless wonder in the natural world."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/165f4428-e1a1-477d-bd90-138189e88163.jpg?1712355950"
    }
}
