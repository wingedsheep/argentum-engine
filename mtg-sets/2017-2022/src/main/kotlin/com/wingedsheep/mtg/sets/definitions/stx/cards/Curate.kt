package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Curate — Strixhaven: School of Mages #40 (canonical printing)
 * {1}{U} · Instant
 *
 * Surveil 2.
 * Draw a card.
 *
 * Reprinted in The Brothers' War (#44) — see the BRO `CurateReprint` row.
 */
val Curate = card("Curate") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Surveil 2. (Look at the top two cards of your library, then put any number of them " +
        "into your graveyard and the rest on top of your library in any order.)\nDraw a card."

    spell {
        effect = Effects.Surveil(2) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Izzy"
        flavorText = "\"*Kathorran War Chants, Vol. III*? That doesn't even belong in this wing!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2a0e716-22b7-4a5e-9b7a-d4a806ee7427.jpg?1783927381"
    }
}
