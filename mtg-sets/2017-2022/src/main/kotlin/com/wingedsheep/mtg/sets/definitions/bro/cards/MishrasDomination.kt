package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * "As long as you control enchanted creature" — shared by both statics, the second negated for the
 * "Otherwise" branch. Declared ahead of the card because top-level properties initialize in file order.
 */
private val YouControlEnchantedCreature = Conditions.EnchantedPermanentMatches(
    GameObjectFilter.Any.youControl()
)

/**
 * Mishra's Domination — The Brothers' War #142
 * {1}{R} · Enchantment — Aura
 *
 * Enchant creature
 * As long as you control enchanted creature, it gets +2/+2. Otherwise, it can't block.
 *
 * Same complementary-condition shape as Burden of Proof: one condition, checked continuously, so
 * the branch flips if control of the creature (or of the Aura) changes.
 */
val MishrasDomination = card("Mishra's Domination") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "As long as you control enchanted creature, it gets +2/+2. Otherwise, it can't block."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        condition = YouControlEnchantedCreature
        ability = ModifyStats(+2, +2, Filters.EnchantedCreature)
    }

    staticAbility {
        condition = Conditions.Not(YouControlEnchantedCreature)
        ability = CantBlock(Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Fajareka Setiawan"
        flavorText = "Power radiated from the Weakstone, binding the dragon engine to Mishra's command."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87c71fb1-a30b-4e0e-a6d6-4656cfff2da1.jpg?1783920067"
    }
}
