package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Neverwinter Dryad
 * {G}
 * Creature — Dryad
 * 1/1
 * {2}, Sacrifice this creature: Search your library for a basic Forest card, put it onto the
 * battlefield tapped, then shuffle.
 *
 * Same search pipeline as Field Trip: `ChooseUpTo(1)` inside `searchLibrary` lets the player fail
 * to find, `entersTapped` is the printed "tapped", `shuffleAfter` the "then shuffle".
 */
val NeverwinterDryad = card("Neverwinter Dryad") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    power = 1
    toughness = 1
    oracleText = "{2}, Sacrifice this creature: Search your library for a basic Forest card, " +
        "put it onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.FOREST),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
        description = "{2}, Sacrifice this creature: Search your library for a basic Forest card, " +
            "put it onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Donato Giancola"
        flavorText = "Its life force entwined with its tree, a dryad is an elusive and solitary being " +
            "that wishes only to be left alone."
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e64d7cc7-bbdd-4db3-906e-a2f7672e0f67.jpg?1783926459"
    }
}
