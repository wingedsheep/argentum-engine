package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Noble Elephant
 * {3}{W}
 * Creature — Elephant
 * 2/2
 *
 * Trample; banding
 */
val NobleElephant = card("Noble Elephant") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant"
    oracleText = "Trample; banding"
    power = 2
    toughness = 2

    keywords(Keyword.TRAMPLE, Keyword.BANDING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Tony Roberts"
        flavorText = "\"Proud am I, / strong am I, / courageous in defending my children " +
            "/ and fierce in punishing what stands in my way.\"\n" +
            "—\"So the Elephant Speaks,\" Zhalfirin song"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65f399cb-dddb-422a-8d36-938b82b59e10.jpg?1562719755"
    }
}
