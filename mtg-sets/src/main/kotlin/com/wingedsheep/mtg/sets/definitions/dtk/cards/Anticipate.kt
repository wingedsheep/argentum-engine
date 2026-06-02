package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Anticipate
 * {1}{U}
 * Instant
 * Look at the top three cards of your library. Put one of them into your hand
 * and the rest on the bottom of your library in any order.
 */
val Anticipate = card("Anticipate") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top three cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order."

    spell {
        // Look at top 3, keep one in hand, rest to bottom of library in chosen order.
        effect = LibraryPatterns.lookAtTopAndKeep(
            count = DynamicAmount.Fixed(3),
            keepCount = DynamicAmount.Fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7028d9e8-002f-43a1-bdce-0db0b6a642b0.jpg?1562788114"
    }
}
