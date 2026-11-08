package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Deploy the Gatewatch (Eldritch Moon #20)
 * {4}{W}{W}
 * Sorcery
 *
 * Look at the top seven cards of your library. Put up to two planeswalker cards from among them
 * onto the battlefield. Put the rest on the bottom of your library in a random order.
 */
val DeployTheGatewatch = card("Deploy the Gatewatch") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Look at the top seven cards of your library. Put up to two planeswalker cards from " +
        "among them onto the battlefield. Put the rest on the bottom of your library in a random order."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(7),
            filter = GameObjectFilter.Planeswalker,
            prompt = "Put up to two planeswalker cards from among them onto the battlefield",
            selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(2)),
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "20"
        artist = "Wesley Burt"
        flavorText = "Gideon and Liliana may not have seen eye to eye, but that did not stop them from " +
            "fighting side by side."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3aa54d9-9f01-46e4-b4bd-f8bc57b44ac1.jpg"
    }
}
