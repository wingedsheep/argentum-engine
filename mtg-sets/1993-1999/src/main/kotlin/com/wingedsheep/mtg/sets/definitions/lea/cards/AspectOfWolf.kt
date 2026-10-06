package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.dsl.divRoundedUp
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aspect of Wolf
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +X/+Y, where X is half the number of Forests you control,
 * rounded down, and Y is half the number of Forests you control, rounded up.
 */
val AspectOfWolf = card("Aspect of Wolf") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +X/+Y, where X is half the number of Forests you control, " +
        "rounded down, and Y is half the number of Forests you control, rounded up."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.FOREST)
            ).count() / 2,
            toughnessBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.FOREST)
            ).count() divRoundedUp 2
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd9ac9e6-1395-4fbd-80e2-645f0d910c29.jpg?1783948679"
    }
}
