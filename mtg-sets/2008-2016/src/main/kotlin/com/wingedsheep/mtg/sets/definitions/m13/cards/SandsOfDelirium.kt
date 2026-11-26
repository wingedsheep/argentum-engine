package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sands of Delirium
 * {3}
 * Artifact
 *
 * {X}, {T}: Target player mills X cards.
 */
val SandsOfDelirium = card("Sands of Delirium") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{X}, {T}: Target player mills X cards."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(DynamicAmounts.xValue(), player)
        description = "{X}, {T}: Target player mills X cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Charles Urbach"
        flavorText = "It counts down your last few moments of sanity."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78c9d3bf-c858-42f4-bb61-3292f9a7141b.jpg?1783940457"
        ruling("2012-07-01", "If the player has fewer than X cards in their library, they put all of them into their graveyard.")
    }
}
