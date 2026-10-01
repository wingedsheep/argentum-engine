package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Thirsting Roots — Phyrexia: All Will Be One #185
 * {G}
 * Sorcery
 * Choose one —
 * • Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * • Proliferate.
 */
val ThirstingRoots = card("Thirsting Roots") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Search your library for a basic land card, reveal it, put it into your hand, then shuffle.\n" +
        "• Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        modal(chooseCount = 1) {
            mode("Search your library for a basic land card, reveal it, put it into your hand, then shuffle") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    destination = SearchDestination.HAND,
                    reveal = true
                )
            }
            mode("Proliferate") {
                effect = Effects.Proliferate()
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "WolfSkullJack"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75a4a340-453b-471a-82ba-48ebf20b271a.jpg?1783918010"
    }
}
