package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Life Total Conditions
// =============================================================================

/**
 * Condition: "If your life total is X or less"
 */
@SerialName("LifeTotalAtMost")
@Serializable
data class LifeTotalAtMost(val threshold: Int) : Condition {
    override val description: String = "if your life total is $threshold or less"
}

/**
 * Condition: "If your life total is X or more"
 */
@SerialName("LifeTotalAtLeast")
@Serializable
data class LifeTotalAtLeast(val threshold: Int) : Condition {
    override val description: String = "if your life total is $threshold or more"
}

/**
 * Condition: "If you have more life than an opponent"
 */
@SerialName("MoreLifeThanOpponent")
@Serializable
data object MoreLifeThanOpponent : Condition {
    override val description: String = "if you have more life than an opponent"
}

/**
 * Condition: "If you have less life than an opponent"
 */
@SerialName("LessLifeThanOpponent")
@Serializable
data object LessLifeThanOpponent : Condition {
    override val description: String = "if you have less life than an opponent"
}
