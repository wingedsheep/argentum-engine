package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Steeple Roc
 * {4}{W}
 * Creature — Bird
 * 3/1
 * Flying, first strike
 */
val SteepleRoc = card("Steeple Roc") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 1
    oracleText = "Flying, first strike"

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "David Palumbo"
        flavorText = "\"Sometimes it forgets to loosen its grip before taking flight. I lost my roof that way.\"\n—Ecaban, Boros scout"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fecafab-97f4-40ed-bc43-d186eb2f3af6.jpg?1783940043"
    }
}
