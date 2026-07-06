package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Envoy of Okinec Ahau — {2}{W}
 * Creature — Cat Advisor
 * 3/3
 * {4}{W}: Create a 1/1 colorless Gnome artifact creature token.
 */
val EnvoyOfOkinecAhau = card("Envoy of Okinec Ahau") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Advisor"
    oracleText = "{4}{W}: Create a 1/1 colorless Gnome artifact creature token."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Gnome"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Zoltan Boros"
        flavorText = "Though wary of each other at first, the Malamet and the Oltec found a common enemy in the mycoids. They began to share glyph knowledge and other crafting secrets."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96de0741-9270-4118-bb8e-f3480c75a582.jpg?1782694605"
    }
}
