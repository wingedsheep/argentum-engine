package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jade Mage
 * {1}{G}
 * Creature — Human Shaman
 * 2/1
 * {2}{G}: Create a 1/1 green Saproling creature token.
 *
 * A repeatable token maker: [Costs.Mana] plus the shared [Effects.CreateToken] facade, which spells
 * the token entirely by its printed characteristics — P/T, [Color.GREEN], and the "Saproling"
 * creature type. No bespoke token definition is needed.
 */
val JadeMage = card("Jade Mage") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 1
    oracleText = "{2}{G}: Create a 1/1 green Saproling creature token."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Izzy"
        flavorText = "\"We are one with the wild things. Life blooms from our fingertips and nature responds to our summons.\"\n—Jade creed"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32d6c8d3-04a1-4b35-b7d1-18bed82beaf4.jpg"
    }
}
