package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Maze Behemoth
 * {5}{G}
 * Creature — Elemental
 * 5/4
 * Trample
 * Multicolored creatures you control have trample.
 *
 * Canonical printing: Dragon's Maze, the card's only printing. Maze Behemoth itself is monocolored, so
 * it has trample only from its own keyword, never from its grant.
 */
val MazeBehemoth = card("Maze Behemoth") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 4
    oracleText = "Trample\nMulticolored creatures you control have trample."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Multicolored).youControl()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Yeong-Hao Han"
        flavorText = "It returned to the leylines as quickly as it had appeared, leaving only broken bodies to mark its passage."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a7c9678-dea7-4219-bac0-9e1cef531f54.jpg?1783940035"
    }
}
