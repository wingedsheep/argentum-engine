package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mesa Unicorn
 * {1}{W}
 * Creature — Unicorn
 * 2/2
 * Lifelink
 */
val MesaUnicorn = card("Mesa Unicorn") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Unicorn"
    power = 2
    toughness = 2
    oracleText = "Lifelink"

    keywords(Keyword.LIFELINK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Winona Nelson"
        flavorText = "The unicorns of Sursi are a manifestation of Serra's joy and compassion. They frolic and dance like children, offering blessings to anyone they encounter."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf6ed7b5-08ee-4d2c-9bf8-03a1b85b635a.jpg?1562743222"
    }
}
