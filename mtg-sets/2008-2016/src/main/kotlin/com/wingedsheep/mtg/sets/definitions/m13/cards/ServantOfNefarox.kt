package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Servant of Nefarox
 * {2}{B}
 * Creature — Human Cleric
 * 3/1
 * Exalted
 */
val ServantOfNefarox = card("Servant of Nefarox") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    oracleText = "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)"
    power = 3
    toughness = 1

    keywords(Keyword.EXALTED)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "Igor Kieryluk"
        flavorText = "\"My cause is power, and at the altar of power I will gladly forfeit my soul.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e00a2b22-a473-44ae-919f-29bc8be05543.jpg?1783940490"
    }
}
