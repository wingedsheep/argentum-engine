package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The one reading of a [TargetObject]'s dynamic target count — [TargetObject.dynamicMaxCount] and
 * [TargetObject.dynamicMinCount] — shared by every site that has to turn "X target creatures" or
 * "up to X target creatures" into numbers: cast validation ([TargetValidator]), the modal cast's
 * per-mode decision, the trigger snapshot and legal-action enumeration. Each of those used to carry
 * its own copy of the XValue special case; one copy per site is how the offered count and the
 * validated count drift apart.
 *
 * X is announced before targets are chosen (CR 601.2b, then 601.2c), so a site that knows the
 * announced X passes it; [DynamicAmount.XValue] is read straight off it. Every other amount is
 * read from the board through the caller's [evaluate], which carries whatever context the site has
 * (a trigger's scry count, a reflexive trigger's stored collection).
 */
object DynamicTargetCount {

    /** A requirement's resolved target-count bounds. A null [max] means "no dynamic cap here". */
    data class Bounds(val min: Int, val max: Int?)

    /** The [TargetObject] carrying the count, looking through an "another target" wrapper. */
    fun objectOf(requirement: TargetRequirement): TargetObject? = when (requirement) {
        is TargetObject -> requirement
        is TargetOther -> objectOf(requirement.baseRequirement)
        else -> null
    }

    /**
     * Resolve [amount] — null when it is [DynamicAmount.XValue] and X isn't announced yet, or when
     * the board can't be read (the caller then keeps its static fallback).
     */
    fun resolve(amount: DynamicAmount, xValue: Int?, evaluate: (DynamicAmount) -> Int): Int? {
        if (amount == DynamicAmount.XValue) return xValue?.coerceAtLeast(0)
        return try {
            evaluate(amount).coerceAtLeast(0)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The bounds of [requirement]: its dynamic maximum when it has one (null otherwise, so the
     * caller falls back to its own static or unlimited cap), and its minimum — the resolved
     * [TargetObject.dynamicMinCount] clamped to that maximum, or the static
     * [TargetRequirement.effectiveMinCount] when there is no dynamic minimum or X is not yet known.
     */
    fun bounds(requirement: TargetRequirement, xValue: Int?, evaluate: (DynamicAmount) -> Int): Bounds {
        val obj = objectOf(requirement) ?: return Bounds(requirement.effectiveMinCount, null)
        val max = obj.dynamicMaxCount?.let { resolve(it, xValue, evaluate) }
        val min = obj.dynamicMinCount?.let { resolve(it, xValue, evaluate) }
            ?.let { if (max != null) minOf(it, max) else it }
            ?: requirement.effectiveMinCount
        return Bounds(min, max)
    }
}
