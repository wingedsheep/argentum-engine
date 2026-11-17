package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Burning Anger
 * {4}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "{T}: This creature deals damage equal to its power to any target."
 *
 * The granted ability's source is the enchanted creature, so "its power" reads that
 * creature and the damage comes from it.
 */
val BurningAnger = card("Burning Anger") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature has \"{T}: This creature deals damage equal to its power to any target.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val target = target(Targets.Any)
                effect = Effects.DealDamage(DynamicAmounts.sourcePower(), target)
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Anthony Palumbo"
        flavorText = "\"With rage as your forge, your hammer can smite anything.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2058a7eb-0f78-46a4-bea5-afdfe920264f.jpg"
    }
}
