package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Worldly Counsel
 * {1}{U}
 * Instant
 * Domain — Look at the top X cards of your library, where X is the number of basic
 * land types among lands you control. Put one of those cards into your hand and the
 * rest on the bottom of your library in any order.
 */
val WorldlyCounsel = card("Worldly Counsel") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Domain — Look at the top X cards of your library, where X is the number of basic land types among lands you control. Put one of those cards into your hand and the rest on the bottom of your library in any order."

    spell {
        // Look at domain cards, keep one in hand, rest to bottom of library in chosen order.
        effect = LibraryPatterns.lookAtTopAndKeep(
            count = DynamicAmounts.domain(),
            keepCount = DynamicAmount.Fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Gary Ruddell"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fc66fbf-f411-4607-aece-7c35d9a07c80.jpg?1562924010"
    }
}
