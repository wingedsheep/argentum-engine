package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Rat
 * {B}
 * Creature — Rat
 * 1 / 1
 *
 * Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)
 */
val ZodiacRat = card("Zodiac Rat") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 1
    toughness = 1
    oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)"

    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Qi Baocheng"
        flavorText = "\" . . . Cao Pi, Cao Rui, Fang, Mao, and briefly, Huan— / The Sima took the empire in their turn. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb6e5814-e1b5-48d2-9c4f-62b5727f333f.jpg"
    }
}
