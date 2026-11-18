package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Crippling Blight
 * {B}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets -1/-1 and can't block.
 */
val CripplingBlight = card("Crippling Blight") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets -1/-1 and can't block."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-1, -1, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Lucas Graciano"
        flavorText = "\"Still alive? No matter. I'll leave you as a warning to others who would oppose me.\"\n—Vish Kal, Blood Arbiter"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a96a5d6-6527-4018-923e-7e850fda106a.jpg?1783940497"
    }
}
