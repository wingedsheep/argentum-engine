package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Go Forth — Foundations Jumpstart #20
 * {G} · Instant
 *
 * Choose one —
 * • Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * • Target creature gets +2/+2 until end of turn.
 */
val GoForth = card("Go Forth") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Search your library for a basic land card, reveal it, put it into your hand, then shuffle.\n" +
        "• Target creature gets +2/+2 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Search your library for a basic land card, reveal it, put it into your hand, then shuffle") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    count = 1,
                    destination = SearchDestination.HAND,
                    reveal = true,
                    shuffleAfter = true
                )
            }
            mode("Target creature gets +2/+2 until end of turn") {
                val t = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(2, 2, t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Serena Malyon"
        flavorText = "Spark or no, Nissa would always be an explorer."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4ed67f0-85cc-4b1d-b745-e13e5bea3397.jpg?1783908864"
    }
}
