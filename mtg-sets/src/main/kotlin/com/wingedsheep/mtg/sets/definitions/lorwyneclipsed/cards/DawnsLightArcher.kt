package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dawn's Light Archer
 * {2}{G}
 * Creature — Elf Archer
 * 4/2
 *
 * Flash
 * Reach
 */
val DawnsLightArcher = card("Dawn's Light Archer") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf Archer"
    power = 4
    toughness = 2
    oracleText = "Flash\nReach"

    keywords(Keyword.FLASH, Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Scott Gustafson"
        flavorText = "\"Archery is beauty in action. Your stance, your draw, your aim, and your release. All must be a single harmonious motion.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76e80656-6bcb-4d99-8bd2-ca5f75f40daf.jpg?1767873724"
    }
}
