package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Impulse
 * {1}{U}
 * Instant
 * Look at the top four cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order.
 */
val Impulse = card("Impulse") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top four cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order."

    spell {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 4,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Bryan Talbot"
        flavorText = "\"Controlling time ensures you need never look impulsive again.\"\n—Teferi"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d710a97-062f-4773-b6c6-8aeddeb3b6e8.jpg?1783947000"
    }
}
