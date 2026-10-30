package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Oko's Accomplices
 * {2}{U}
 * Creature — Faerie
 * 2/3
 *
 * Flying
 *
 * ELD #310 — a planeswalker-deck card, not in draft boosters.
 */
val OkosAccomplices = card("Oko's Accomplices") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie"
    oracleText = "Flying"
    power = 2
    toughness = 3
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "310"
        artist = "Zezhou Chen"
        flavorText = "They function as everything from assassins to jesters in the name of their fae king."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71c52b50-35aa-4858-8c8e-c81dcb29a7fc.jpg?1783932554"
    }
}
