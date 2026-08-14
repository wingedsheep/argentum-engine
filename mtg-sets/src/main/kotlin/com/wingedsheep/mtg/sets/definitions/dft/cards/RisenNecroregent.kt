package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity

/**
 * Risen Necroregent
 * {4}{B}
 * Creature — Zombie Cat Knight
 * 5/4
 * Start your engines! (If you have no speed, it starts at 1. It increases once on each of your turns when an opponent loses life. Max speed is 4.)
 * Max speed — At the beginning of your end step, create a 2/2 black Zombie creature token.
 */
val RisenNecroregent = card("Risen Necroregent") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Cat Knight"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Max speed — At the beginning of your end step, create a 2/2 black Zombie creature token."
    power = 5
    toughness = 4
    startYourEngines()
    maxSpeed {
        triggeredAbility {
            trigger = Triggers.YourEndStep
            effect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie")
            )
        }
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Inkognit"
        flavorText = "In life, she commanded the respect of her army. In death, she demands it."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5a68482a-401d-48e7-854e-46e3db07ff35.jpg"
    }
}
