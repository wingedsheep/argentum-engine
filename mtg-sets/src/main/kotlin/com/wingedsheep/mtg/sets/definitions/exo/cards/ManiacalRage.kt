package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Maniacal Rage
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2 and can't block.
 */
val ManiacalRage = card("Maniacal Rage") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +2/+2 and can't block."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3aa840f-6a70-4674-acb7-ded0ea4397d8.jpg?1562089267"
    }
}
