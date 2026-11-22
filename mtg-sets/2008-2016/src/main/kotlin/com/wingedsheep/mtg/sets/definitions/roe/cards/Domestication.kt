package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Domestication
 * {2}{U}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * You control enchanted creature.
 * At the beginning of your end step, if enchanted creature's power is 4 or greater,
 * sacrifice this Aura.
 *
 * The end-step check is an intervening "if": it is tested both as the step begins and again
 * on resolution, reading the enchanted creature's projected power.
 */
val Domestication = card("Domestication") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nYou control enchanted creature.\nAt the beginning of your end step, if enchanted creature's power is 4 or greater, sacrifice this Aura."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.propertyOf(EffectTarget.EnchantedCreature, EntityNumericProperty.Power),
            ComparisonOperator.GTE,
            4
        )
        effect = Effects.SacrificeTarget(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1f15831-8dfd-4232-875c-efa6744c9a12.jpg?1783941998"

        ruling("2010-06-15", "Domestication's triggered ability has an \"intervening 'if' clause.\" That means (1) the ability triggers only if the enchanted creature's power is 4 or greater as your end step begins, and (2) the ability does nothing if the enchanted creature's power is 3 or less by the time it resolves.")
        ruling("2010-06-15", "Domestication can target, and can enchant, a creature with power 4 or greater. The enchanted creature's power is checked only when the triggered ability triggers and resolves.")
        ruling("2010-06-15", "If an effect changes the enchanted creature's power until end of turn, it will still have the modified power during your end step.")
    }
}
