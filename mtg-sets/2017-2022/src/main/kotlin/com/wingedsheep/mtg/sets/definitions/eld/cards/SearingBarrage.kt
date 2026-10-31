package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Searing Barrage
 * {4}{R}
 * Instant
 * Searing Barrage deals 5 damage to target creature.
 * Adamant — If at least three red mana was spent to cast this spell, Searing Barrage deals 3 damage
 * to that creature's controller.
 *
 * Adamant reads this spell's own cast payment ([DynamicAmounts.manaOfColorSpent], as Turn into a
 * Pumpkin does). The player damage goes to the targeted creature's controller
 * ([EffectTarget.TargetController], as Agonizing Demise does); the creature is still on the
 * battlefield then, since state-based actions wait until the spell has finished resolving.
 */
val SearingBarrage = card("Searing Barrage") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Searing Barrage deals 5 damage to target creature.\n" +
        "Adamant — If at least three red mana was spent to cast this spell, Searing Barrage deals " +
        "3 damage to that creature's controller."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, creature) then Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.manaOfColorSpent(Color.RED),
                ComparisonOperator.GTE,
                3,
            ),
            Effects.DealDamage(3, EffectTarget.TargetController),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Allen Williams"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d2f11135-e9ce-4e4c-bea7-72a46d326e40.jpg?1783932619"
    }
}
