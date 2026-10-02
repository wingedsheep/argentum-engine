package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Optional destination replacement for a discard caused by a resolving effect, never a cost. */
@Serializable
@SerialName("OptionalEffectDiscardDestination")
data class OptionalEffectDiscardDestination(
    val destination: CardDestination.ToZone,
    override val appliesTo: EventPattern.DiscardEvent = EventPattern.DiscardEvent(Player.You),
) : ReplacementEffect {
    init {
        require(destination.player == Player.You) { "Discard destinations are relative to the discarding player" }
        require(destination.zone in setOf(com.wingedsheep.sdk.core.Zone.LIBRARY, com.wingedsheep.sdk.core.Zone.GRAVEYARD,
            com.wingedsheep.sdk.core.Zone.EXILE, com.wingedsheep.sdk.core.Zone.COMMAND)) { "Discard destinations must be non-battlefield zones" }
    }
    override fun applyTextReplacement(replacer: com.wingedsheep.sdk.scripting.text.TextReplacer): ReplacementEffect {
        val event = appliesTo.applyTextReplacement(replacer) as EventPattern.DiscardEvent
        return if (event == appliesTo) this else copy(appliesTo = event)
    }
    override val optional: Boolean get() = true
    override val description: String get() = "You may discard into ${destination.description} instead"
}
