package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Terramorphic Expanse
 * Land
 *
 * {T}, Sacrifice this land: Search your library for a basic land card,
 * put it onto the battlefield tapped, then shuffle.
 */
val TerramorphicExpanse = card("Terramorphic Expanse") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}, Sacrifice this land: Search your library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = LibraryPatterns.searchLibrary(
            filter = Filters.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
        manaAbility = false
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "279"
        artist = "Dan Murayama Scott"
        flavorText = "Take two steps north into the unsettled future, south into the unquiet past, east into the present day, or west into the great unknown."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd097ea2-0a1c-44a0-824b-d5373df513fb.jpg?1562948625"
    }
}
