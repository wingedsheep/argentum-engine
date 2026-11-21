package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Maze Sentinel
 * {5}{W}
 * Creature — Elemental
 * 3/6
 * Vigilance
 * Multicolored creatures you control have vigilance.
 *
 * Canonical printing: Dragon's Maze, the card's only printing. Maze Sentinel itself is monocolored, so
 * it has vigilance only from its own keyword, never from its grant.
 */
val MazeSentinel = card("Maze Sentinel") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 6
    oracleText = "Vigilance\nMulticolored creatures you control have vigilance."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = GrantKeyword(
            Keyword.VIGILANCE,
            GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Multicolored).youControl()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Yeong-Hao Han"
        flavorText = "The route has been known for millennia, but only by those with no means to tell it."
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a977e2d-a2bc-42d1-be7d-36a822c6a66e.jpg?1783940044"
    }
}
