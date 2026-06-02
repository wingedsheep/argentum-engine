package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Willow-Wind
 * {4}{U}
 * Creature — Elemental
 * 3/4
 *
 * Flying
 * When this creature enters, scry 2.
 */
val WillowWind = card("Willow-Wind") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 4
    oracleText = "Flying\nWhen this creature enters, scry 2."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.scry(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Ignis Bruno"
        flavorText = "The branches of the willow began to sway violently. Frodo called for help, but he could hardly hear his own voice: it was blown away from him by the willow-wind and drowned in a clamor of leaves."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0adc3bff-0eb1-40e9-b954-5515babd07a3.jpg?1686968364"
    }
}
