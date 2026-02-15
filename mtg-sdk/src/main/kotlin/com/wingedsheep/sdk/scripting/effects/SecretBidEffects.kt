package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Secret Bid Effects
// =============================================================================

/**
 * Each player secretly chooses a number. Then those numbers are revealed.
 * Each player with the highest number loses that much life.
 * If the controller is one of those players, put counters on the source.
 *
 * Used for Menacing Ogre's enters-the-battlefield trigger.
 *
 * @property counterType Type of counter to put on the source (e.g., "+1/+1")
 * @property counterCount Number of counters to put on the source if controller has the highest bid
 */
@SerialName("SecretBid")
@Serializable
data class SecretBidEffect(
    val counterType: String = "+1/+1",
    val counterCount: Int = 2
) : Effect {
    override val description: String =
        "Each player secretly chooses a number. Then those numbers are revealed. " +
            "Each player with the highest number loses that much life. " +
            "If you are one of those players, put $counterCount $counterType counters on this creature."
}
