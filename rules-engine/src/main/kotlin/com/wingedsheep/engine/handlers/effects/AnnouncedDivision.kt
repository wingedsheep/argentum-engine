package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatedEffect

/**
 * A division its controller announces as the spell or ability is put on the stack (CR 601.2d;
 * CR 603.3d for triggered abilities): "N damage divided as you choose among …" or "distribute N
 * counters among …". One reading shared by the activation path (legal actions + validation) and the
 * triggered-ability path, so a sequence like Ajani, Sleeper Agent's −3 ("distribute three +1/+1
 * counters …. They gain vigilance …") announces its division the same way wherever it appears.
 *
 * The announced map rides on the stack object (`damageDistribution`, shared by damage and counters)
 * and reaches the executor as `EffectContext.damageDistribution`.
 */
object AnnouncedDivision {

    /**
     * The effect whose division is announced: the whole effect, the one divided step of a sequence,
     * or the divided payoff of a gate (Kuldotha Flamefiend — its targets are chosen now, so their
     * division is announced now too, whether or not the gate later opens). Null when there is none,
     * or more than one to tell apart — those divide at resolution instead.
     */
    fun of(effect: Effect): Effect? {
        fun divisions(e: Effect): List<Effect> = when (e) {
            is DividedDamageEffect, is DistributeCountersAmongTargetsEffect -> listOf(e)
            is CompositeEffect -> e.effects.flatMap(::divisions)
            is GatedEffect -> divisions(e.then) + (e.otherwise?.let(::divisions) ?: emptyList())
            else -> emptyList()
        }
        return divisions(effect).singleOrNull()
    }

    /** The amount [division] divides, evaluated now — a dynamic total reads [context]'s X. */
    fun total(state: GameState, division: Effect, context: EffectContext, amounts: DynamicAmountEvaluator): Int =
        when (division) {
            is DividedDamageEffect ->
                division.dynamicTotal?.let { amounts.evaluate(state, it, context) } ?: division.totalDamage
            is DistributeCountersAmongTargetsEffect -> amounts.evaluate(state, division.totalCounters, context)
            else -> error("Not a divided effect: ${division::class.simpleName}")
        }.coerceAtLeast(0)

    /** The least each chosen target may be assigned — 1 for damage (CR 601.2d), the effect's own floor for counters. */
    fun minPerTarget(division: Effect): Int = when (division) {
        is DistributeCountersAmongTargetsEffect -> division.minPerTarget
        else -> 1
    }

    /**
     * What is being divided, as the player reads it: null for damage (the client's default), the
     * printed counter name for counters ("+1/+1 counter").
     */
    fun counterUnit(division: Effect): String? =
        (division as? DistributeCountersAmongTargetsEffect)?.let { "${it.counterType.printed} counter" }
}
