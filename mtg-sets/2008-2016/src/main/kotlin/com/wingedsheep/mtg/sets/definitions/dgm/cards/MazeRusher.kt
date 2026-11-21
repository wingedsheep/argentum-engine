package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Maze Rusher
 * {5}{R}
 * Creature — Elemental
 * 6/3
 * Haste
 * Multicolored creatures you control have haste.
 *
 * Canonical printing: Dragon's Maze, the card's only printing. Maze Rusher itself is monocolored, so
 * it has haste only from its own keyword, never from its grant.
 */
val MazeRusher = card("Maze Rusher") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 6
    toughness = 3
    oracleText = "Haste\nMulticolored creatures you control have haste."

    keywords(Keyword.HASTE)

    staticAbility {
        ability = GrantKeyword(
            Keyword.HASTE,
            GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Multicolored).youControl()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Yeong-Hao Han"
        flavorText = "\"The maze seems to fight us at every turn, resisting our efforts to solve it.\"\n—Niv-Mizzet"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/864d2eb8-e27f-4f84-9725-d2ae6446e217.jpg?1783940037"
    }
}
