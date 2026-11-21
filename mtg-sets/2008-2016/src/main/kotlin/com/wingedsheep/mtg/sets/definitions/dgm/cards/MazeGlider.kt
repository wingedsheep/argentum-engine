package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Maze Glider
 * {5}{U}
 * Creature — Elemental
 * 3/5
 * Flying
 * Multicolored creatures you control have flying.
 *
 * Canonical printing: Dragon's Maze, the card's only printing. Maze Glider itself is monocolored, so
 * it has flying only from its own keyword, never from its grant.
 */
val MazeGlider = card("Maze Glider") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 5
    oracleText = "Flying\nMulticolored creatures you control have flying."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = GrantKeyword(
            Keyword.FLYING,
            GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Multicolored).youControl()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "13"
        artist = "Yeong-Hao Han"
        flavorText = "The pattern of the Implicit Maze extends in more than two dimensions."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1d20281-49c0-4fd0-91f2-390506ac33f6.jpg?1783940042"
    }
}
