package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Rampant Growth {1}{G}
 * Sorcery
 *
 * Search your library for a basic land card, put that card onto the battlefield
 * tapped, then shuffle.
 */
val RampantGrowth = card("Rampant Growth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card, put that card onto the battlefield tapped, then shuffle."

    spell {
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "234"
        artist = "Steven Belledin"
        flavorText = "Nature grows solutions to its problems."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6e4e354-dee5-4c9d-960d-9847fca97c7a.jpg?1721429355"
        ruling(
            "2004-10-04",
            "The land does not count toward your one per turn limit because it was put onto the battlefield by an effect.",
        )
    }
}
