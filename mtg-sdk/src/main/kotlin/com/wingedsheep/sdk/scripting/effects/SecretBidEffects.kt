package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Secret Bid Effects
// =============================================================================

/**
 * Each player secretly chooses a number. Then those numbers are revealed.
 * Each player with the highest number loses that much life.
 * If the controller is one of those players, apply the [winnerEffect] (if any).
 *
 * The "highest bidder loses life equal to bid" behaviour is intrinsic to the
 * secret-bid mechanic and handled by the executor. The [winnerEffect] is the
 * composable reward (e.g., add counters, draw cards) applied when the controller
 * wins (or ties for highest).
 *
 * @property winnerEffect Effect applied to the source if the controller has the highest bid (or null for no reward)
 */
@SerialName("SecretBid")
@Serializable
data class SecretBidEffect(
    val winnerEffect: Effect? = null
) : Effect {
    override val description: String =
        "Each player secretly chooses a number. Then those numbers are revealed. " +
            "Each player with the highest number loses that much life." +
            (winnerEffect?.let { " If you are one of those players, ${it.description}." } ?: "")

    override fun applyTextReplacement(replacer: TextReplacer): Effect =
        copy(winnerEffect = winnerEffect?.applyTextReplacement(replacer))
}
