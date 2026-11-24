package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ethereal Armor
 * {W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 for each enchantment you control and has first strike.
 */
val EtherealArmor = card("Ethereal Armor") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+1 for each enchantment you control and has first strike."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        val enchantmentCount = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Enchantment).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = enchantmentCount,
            toughnessBonus = enchantmentCount
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "9"
        artist = "Daarken"
        flavorText = "Metal is scarce, smiths are pricey, and plate mail is heavy."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76960e65-e5c7-4414-b9a5-37d7b2ded4a0.jpg?1783940376"
    }
}
