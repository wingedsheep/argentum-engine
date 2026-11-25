package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Guardian Lions
 * {4}{W}
 * Creature — Cat
 * 1/6
 * Vigilance
 */
val GuardianLions = card("Guardian Lions") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)"
    power = 1
    toughness = 6

    keywords(Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Johannes Voss"
        flavorText = "Approach in peace or not at all."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3defc506-537e-4659-815d-5dab15fbf199.jpg"
    }
}
