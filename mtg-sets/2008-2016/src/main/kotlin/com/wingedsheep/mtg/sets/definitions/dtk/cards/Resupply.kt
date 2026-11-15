package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Resupply
 * {5}{W}
 * Instant
 * You gain 6 life.
 * Draw a card.
 */
val Resupply = card("Resupply") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You gain 6 life.\nDraw a card."

    spell {
        effect = Effects.GainLife(6) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Filip Burburan"
        flavorText = "\"If the scalelords are the brains of Dromoka's army, the supply caravans are its beating heart.\"\n—Baihir, Dromoka mage"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecf47c52-4065-4b81-acdc-878e74767bba.jpg?1783938613"
    }
}
