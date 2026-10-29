package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sphinx's Revelation
 * {X}{W}{U}{U}
 * Instant
 * You gain X life and draw X cards.
 */
val SphinxsRevelation = card("Sphinx's Revelation") {
    manaCost = "{X}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "You gain X life and draw X cards."

    spell {
        effect = Effects.GainLife(DynamicAmounts.xValue()) then Effects.DrawCards(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "200"
        artist = "Slawomir Maniak"
        flavorText = "\"Let the knowledge of absolute law inspire you to live a life of absolute order.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/0/404d9413-ef57-4b6e-8584-48a1dc7fe6f1.jpg?1783940331"
        ruling("2012-10-01", "If the value chosen for X is larger than the number of cards left in your library, you'll lose the game the next time state-based actions are performed.")
    }
}
