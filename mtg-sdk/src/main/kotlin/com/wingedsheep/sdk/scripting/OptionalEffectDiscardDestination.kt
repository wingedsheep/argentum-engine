package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Optional destination replacement for a discard caused by a resolving effect, never a cost.
 *
 * [appliesTo] scopes it: its `player` is who discards, `cardFilter` which cards, and `cause` what
 * caused the discard — `ZoneChangeCause.DiscardedByOpponentEffect` is "if a spell or ability an
 * opponent controls causes you to discard a card" (Nephalia Academy). [reveal] shows the card to every
 * player as it goes to the alternate destination ("you may reveal that card and put it on top of your
 * library"), so its characteristics stay defined for discard filters and its name is public; without
 * it a library-bound discard is hidden (Library of Leng).
 */
@Serializable
@SerialName("OptionalEffectDiscardDestination")
data class OptionalEffectDiscardDestination(
    val destination: CardDestination.ToZone,
    override val appliesTo: EventPattern.DiscardEvent = EventPattern.DiscardEvent(Player.You),
    val reveal: Boolean = false,
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
    override val description: String get() = if (reveal) "You may reveal it and discard into ${destination.description} instead"
        else "You may discard into ${destination.description} instead"
}
