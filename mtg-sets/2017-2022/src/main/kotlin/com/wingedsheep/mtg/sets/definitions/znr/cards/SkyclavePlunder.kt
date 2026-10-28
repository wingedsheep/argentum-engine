package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Skyclave Plunder — Zendikar Rising #81
 * {4}{U} · Sorcery
 *
 * Look at the top X cards of your library, where X is three plus the number of creatures in your
 * party. Put three of those cards into your hand and the rest on the bottom of your library in a
 * random order.
 *
 * The look-at-top / keep-to-hand / rest-on-the-bottom-randomly pipeline
 * ([Patterns.Library.lookAtTopAndKeep]) with a count of 3 + [DynamicAmounts.partySize].
 */
val SkyclavePlunder = card("Skyclave Plunder") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Look at the top X cards of your library, where X is three plus the number of creatures " +
        "in your party. Put three of those cards into your hand and the rest on the bottom of your library " +
        "in a random order. (Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"

    spell {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 3 + DynamicAmounts.partySize(),
            keepCount = DynamicAmounts.fixed(3),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "YW Tang"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3ff05312-787e-4e5e-a0d5-8f3fb576fc49.jpg?1783929386"
    }
}
