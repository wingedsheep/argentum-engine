package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lesser Werewolf
 * {3}{B}
 * Creature — Werewolf
 * 2/4
 *
 * {B}: If this creature's power is 1 or more, it gets -1/-0 until end of turn and put a -0/-1
 * counter on target creature blocking or blocked by this creature. Activate only during the
 * declare blockers step.
 *
 * The power check happens on resolution against the Werewolf's projected power; once it has
 * shrunk to 0 power, further activations do nothing.
 */
val LesserWerewolf = card("Lesser Werewolf") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Werewolf"
    power = 2
    toughness = 4
    oracleText = "{B}: If this creature's power is 1 or more, it gets -1/-0 until end of turn and " +
        "put a -0/-1 counter on target creature blocking or blocked by this creature. Activate " +
        "only during the declare blockers step."

    activatedAbility {
        cost = Costs.Mana("{B}")
        val paired = target(TargetFilter(GameObjectFilter.Creature.blockingOrBlockedBySource()))
        effect = Effects.If(
            condition = Conditions.CompareAmounts(DynamicAmounts.sourcePower(), ComparisonOperator.GTE, 1),
            then = Effects.ModifyStats(-1, 0, EffectTarget.Self) then
                Effects.AddCounters(CounterType.MINUS_ZERO_MINUS_ONE, 1, paired),
        )
        restrictions = listOf(ActivationRestriction.DuringStep(Step.DECLARE_BLOCKERS))
        description = "If Lesser Werewolf's power is 1 or more, it gets -1/-0 until end of turn " +
            "and put a -0/-1 counter on target creature blocking or blocked by it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Quinton Hoover"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/baae02e4-7db9-4a7b-a4ee-ecb22fcb77bd.jpg?1783948064"
    }
}
