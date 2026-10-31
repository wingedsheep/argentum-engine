package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Frogify
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature loses all abilities and is a blue Frog creature with base power
 * and toughness 1/1. (It loses all other card types and creature types.)
 */
val Frogify = card("Frogify") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature loses all abilities and is a blue Frog creature with base power and toughness 1/1. (It loses all other card types and creature types.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // "loses all abilities" — Layer 6
    staticAbility {
        ability = LoseAllAbilities()
    }

    // "is a blue Frog creature" — Layer 4 (card types + subtypes) + Layer 5 (color)
    staticAbility {
        ability = TransformPermanent(
            setCardTypes = setOf("CREATURE"),
            setSubtypes = setOf("Frog"),
            setColors = setOf(Color.BLUE)
        )
    }

    // "base power and toughness 1/1" — Layer 7b
    staticAbility {
        ability = SetBasePowerToughnessStatic(1, 1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Chris Seaman"
        flavorText = "That gnat was still hovering by the venison, and now both looked delicious."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b69cbb20-3c4e-480b-a330-9c6d6b39d12f.jpg?1783932656"
    }
}
