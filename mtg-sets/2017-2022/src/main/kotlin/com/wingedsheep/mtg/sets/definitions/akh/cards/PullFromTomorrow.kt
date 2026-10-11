package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pull from Tomorrow
 * {X}{U}{U}
 * Instant
 * Draw X cards, then discard a card.
 */
val PullFromTomorrow = card("Pull from Tomorrow") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw X cards, then discard a card."

    spell {
        effect = Effects.DrawCards(DynamicAmounts.xValue()) then Effects.Discard(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Sara Winters"
        flavorText = "\"I do things in my own time.\"\n—Etha, initiate of Rhet crop"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1eccf27-c934-45fb-87ae-67006cd4e0ed.jpg?1783936518"
    }
}
