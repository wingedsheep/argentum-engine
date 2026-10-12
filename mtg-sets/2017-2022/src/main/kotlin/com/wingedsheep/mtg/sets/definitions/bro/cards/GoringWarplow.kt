package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Goring Warplow
 * {6}
 * Artifact Creature — Construct
 * 5/4
 * Prototype {1}{B} — 1/1
 * Deathtouch
 */
val GoringWarplow = card("Goring Warplow") {
    manaCost = "{6}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Construct"
    power = 5
    toughness = 4
    oracleText = "Prototype {1}{B} — 1/1 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Deathtouch"

    keywordAbility(KeywordAbility.prototype("{1}{B}", 1, 1))
    keywords(Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Wayne Reynolds"
        flavorText = "Built to crush walls and wills in equal measure."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d927dfd-ae66-4235-a61b-39c85d0c1222.jpg?1783920076"
    }
}
