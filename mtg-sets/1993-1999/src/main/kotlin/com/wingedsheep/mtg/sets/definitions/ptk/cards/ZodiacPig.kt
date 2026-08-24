package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Pig
 * {3}{B}
 * Creature — Boar
 * 3/3
 * Swampwalk
 */
val ZodiacPig = card("Zodiac Pig") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Boar"
    power = 3
    toughness = 3
    oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)"

    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Qi Baocheng"
        flavorText = "\". . . Zhong Hui and Deng Ai next led armies west: / And to the Cao, Han's hills and streams now passed. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52e364b5-55ca-4df5-8755-6643218b0969.jpg"
    }
}
