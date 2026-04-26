package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mischievous Sneakling
 * {1}{U/B}
 * Creature — Shapeshifter
 * 2/2
 *
 * Changeling (This card is every creature type.)
 * Flash
 */
val MischievousSneakling = card("Mischievous Sneakling") {
    manaCost = "{1}{U/B}"
    typeLine = "Creature — Shapeshifter"
    power = 2
    toughness = 2
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Flash"

    keywords(Keyword.CHANGELING, Keyword.FLASH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "235"
        artist = "Ron Spears"
        flavorText = "The changeling felt that the most important part of making new friends was the element of surprise."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b66fa00-2fa6-4060-9e7f-8e3fde6deb73.jpg?1767957261"
    }
}
