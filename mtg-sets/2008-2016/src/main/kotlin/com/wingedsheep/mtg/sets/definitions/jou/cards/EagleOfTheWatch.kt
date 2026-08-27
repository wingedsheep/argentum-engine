package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Eagle of the Watch
 * {2}{W}
 * Creature — Bird
 * 2/1
 * Flying, vigilance
 */
val EagleOfTheWatch = card("Eagle of the Watch") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 1
    oracleText = "Flying, vigilance"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Scott Murphy"
        flavorText = "\"Even from miles away, I could see our eagles circling. That's when I gave the command to pick up the pace. I knew we were needed at home.\"\n—Kanlos, Akroan captain"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc4b6fb1-dc06-48f4-aae2-aa8bbb573548.jpg?1783939461"
    }
}
