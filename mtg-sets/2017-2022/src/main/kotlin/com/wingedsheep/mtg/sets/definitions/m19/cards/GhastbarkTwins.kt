package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ghastbark Twins
 * {5}{G}{G}
 * Creature — Treefolk
 * 7/7
 * Trample
 * This creature can block an additional creature each combat.
 */
val GhastbarkTwins = card("Ghastbark Twins") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 7
    toughness = 7
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "This creature can block an additional creature each combat."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(
            count = 1,
            filter = GroupFilter.source(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Zack Stella"
        flavorText = "They grow closer with every passing season."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d470e441-3520-4669-aaa5-f0c57829352a.jpg?1783934537"
    }
}
