package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cast into Darkness
 * {1}{B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -2/-0 and can't block.
 */
val CastIntoDarkness = card("Cast into Darkness") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -2/-0 and can't block."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-2, 0, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Clint Cearley"
        flavorText = "In desperation the soldier prayed to Phenax, begging for the power to cheat the gorgon's gaze."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c1880d4-90c7-46a2-bbc9-d2b52971d11c.jpg?1783939438"
    }
}
