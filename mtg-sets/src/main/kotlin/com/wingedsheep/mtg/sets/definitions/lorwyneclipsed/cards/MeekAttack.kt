package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Meek Attack
 * {2}{R}
 * Enchantment
 *
 * {1}{R}: You may put a creature card with total power and toughness 5 or less from
 * your hand onto the battlefield. That creature gains haste. At the beginning of the
 * next end step, sacrifice that creature.
 */
val MeekAttack = card("Meek Attack") {
    manaCost = "{2}{R}"
    typeLine = "Enchantment"
    oracleText = "{1}{R}: You may put a creature card with total power and toughness 5 or less from " +
        "your hand onto the battlefield. That creature gains haste. At the beginning of the next end step, " +
        "sacrifice that creature."

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = EffectPatterns.putFromHand(
            filter = GameObjectFilter.Creature.totalPowerAndToughnessAtMost(5)
        ).then(
            ConditionalOnCollectionEffect(
                collection = "putting",
                ifNotEmpty = Effects.Composite(
                    Effects.GrantKeyword(
                        keyword = Keyword.HASTE,
                        target = EffectTarget.PipelineTarget("putting", 0),
                        duration = Duration.Permanent
                    ),
                    CreateDelayedTriggerEffect(
                        step = Step.END,
                        effect = Effects.SacrificeTarget(EffectTarget.PipelineTarget("putting", 0))
                    )
                )
            )
        )
        description = "Put a creature card with total power and toughness 5 or less from your hand " +
            "onto the battlefield. It gains haste. Sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "151"
        artist = "Karl Kopinski"
        flavorText = "\"Charge! Aim for the cuticles!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21157461-5435-4879-80a9-100afc5bbf4c.jpg?1769242221"
        ruling("2025-11-17", "You sacrifice the creature only if you still control it. If that creature has left the battlefield, even if it came back, you don't sacrifice it.")
    }
}
