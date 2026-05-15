package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Farseek {1}{G}
 * Sorcery
 *
 * Search your library for a Plains, Island, Swamp, or Mountain card,
 * put it onto the battlefield tapped, then shuffle.
 *
 * The filter matches any card carrying one of those four basic land subtypes,
 * so nonbasic duals like Tundra or shocklands are legal targets (per the
 * 2021-03-19 ruling). Forest is intentionally excluded.
 */
val Farseek = card("Farseek") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a Plains, Island, Swamp, or Mountain card, " +
        "put it onto the battlefield tapped, then shuffle."

    spell {
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter.Any.withAnyOfSubtypes(
                listOf(Subtype.PLAINS, Subtype.ISLAND, Subtype.SWAMP, Subtype.MOUNTAIN)
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Jakob Eirich"
        flavorText = "For most animalfolk, Valley is a paradise. But among some adventurers, " +
            "the promise of \"somewhere else\" is just as captivating."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c41b3c9-bbed-447d-a952-250bb7091a9e.jpg?1721428764"
        ruling(
            "2021-03-19",
            "Farseek can find any land with any of the listed land types, including nonbasic ones, " +
                "even if that land is a Forest in addition to one or more of those types.",
        )
    }
}
