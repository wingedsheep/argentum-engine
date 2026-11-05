package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Huatli's Snubhorn
 * {1}{W}
 * Creature — Dinosaur
 * 2/2
 *
 * Vigilance
 */
val HuatlisSnubhorn = card("Huatli's Snubhorn") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dinosaur"
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)"
    power = 2
    toughness = 2

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "286"
        inBooster = false
        artist = "Randy Vargas"
        flavorText = "Don't make the mistake of thinking blunt horns can't kill."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2d88e6c-4aa8-4175-9f5d-a4c0182cdf74.jpg"
    }
}
