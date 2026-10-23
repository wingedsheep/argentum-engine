package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Closing Statement — Strixhaven: School of Mages #169 (canonical printing)
 * {3}{W}{B} · Instant
 *
 * This spell costs {2} less to cast during your end step.
 * Destroy target creature or planeswalker you don't control. Put a +1/+1 counter on up to one
 * target creature you control.
 *
 * The discount is a self-cast [ModifySpellCost] gated on `Conditions.IsInStep(Step.END)` (`yoursOnly` by default), so
 * it applies only on your own turn's end step. "You don't control" is the literal
 * `Not(ControlledByYou)` controller predicate. The second target is `optional = true` ("up to
 * one"); each target is checked independently on resolution, so an illegal destroy target still
 * leaves the counter half to resolve.
 */
val ClosingStatement = card("Closing Statement") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "This spell costs {2} less to cast during your end step.\n" +
        "Destroy target creature or planeswalker you don't control. Put a +1/+1 counter on up to " +
        "one target creature you control."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.IsInStep(Step.END)),
        )
    }

    spell {
        val victim = target(
            TargetFilter(
                GameObjectFilter.CreatureOrPlaneswalker
                    .withControllerPredicate(ControllerPredicate.Not(ControllerPredicate.ControlledByYou))
            )
        )
        val creature = target(TargetFilter.Creature.youControl(), optional = true)
        effect = Effects.Destroy(victim) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Craig J Spearing"
        flavorText = "\"In conclusion . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/7/8/785e6d07-fe40-4723-b963-02da0a0987c7.jpg?1783927320"
    }
}
