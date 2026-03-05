package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mantis Rider
 * {U}{R}{W}
 * Creature — Human Monk
 * 3/3
 * Flying, vigilance, haste
 */
val MantisRider = card("Mantis Rider") {
    manaCost = "{U}{R}{W}"
    typeLine = "Creature — Human Monk"
    power = 3
    toughness = 3
    oracleText = "Flying, vigilance, haste"

    keywords(Keyword.FLYING, Keyword.VIGILANCE, Keyword.HASTE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Johann Bodin"
        flavorText = "Mantis riders know their mounts owe them no allegiance. Even a mantis ridden for years would consume a rider who loses focus for only a moment."
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82d5ce46-7118-4ede-ba1d-c387e7ce16e7.jpg?1562789458"
    }
}
