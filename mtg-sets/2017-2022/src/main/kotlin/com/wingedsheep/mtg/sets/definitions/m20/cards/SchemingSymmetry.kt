package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Scheming Symmetry
 * {B}
 * Sorcery
 * Choose two target players. Each of them searches their library for a card, then shuffles and
 * puts that card on top.
 *
 * "Two target players" is a single count-2 player requirement (the engine keeps the two chosen
 * players distinct). [Player.EachTargetedPlayer] iterates the targeted players still legal on
 * resolution, rebinding `Player.You` to each, so every one of them runs the ordinary tutor-to-top
 * search on their own library: search, shuffle, then put the found card on top.
 */
val SchemingSymmetry = card("Scheming Symmetry") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose two target players. Each of them searches their library for a card, then " +
        "shuffles and puts that card on top."

    spell {
        target(TargetPlayer(count = 2))
        effect = Effects.ForEachPlayer(
            Player.EachTargetedPlayer,
            Patterns.Library.searchLibrary(destination = SearchDestination.TOP_OF_LIBRARY)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Seb McKinnon"
        flavorText = "\"One for you, one for me. What could be more fair?\"\n—Kethis, the Hidden Hand"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01acc50b-856d-442d-9880-1a892b40643b.jpg?1783932988"
    }
}
