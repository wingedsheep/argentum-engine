package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Costs
/**
 * Raven Guild Initiate
 * {2}{U}
 * Creature — Human Wizard
 * 1/4
 * Morph—Return a Bird you control to its owner's hand.
 */
val RavenGuildInitiate = card("Raven Guild Initiate") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 4
    oracleText = "Morph—Return a Bird you control to its owner's hand."

    morphCost = Costs.pay.ReturnToHand(GameObjectFilter.Creature.withSubtype("Bird"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Christopher Moeller"
        flavorText = "\"The Raven Guild soars on winds of unease.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1e11f70-06c3-4dc5-aafe-82d65080085e.jpg?1562534262"
    }
}
