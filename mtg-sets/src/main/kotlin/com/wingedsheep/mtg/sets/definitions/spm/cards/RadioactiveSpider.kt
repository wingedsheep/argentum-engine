package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Radioactive Spider
 * {G}
 * Creature — Spider
 * 1/1
 * Reach
 * Deathtouch
 */
val RadioactiveSpider = card("Radioactive Spider") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 1
    toughness = 1
    oracleText = "Reach\nDeathtouch"

    keywords(Keyword.REACH, Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Pavel Kolomeyets"
        flavorText = "\"Ow! A spider! It bit me! But why is it glowing that way?\"\n—Peter Parker"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2d267f5-7f12-45f8-8fcb-e0ba3fbdeddc.jpg?1757377503"
    }
}
