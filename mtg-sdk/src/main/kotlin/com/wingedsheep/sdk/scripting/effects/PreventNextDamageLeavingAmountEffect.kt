package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A single-instance prevention shield that leaves up to [amountToLeave] damage unprevented. */
@Serializable
@SerialName("PreventNextDamageLeavingAmount")
data class PreventNextDamageLeavingAmountEffect(
    val amountToLeave: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller,
    val eligibleSource: GameObjectFilter = GameObjectFilter.Any,
    val scope: PreventionScope = PreventionScope.AllDamage,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String =
        "Prevent all but ${amountToLeave.description} of the next " +
            (if (scope == PreventionScope.CombatOnly) "combat damage" else "damage") +
            " to ${target.description} from a chosen ${eligibleSource.description.lowercase()}"
}
