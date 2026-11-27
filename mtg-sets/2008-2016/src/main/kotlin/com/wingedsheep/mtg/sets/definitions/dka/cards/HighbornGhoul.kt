package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Highborn Ghoul
 * {B}{B}
 * Creature — Zombie
 * 2/1
 * Intimidate
 */
val HighbornGhoul = card("Highborn Ghoul") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)"

    keywords(Keyword.INTIMIDATE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Volkan Baǵa"
        flavorText = "\"All the dead sleep lightly here, no matter how refined their beds may be.\"\n—Grafdigger Wulmer"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbe999ed-b419-440c-9189-1046f43d7b87.jpg"
    }
}
