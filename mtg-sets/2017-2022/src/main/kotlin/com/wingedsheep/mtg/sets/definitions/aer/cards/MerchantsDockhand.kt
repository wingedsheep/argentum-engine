package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Merchant's Dockhand
 * {1}
 * Artifact Creature — Construct
 * 1/2
 * {3}{U}, {T}, Tap X untapped artifacts you control: Look at the top X cards of your library.
 * Put one of them into your hand and the rest on the bottom of your library in any order.
 *
 * The Dockhand pays its own {T}, so it can't also be one of the X artifacts (ruling) — the tap-X
 * filter excludes the source.
 */
val MerchantsDockhand = card("Merchant's Dockhand") {
    manaCost = "{1}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 2
    oracleText = "{3}{U}, {T}, Tap X untapped artifacts you control: Look at the top X cards of your library. " +
        "Put one of them into your hand and the rest on the bottom of your library in any order."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{U}"),
            Costs.Tap,
            Costs.TapXPermanents(GameObjectFilter.Artifact.notSourceItself())
        )
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmounts.xValue(),
            keepCount = DynamicAmounts.fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Christine Choi"
        flavorText = "\"If you can build it, we can move it.\"\n—Bes Tavani, Bomat merchant"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/955b4bc9-1ded-4f23-b415-ab968c681eb7.jpg?1783936724"
        ruling("2017-02-09", "If the value of X is greater than or equal to the number of cards in your library, you can order your library as you wish.")
        ruling("2017-02-09", "If the value of X is 0, you don't look at or move any cards.")
        ruling("2017-02-09", "Merchant's Dockhand is tapped to pay the {T} cost. It can't also be one of the X untapped artifacts you tap.")
    }
}
