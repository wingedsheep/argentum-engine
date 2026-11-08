package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.OptionalEffectDiscardDestination
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reads a shield over its controller's hand against effect discards: "if an effect causes you to
 * discard a card, … you may put it on top of your library instead" (Library of Leng), "if a spell or
 * ability an opponent controls causes you to discard a card, you may reveal that card and put it on
 * top of your library instead" (Nephalia Academy). The discarded card comes back as the next draw,
 * so the opponent's discard spell trades for a draw step instead of a card.
 *
 * Only a shield over its controller ("you") that sends the card somewhere other than the graveyard
 * reads; one scoped to opponents' discards is no shield of ours.
 */
internal object DiscardShields {
    fun reads(replacement: ReplacementEffect): Boolean =
        replacement is OptionalEffectDiscardDestination &&
            replacement.appliesTo.player == Player.You &&
            replacement.destination.zone != Zone.GRAVEYARD
}
