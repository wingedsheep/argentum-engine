package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.EmitKeywordCounterEventEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** The printed Evolve reminder text (Gatecrash / Bloomburrow Commander wording). */
private const val EVOLVE_REMINDER =
    "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)"

/**
 * The single triggered ability that *is* Evolve (CR 702.100a): "Whenever a creature you control
 * enters, if that creature's power is greater than this creature's power and/or that creature's
 * toughness is greater than this creature's toughness, put a +1/+1 counter on this creature."
 *
 * The "if" is an intervening-if (CR 603.4), checked when the creature enters and again on
 * resolution. Both sides are [com.wingedsheep.sdk.scripting.values.DynamicAmount.EntityProperty]
 * value reads — projected power/toughness while the object is on the battlefield, last-known
 * information once it has left — so an entering creature that dies in response still evolves the
 * source off its last-known P/T, as the Gatecrash rulings require. The comparison is P against P
 * and T against T only; a 1/5 entering beside a 3/3 evolves it, a 2/2 entering beside a 3/1 does
 * too. The evolving creature's own entry never qualifies (it can't be greater than itself).
 *
 * The effect is a two-step [CompositeEffect]: place one +1/+1 counter, then
 * [EmitKeywordCounterEventEffect] (EVOLVE) — the CR 702.100b "this creature evolves" signal, emitted only
 * when the counter actually lands, so "Whenever this creature evolves" payoffs (Watchful Radstag,
 * `Triggers.self.evolves()`) fire and arbitrary +1/+1 counter placements don't.
 *
 * Standalone so a token that intrinsically has evolve can carry the same ability; each instance is
 * a separate `TriggeredAbility`, so multiple instances trigger separately (CR 702.100d).
 */
fun evolveTriggeredAbility(): TriggeredAbility {
    val entering = EffectTarget.TriggeringEntity
    return TriggeredAbility.create(
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters(),
        interveningIf = Conditions.Any(
            Conditions.CompareAmounts(
                DynamicAmounts.powerOf(entering), ComparisonOperator.GT, DynamicAmounts.powerOf(EffectTarget.Self)
            ),
            Conditions.CompareAmounts(
                DynamicAmounts.toughnessOf(entering), ComparisonOperator.GT, DynamicAmounts.toughnessOf(EffectTarget.Self)
            ),
        ),
        effect = CompositeEffect(
            listOf(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                EmitKeywordCounterEventEffect(Keyword.EVOLVE)
            )
        ),
        descriptionOverride = EVOLVE_REMINDER,
    )
}

/** Add Evolve (CR 702.100) — the display keyword plus [evolveTriggeredAbility]. */
fun CardBuilder.evolve() {
    keywordSet.add(Keyword.EVOLVE)
    triggeredAbilities.add(evolveTriggeredAbility())
}
