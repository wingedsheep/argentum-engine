package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Siegecraft
 * {3}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+4.
 */
val Siegecraft = card("Siegecraft") {
    manaCost = "{3}{W}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+4."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 4, GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Viktor Titov"
        flavorText = "\"They thought their fortress impregnable . . . until we marched up with ours, and blocked out the sun.\" —Golran, dragonscale captain"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbd17ef9-9f1b-4937-a60a-d7175f04eef2.jpg?1562796498"
    }
}
