package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tricks of the Trade
 * {3}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+0 and can't be blocked.
 */
val TricksOfTheTrade = card("Tricks of the Trade") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+0 and can't be blocked."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 0, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBeBlocked(GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Steven Belledin"
        flavorText = "\"Pathetic. You might as well have protected your treasures with a paper box and some string.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c796ef9-4061-4f82-9ee9-3bc446804ee9.jpg?1783940501"
    }
}
