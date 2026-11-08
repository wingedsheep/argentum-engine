package com.wingedsheep.engine.view.projection

import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.BlightWasPaid
import com.wingedsheep.sdk.scripting.conditions.CastChoiceIs
import com.wingedsheep.sdk.scripting.conditions.CastChoiceMade
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.Escaped
import com.wingedsheep.sdk.scripting.conditions.ExiledAsCostHadSubtype
import com.wingedsheep.sdk.scripting.conditions.ManaSpentToCastIncludes
import com.wingedsheep.sdk.scripting.conditions.MayhemCostWasPaid
import com.wingedsheep.sdk.scripting.conditions.NoManaSpentToCast
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.conditions.NumberMatches
import com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentHadSubtype
import com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentWasLegendary
import com.wingedsheep.sdk.scripting.conditions.SacrificedPermanentWasSuspected
import com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid
import com.wingedsheep.sdk.scripting.conditions.WasCast
import com.wingedsheep.sdk.scripting.conditions.WasCastFromHand
import com.wingedsheep.sdk.scripting.conditions.WasCastFromZone
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.conditions.WaterbendWasPaid
import com.wingedsheep.sdk.scripting.conditions.WebSlungCostWasPaid
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Which conditions a spell's cast already settled — the only ones [StackTextRenderer] may collapse
 * to a single branch when it shows the spell on the stack.
 *
 * Everything here reads a fact frozen as the spell was cast (CR 601.2): which optional and
 * additional costs were paid, where it was cast from, the mana spent, the value of X, choices
 * locked in, and the snapshots of what was sacrificed or exiled to pay for it. Every player saw
 * those facts happen, and nothing between casting and resolution can change them, so naming the
 * branch they pick is neither a leak nor a guess.
 *
 * Any other condition is shown in full ("If …, … . Otherwise, …"). It may read a hidden zone — a
 * hand, a library, a face-down card — whose answer would be told to every viewer through the stack
 * text, or the live board, whose answer can change before the spell resolves (View from Above's
 * "if you control a white permanent").
 *
 * This is an allow-list on purpose: a new condition type shows in full until someone decides it
 * belongs here, so the failure mode of forgetting it is a longer line of text, never a leak.
 */
internal object CastSettledConditions {

    fun isSettled(condition: Condition): Boolean = when (condition) {
        is AllConditions -> condition.conditions.all(::isSettled)
        is AnyCondition -> condition.conditions.all(::isSettled)
        is NotCondition -> isSettled(condition.condition)
        is Compare -> isSettled(condition.left) && isSettled(condition.right)
        is NumberMatches -> isSettled(condition.amount)
        // Costs paid, and how the spell was cast.
        WasKicked, BlightWasPaid, WaterbendWasPaid, SneakCostWasPaid, WebSlungCostWasPaid,
        MayhemCostWasPaid, Escaped, WasCast, WasCastFromHand, NoManaSpentToCast -> true
        is WasCastFromZone, is ManaSpentToCastIncludes, is CastChoiceMade, is CastChoiceIs -> true
        // Last-known snapshots of what paid the cost.
        SacrificedPermanentWasLegendary, SacrificedPermanentWasSuspected -> true
        is SacrificedPermanentHadSubtype, is ExiledAsCostHadSubtype -> true
        else -> false
    }

    private fun isSettled(amount: DynamicAmount): Boolean = when (amount) {
        is DynamicAmount.Fixed, DynamicAmount.XValue, DynamicAmount.CastX,
        is DynamicAmount.CastChoice, is DynamicAmount.CapturedAtCast,
        DynamicAmount.TotalManaSpent, is DynamicAmount.ManaSpentOnX, is DynamicAmount.ManaOfColorSpent,
        DynamicAmount.SnowManaSpent, is DynamicAmount.DistinctColorsManaSpent,
        DynamicAmount.PermanentsSacrificedThisWay, DynamicAmount.TotalPowerSacrificedThisWay,
        DynamicAmount.CountersRemovedAsCost -> true
        is DynamicAmount.ContextProperty -> amount.key == ContextPropertyKey.ADDITIONAL_COST_EXILED_COUNT ||
            amount.key == ContextPropertyKey.DISCARDED_AS_COST_TOTAL_MANA_VALUE
        is DynamicAmount.Add -> isSettled(amount.left) && isSettled(amount.right)
        is DynamicAmount.Subtract -> isSettled(amount.left) && isSettled(amount.right)
        is DynamicAmount.Max -> isSettled(amount.left) && isSettled(amount.right)
        is DynamicAmount.Min -> isSettled(amount.left) && isSettled(amount.right)
        is DynamicAmount.Multiply -> isSettled(amount.amount)
        is DynamicAmount.Divide -> isSettled(amount.numerator) && isSettled(amount.denominator)
        is DynamicAmount.Power -> isSettled(amount.exponent)
        is DynamicAmount.IfPositive -> isSettled(amount.amount)
        is DynamicAmount.Conditional ->
            isSettled(amount.condition) && isSettled(amount.ifTrue) && isSettled(amount.ifFalse)
        else -> false
    }
}
