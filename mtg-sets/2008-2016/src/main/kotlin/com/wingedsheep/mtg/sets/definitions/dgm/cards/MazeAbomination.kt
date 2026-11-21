package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Maze Abomination
 * {5}{B}
 * Creature — Elemental
 * 4/5
 * Deathtouch
 * Multicolored creatures you control have deathtouch.
 *
 * Canonical printing: Dragon's Maze, the card's only printing. Maze Abomination itself is monocolored, so
 * it has deathtouch only from its own keyword, never from its grant.
 */
val MazeAbomination = card("Maze Abomination") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 5
    oracleText = "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\nMulticolored creatures you control have deathtouch."

    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = GrantKeyword(
            Keyword.DEATHTOUCH,
            GroupFilter((GameObjectFilter.Creature and GameObjectFilter.Multicolored).youControl()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd84659f-4209-42a2-800a-61706470ce54.jpg?1783940039"
    }
}
