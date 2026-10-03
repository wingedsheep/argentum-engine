package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remove a counter from the damaged permanent for each point of matching damage and prevent
 * that point. Damage above the available counter count is dealt normally. Unlike
 * [PreventDamageByRemovingCounter], prevention is bounded by the counters actually removed.
 * Unpreventable damage still removes counters, but none of that damage is prevented.
 */
@Serializable
@SerialName("PreventDamagePerCounter")
data class PreventDamagePerCounter(
    val counterType: CounterType,
    override val appliesTo: EventPattern.DamageEvent = EventPattern.DamageEvent(recipient = Recipient.Self),
) : ReplacementEffect {
    override val description: String =
        "For each 1 damage that would be dealt to ${appliesTo.recipient.description}, " +
            "if it has a ${counterType.printed} counter on it, remove a ${counterType.printed} " +
            "counter from it and prevent that 1 damage."

    override fun applyTextReplacement(replacer: TextReplacer): ReplacementEffect {
        val pattern = appliesTo.applyTextReplacement(replacer) as EventPattern.DamageEvent
        return if (pattern === appliesTo) this else copy(appliesTo = pattern)
    }
}
