package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wildvine Pummeler
 * {6}{G}
 * Creature — Giant Berserker
 * 6/5
 *
 * Vivid — This spell costs {1} less to cast for each color among permanents you control.
 * Reach, trample
 */
val WildvinePummeler = card("Wildvine Pummeler") {
    manaCost = "{6}{G}"
    typeLine = "Creature — Giant Berserker"
    power = 6
    toughness = 5
    oracleText = "Vivid — This spell costs {1} less to cast for each color among permanents you control.\nReach, trample"

    keywords(Keyword.REACH, Keyword.TRAMPLE)
    vividCostReduction()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Kev Walker"
        flavorText = "Stinging roots of wild magic woke the giant in a very foul mood."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11bad5c7-fe9a-4d89-a531-8d4f03d5a0e4.jpg?1767957300"
    }
}
