package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sun Quan, Lord of Wu
 * {4}{U}{U}
 * Legendary Creature — Human Soldier
 */
val SunQuanLordOfWu = card("Sun Quan, Lord of Wu") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "Creatures you control have horsemanship. (They can't be blocked except by creatures with horsemanship.)"

    staticAbility {
        ability = GrantKeyword(Keyword.HORSEMANSHIP, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Xu Xiaoming"
        flavorText = "\"One score and four he reigned, the Southland king: / A dragon coiled, a tiger poised below the mighty Yangtze.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6def4492-3f67-4cdb-8a25-c3ddebd125c7.jpg"
    }
}
