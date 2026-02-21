package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Lavamancer's Skill
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "{T}: This creature deals 1 damage to target creature."
 * As long as enchanted creature is a Wizard, it has "{T}: This creature deals 2 damage
 * to target creature." instead.
 *
 * Implementation uses a single ability with conditional damage amount (2 if Wizard, 1 otherwise).
 */
val LavamancersSkill = card("Lavamancer's Skill") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has \"{T}: This creature deals 1 damage to target creature.\"\nAs long as enchanted creature is a Wizard, it has \"{T}: This creature deals 2 damage to target creature.\" instead."

    auraTarget = Targets.Creature

    activatedAbility {
        cost = AbilityCost.TapAttachedCreature
        val t = target("target", TargetCreature())
        effect = DealDamageEffect(
            amount = DynamicAmount.Conditional(
                condition = EnchantedCreatureHasSubtype(Subtype("Wizard")),
                ifTrue = DynamicAmount.Fixed(2),
                ifFalse = DynamicAmount.Fixed(1)
            ),
            target = t,
            damageSource = EffectTarget.EnchantedCreature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "215"
        artist = "Scott M. Fischer"
        flavorText = "The best wizards know the shortest route between two points is a bolt of fire."
        imageUri = "https://cards.scryfall.io/large/front/0/d/0d4dd156-a2c1-4fab-b9f4-3302a4e8835a.jpg?1562898074"
    }
}
