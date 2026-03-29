package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Brightblade Stoat
 * {1}{W}
 * Creature — Weasel Soldier
 * 2/2
 * First strike, lifelink
 */
val BrightbladeStoat = card("Brightblade Stoat") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Weasel Soldier"
    oracleText = "First strike, lifelink"
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Lius Lasahido"
        flavorText = "Brightblades are trained to constantly mind the sun's position, adjusting the angle of their dagger to maximize glare."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df7fea2e-7414-4bc8-adb0-9342e174c009.jpg?1721425778"
    }
}
