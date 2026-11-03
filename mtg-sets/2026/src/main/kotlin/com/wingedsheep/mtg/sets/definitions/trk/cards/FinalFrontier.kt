package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Final Frontier
 * {2}{G}
 * Sorcery
 * Search your library for a basic land card or Planet card, put it onto the battlefield tapped,
 * then shuffle.
 * Beam me up {1}{G} (You may cast this card from your graveyard for {1}{G} if you also return a
 * creature you control to its owner's hand. Then exile this spell.)
 */
val FinalFrontier = card("Final Frontier") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for a basic land card or Planet card, put it onto the battlefield " +
        "tapped, then shuffle.\n" +
        "Beam me up {1}{G} (You may cast this card from your graveyard for {1}{G} if you also " +
        "return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{1}{G}")
    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("Planet"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Jessica Rossier"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34ea6df3-36e3-42c5-a4ed-a4cd18ec6dd0.jpg?1793013607"
    }
}
