package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gruul War Chant
 * {2}{R}{G}
 * Enchantment
 *
 * Attacking creatures you control get +1/+0 and have menace.
 *
 * Two statics over one group: Orcish Oriflamme's +1/+0 and Goblin War Drums' menace grant,
 * each scoped to attacking creatures you control. Menace only matters as blockers are declared,
 * when the creatures are already attacking.
 */
val GruulWarChant = card("Gruul War Chant") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment"
    oracleText = "Attacking creatures you control get +1/+0 and have menace."

    val attackers = GroupFilter(GameObjectFilter.Creature.attacking().youControl())

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = attackers)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, attackers)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Dave Kendall"
        flavorText = "\"We are the heart of the wild, the fire in its eyes, and the howl in its throat. Come, join the battle to which you were born.\"\n—Kroshkar, Gruul shaman"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df383a6a-5eb1-48e8-a5f3-f4731ddb871b.jpg?1783940029"
    }
}
