package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin King
 * {1}{R}{R}
 * Creature — Goblin
 * 2/2
 * Other Goblins get +1/+1 and have mountainwalk.
 */
val GoblinKing = card("Goblin King") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Other Goblins get +1/+1 and have mountainwalk."
    power = 2
    toughness = 2
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN), excludeSelf = true)
        )
    }
    staticAbility {
        ability = GrantKeyword(
            Keyword.MOUNTAINWALK,
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN), excludeSelf = true)
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Jesper Myrfors"
        flavorText = "To become king of the Goblins, one must assassinate the previous king. Thus, only the most foolish seek positions of leadership."
        imageUri = "https://cards.scryfall.io/normal/front/5/8/5873672d-37ea-4c0f-97f3-12b74fde112d.jpg"
    }
}
