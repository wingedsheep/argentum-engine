package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wu Elite Cavalry
 * {3}{U}
 * Creature — Human Soldier
 * 2/3
 */
val WuEliteCavalry = card("Wu Elite Cavalry") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)"

    keywords(Keyword.HORSEMANSHIP)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Li Wang"
        flavorText = "At the second battle of Ruxu, the brave Wu general Gan Ning raided Cao Cao's camp of 400,000 men with only 100 cavalry. Not a single man or horse was lost."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cf562e2-be8e-4514-b2c8-3268dc1ab0db.jpg"
    }
}
