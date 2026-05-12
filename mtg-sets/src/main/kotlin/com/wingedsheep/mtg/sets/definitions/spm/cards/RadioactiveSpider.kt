package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Radioactive Spider
 * {G}
 * Creature — Spider
 * 1/1
 * Reach
 * Deathtouch
 */
val RadioactiveSpider = card("Radioactive Spider") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 1
    toughness = 1
    oracleText = "Reach\nDeathtouch"

    keywords(Keyword.REACH, Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
    }
}
