package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Head Games effect - target opponent puts their hand on top of their library,
 * then you search that player's library for that many cards and put them in their hand.
 * Then that player shuffles.
 *
 * "Target opponent puts the cards from their hand on top of their library.
 * Search that player's library for that many cards. The player puts those
 * cards into their hand, then shuffles."
 *
 * @property target The opponent whose hand is being replaced
 */
@SerialName("HeadGames")
@Serializable
data class HeadGamesEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "Target opponent puts the cards from their hand on top of their library. " +
        "Search that player's library for that many cards. The player puts those cards into their hand, then shuffles."
}
