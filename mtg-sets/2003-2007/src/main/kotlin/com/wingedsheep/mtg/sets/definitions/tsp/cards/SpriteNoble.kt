package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sprite Noble
 * {1}{U}{U}
 * Creature — Faerie Noble
 * 2/2
 * Flying
 * Other creatures you control with flying get +0/+1.
 * {T}: Other creatures you control with flying get +1/+0 until end of turn.
 */
val SpriteNoble = card("Sprite Noble") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Noble"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Other creatures you control with flying get +0/+1.\n" +
        "{T}: Other creatures you control with flying get +1/+0 until end of turn."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 0,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl(), excludeSelf = true),
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Group.modifyStatsForAll(
            1, 0,
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl(), excludeSelf = true),
        )
        description = "{T}: Other creatures you control with flying get +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Randy Gallegos"
        flavorText = "\"By thorn and by stone, we will fight to see the day when beauty returns to this world.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0de52e53-8a45-4c0a-bb7d-6dc0d659a7c0.jpg?1783943240"
    }
}
