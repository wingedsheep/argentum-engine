package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Woodlot Crawler
 * {U}{B}
 * Creature — Insect
 * 2/1
 * Forestwalk
 * Protection from green
 */
val WoodlotCrawler = card("Woodlot Crawler") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Insect"
    power = 2
    toughness = 1
    oracleText = "Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)\n" +
        "Protection from green"

    keywords(Keyword.FORESTWALK)
    keywordAbility(KeywordAbility.protectionFrom(Color.GREEN))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Greg Staples"
        flavorText = "\"We learned something from the Simic. Unfortunately for them.\"\n—Nefara, Dimir agent"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11f1e6fe-e959-4030-9925-9ccc27040275.jpg?1783940017"
    }
}
