package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Defiant Elf
 * {G}
 * Creature — Elf
 * 1/1
 * Trample
 */
val DefiantElf = card("Defiant Elf") {
    manaCost = "{G}"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 1
    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Pete Venters"
        flavorText = "\"I lost one home when Yavimaya was destroyed. I will not lose another.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b7a0b8f-6942-40b0-8efc-234ae77855b4.jpg?1562907012"
    }
}
