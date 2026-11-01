package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Nissa's Triumph — War of the Spark #170 (canonical printing)
 * {G}{G}
 * Sorcery
 * Search your library for up to two basic Forest cards. If you control a Nissa planeswalker,
 * instead search your library for up to three land cards. Reveal those cards, put them into your
 * hand, then shuffle.
 *
 * "Instead" swaps the whole search (filter and count), so it is one [Effects.If] choosing between
 * two searches. The Nissa check is read on resolution, like Chandra's Triumph.
 */
val NissasTriumph = card("Nissa's Triumph") {
    manaCost = "{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two basic Forest cards. If you control a Nissa planeswalker, " +
        "instead search your library for up to three land cards. Reveal those cards, put them into your hand, " +
        "then shuffle."

    spell {
        effect = Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Nissa")),
            then = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Land,
                count = 3,
                destination = SearchDestination.HAND,
                reveal = true
            ),
            otherwise = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand.withSubtype(Subtype.FOREST),
                count = 2,
                destination = SearchDestination.HAND,
                reveal = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Kieran Yanner"
        flavorText = "Her triumph came not from destroying the unnatural, but from fostering life."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/7946b256-ae79-4b99-8bf4-0d627baf9044.jpg?1783933409"
    }
}
