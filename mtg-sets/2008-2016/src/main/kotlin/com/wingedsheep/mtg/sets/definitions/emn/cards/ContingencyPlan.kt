package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Contingency Plan — Eldritch Moon #52
 * {1}{U} · Sorcery
 *
 * Surveil 5. (Look at the top five cards of your library, then put any number of them into your
 * graveyard and the rest on top of your library in any order.)
 *
 * Authored to the current Oracle wording (surveil 5) rather than the printed "look at the top
 * five" text it replaced — the same action, expressed directly as [Effects.Surveil].
 */
val ContingencyPlan = card("Contingency Plan") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Surveil 5. (Look at the top five cards of your library, then put any number of them " +
        "into your graveyard and the rest on top of your library in any order.)"

    spell {
        effect = Effects.Surveil(5)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "52"
        artist = "Ryan Yee"
        flavorText = "\"Improvisation is for those who failed to prepare.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9edb76bd-db4b-4279-8eaa-dfdf5952eafc.jpg"
    }
}
