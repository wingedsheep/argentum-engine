package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Peer Through Depths
 * {1}{U}
 * Instant — Arcane
 * Look at the top five cards of your library. You may reveal an instant or sorcery card from among
 * them and put it into your hand. Put the rest on the bottom of your library in any order.
 *
 * Seek the Wilds' shape over five cards and an instant-or-sorcery filter.
 */
val PeerThroughDepths = card("Peer Through Depths") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant — Arcane"
    oracleText = "Look at the top five cards of your library. You may reveal an instant or sorcery card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in any order."

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.InstantOrSorcery,
            prompt = "You may reveal an instant or sorcery card from among them and put it into your hand",
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9be02570-b840-46cc-af54-5279463fdcab.jpg?1783944323"
        ruling("2004-12-01", "If you don't reveal an instant or sorcery card, put all the revealed cards on the bottom of your library in any order.")
    }
}
