package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Replace a turn with nothing, then perform [effect] first in the next turn that actually occurs. */
@Serializable
@SerialName("OptionalSkipTurnWith")
data class OptionalSkipTurnWith(
    val effect: Effect,
    override val appliesTo: EventPattern.TurnBeginEvent = EventPattern.TurnBeginEvent(),
    override val restrictions: List<Condition> = emptyList(),
) : ReplacementEffect {
    override val optional: Boolean get() = true
    override val description: String get() = "You may skip that turn. If you do, ${effect.description}"
    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect = copy(
        effect = effect.applyTextReplacement(replacer),
        restrictions = restrictions.map { it.applyTextReplacement(replacer) }
    )
}
