package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Solve the Equation — Strixhaven: School of Mages #54 (canonical printing)
 * {2}{U} · Sorcery
 *
 * Search your library for an instant or sorcery card, reveal it, put it into your hand, then
 * shuffle.
 */
val SolveTheEquation = card("Solve the Equation") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Search your library for an instant or sorcery card, reveal it, put it into your " +
        "hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.InstantOrSorcery,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Lie Setiawan"
        flavorText = "Zimone's brilliance drew attention from many, including the sinister mages " +
            "known as the Oriq."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66c04ee2-c1e0-45fb-aaf5-1b4459df80fc.jpg?1783927375"
    }
}
