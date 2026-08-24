package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zodiac Ox
 * {3}{G}
 * Creature — Ox
 * 3/3
 * Swampwalk
 */
val ZodiacOx = card("Zodiac Ox") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ox"
    power = 3
    toughness = 3
    oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)"

    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "Ai Desheng"
        flavorText = "\". . . Cao's abdication changed the face of all; / No mighty battles marked the Southland's fall. . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a731b3a-0065-448d-841c-28700d78a4fd.jpg"
    }
}
