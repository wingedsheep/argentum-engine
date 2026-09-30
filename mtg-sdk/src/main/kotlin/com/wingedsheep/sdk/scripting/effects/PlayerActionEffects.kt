package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** When a permission created by an effect may be used; these are special actions, not abilities. */
@Serializable
enum class PlayerActionTiming { Instant, ManaAbility, Sorcery }

/** Creates a repeatable special action, retaining the granting resolution's references and values. */
@Serializable
@SerialName("GrantPlayerAction")
data class GrantPlayerActionEffect(
    val cost: PayCost,
    val effect: Effect,
    val timing: PlayerActionTiming,
    val target: EffectTarget = EffectTarget.Controller,
    val duration: Duration = Duration.EndOfTurn,
    val actionDescription: String,
) : Effect {
    init {
        require(duration == Duration.EndOfTurn || duration == Duration.Permanent) {
            "Player action permissions support end of turn or permanent durations"
        }
    }
    override val description: String get() = "${target.description} may ${cost.description}: ${effect.description} ${duration.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = copy(
        cost = cost.applyTextReplacement(replacer), effect = effect.applyTextReplacement(replacer),
    )
}
