package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thrill of Possibility
 * {1}{R}
 * Instant
 *
 * As an additional cost to cast this spell, discard a card.
 * Draw two cards.
 *
 * Canonical printing: Throne of Eldraine (ELD) — the earliest real expansion printing.
 * Later reprints (THB, M21, DMU, ONE, FDN, …) contribute only a `Printing` row.
 */
val ThrillOfPossibility = card("Thrill of Possibility") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, discard a card.\n" +
        "Draw two cards."

    additionalCost(Costs.additional.DiscardCards())

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Steve Argyle"
        flavorText = "\"Remember all the great heroes who were careful and never did anything risky? Me neither.\"\n—Syr Carah, the Bold"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9021f85-7ab4-4a78-a398-1611fe09cd14.jpg?1782707837"
    }
}
