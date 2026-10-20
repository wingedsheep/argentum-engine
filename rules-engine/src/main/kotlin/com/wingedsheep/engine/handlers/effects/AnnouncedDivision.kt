package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.CastTimeCapture
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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

    /**
     * The division a *spell* announces as it is cast (CR 601.2d): a top-level divided-damage effect,
     * or a counter division anywhere in the spell's sequence (Undercity Upheaval: "distribute X +1/+1
     * counters among any number of target creatures you control … creatures you control gain
     * vigilance"). A split that lets a target get nothing (`minPerTarget = 0` — Wicked Slumber's
     * "put a stun counter on either of them" twice) isn't a division: it is made at resolution among
     * the targets still legal then. Null when the spell divides nothing at cast.
     */
    fun ofSpell(spellEffect: Effect?): Effect? = when {
        spellEffect == null -> null
        spellEffect is DividedDamageEffect -> spellEffect
        else -> of(spellEffect)?.takeIf { it is DistributeCountersAmongTargetsEffect && it.minPerTarget >= 1 }
    }

    /**
     * The amount [division] divides, evaluated now — a dynamic total reads [context]'s X.
     *
     * [captures] are the spell's "as you cast this spell" captures (CR 601.2i). The division is
     * announced while the spell is still being cast (CR 601.2d), before those numbers are frozen onto
     * the stack object, so a total that *is* a capture (Undercity Upheaval: "distribute X +1/+1
     * counters …, where X is the number of creature cards in your graveyard as you cast this spell")
     * is read from the capture's own amount instead of the not-yet-recorded value.
     */
    fun total(
        state: GameState,
        division: Effect,
        context: EffectContext,
        amounts: DynamicAmountEvaluator,
        captures: List<CastTimeCapture> = emptyList()
    ): Int {
        fun evaluate(amount: DynamicAmount): Int {
            val captured = (amount as? DynamicAmount.CapturedAtCast)
                ?.let { ref -> captures.firstOrNull { it.captureName == ref.captureName } }
            return amounts.evaluate(state, captured?.value ?: amount, context)
        }
        return when (division) {
            is DividedDamageEffect -> division.dynamicTotal?.let(::evaluate) ?: division.totalDamage
            is DistributeCountersAmongTargetsEffect -> evaluate(division.totalCounters)
            else -> error("Not a divided effect: ${division::class.simpleName}")
        }.coerceAtLeast(0)
    }

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
