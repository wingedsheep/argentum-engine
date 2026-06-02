package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Bundles a [EventPattern] filter with a [TriggerBinding] to fully specify
 * when a triggered ability fires.
 *
 * Used by the [Triggers] facade and [TriggeredAbilityBuilder] so that
 * a single assignment like `trigger = Triggers.EntersBattlefield` carries
 * both the event type and the binding information.
 */
@Serializable
data class TriggerSpec(
    val event: EventPattern,
    val binding: TriggerBinding = TriggerBinding.SELF
) {
    /**
     * Returns a copy with the "you control" controller filter added.
     * Only applies to [EventPattern.ZoneChangeEvent]-based triggers.
     */
    fun youControl(): TriggerSpec {
        val zce = event as? EventPattern.ZoneChangeEvent
            ?: error("youControl() only applies to ZoneChangeEvent triggers")
        return copy(event = zce.copy(filter = zce.filter.youControl()))
    }
}
