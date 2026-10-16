package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * All That Glitters
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 for each artifact and/or enchantment you control.
 */
val AllThatGlitters = card("All That Glitters") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 for each artifact and/or enchantment you control."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactOrEnchantment).count(),
            toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactOrEnchantment).count()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Iain McCaig"
        flavorText = "A faerie's glee at her trove quickly fades to contentment, then to boredom, then to an urge to steal more."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9713032-2956-4564-b5f5-2dd16245a4e6.jpg?1783932680"
        ruling("2019-10-04", "A permanent that's both an artifact and an enchantment is counted only once.")
        ruling("2019-10-04", "You still control Auras that you put onto the battlefield attached to a permanent you don't control.")
    }
}
