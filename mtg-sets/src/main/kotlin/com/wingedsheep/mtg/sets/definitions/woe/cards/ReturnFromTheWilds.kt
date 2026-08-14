package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Return from the Wilds
 * {2}{G}
 * Sorcery
 *
 * Choose two —
 * • Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * • Create a 1/1 white Human creature token.
 * • Create a Food token.
 *
 * A plain `modal(chooseCount = 2)`: two *different* modes (no "you may choose the same mode more
 * than once" clause, so `allowRepeat` stays false) picked at cast time. None of the modes targets,
 * so there is nothing to defer to resolution. The land search is not optional — "search your
 * library for a basic land card" with no "you may", so a player who finds nothing (or declines to
 * find) simply shuffles.
 */
val ReturnFromTheWilds = card("Return from the Wilds") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose two —\n" +
        "• Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\n" +
        "• Create a 1/1 white Human creature token.\n" +
        "• Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        modal(chooseCount = 2) {
            mode(
                "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle",
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true
                )
            )
            mode("Create a 1/1 white Human creature token", woeHumanToken())
            mode("Create a Food token", Effects.CreateFood())
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/9597d9ea-d9b2-4009-8e7c-02caa3585bc5.jpg?1783915079"
        ruling("2024-11-08", "Food is an artifact type. Even though it appears on some creatures, it's never a creature type.")
    }
}
