package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gold-Forged Sentinel
 * {6}
 * Artifact Creature — Chimera
 * 4/4
 * Flying
 */
val GoldForgedSentinel = card("Gold-Forged Sentinel") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Chimera"
    power = 4
    toughness = 4
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "James Zapata"
        flavorText = "Blessed by the gods. Coveted by mortals. Beholden to neither."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e154012-5922-45d1-8e20-d2a2b1de0785.jpg?1783939399"
    }
}
