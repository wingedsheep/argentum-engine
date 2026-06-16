package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Verdant Force
 * {5}{G}{G}{G}
 * Creature — Elemental
 * 7/7
 * At the beginning of each upkeep, create a 1/1 green Saproling creature token.
 */
val VerdantForce = card("Verdant Force") {
    manaCost = "{5}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 7
    toughness = 7
    oracleText = "At the beginning of each upkeep, create a 1/1 green Saproling creature token."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            imageUri = "https://cards.scryfall.io/normal/front/e/6/e6544989-91b4-4db7-ad44-f1355f1d6e6b.jpg?1562540216"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "263"
        artist = "DiTerlizzi"
        flavorText = "Burl, scurf, and bower\nBirth fern and flower."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29bd094c-fcc1-4abf-ba3e-03a5b9b6d1c2.jpg"
    }
}
