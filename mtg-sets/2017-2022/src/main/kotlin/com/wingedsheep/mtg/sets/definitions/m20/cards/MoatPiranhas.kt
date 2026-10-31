package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Moat Piranhas (M20 #67)
 * {1}{U}  Creature — Fish  3/3
 *
 * Defender
 */
val MoatPiranhas = card("Moat Piranhas") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish"
    oracleText = "Defender (This creature can't attack.)"
    power = 3
    toughness = 3

    keywords(Keyword.DEFENDER)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Rudy Siswanto"
        flavorText = "\"Come on, it's not even that deep!\"\n—Gorin the Brazen"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/543b3f69-19be-494b-928f-e16b92560e35.jpg"
    }
}
