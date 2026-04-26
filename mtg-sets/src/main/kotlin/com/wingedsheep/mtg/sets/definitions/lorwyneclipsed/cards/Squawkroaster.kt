package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.Rarity

/**
 * Squawkroaster
 * {3}{R}
 * Creature — Elemental
 * &#42;/4
 *
 * Double strike
 * Vivid — Squawkroaster's power is equal to the number of colors among permanents you control.
 */
val Squawkroaster = card("Squawkroaster") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Elemental"
    oracleText = "Double strike\n" +
        "Vivid — Squawkroaster's power is equal to the number of colors among permanents you control."
    dynamicPower = CharacteristicValue.Dynamic(DynamicAmounts.colorsAmongPermanents())
    toughness = 4

    keywords(Keyword.DOUBLE_STRIKE, Keyword.VIVID)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Alessandra Pisano"
        flavorText = "As Sanar watched the tangle of chirps and flames run by, his eyes lit up. \"I want one,\" he whispered."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4112f960-70af-4e2d-bcd4-9e9cf7aac4fb.jpg?1767871978"
    }
}
