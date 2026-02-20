package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.scripting.conditions.Condition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Composite Conditions
// =============================================================================

/**
 * Condition: All of the sub-conditions must be met (AND)
 */
@SerialName("All")
@Serializable
data class AllConditions(val conditions: List<Condition>) : Condition {
    override val description: String = conditions.joinToString(" and ") { it.description }
}

/**
 * Condition: Any of the sub-conditions must be met (OR)
 */
@SerialName("Any")
@Serializable
data class AnyCondition(val conditions: List<Condition>) : Condition {
    override val description: String = conditions.joinToString(" or ") { it.description }
}

/**
 * Condition: The sub-condition must NOT be met
 */
@SerialName("Not")
@Serializable
data class NotCondition(val condition: Condition) : Condition {
    override val description: String = "if not (${condition.description})"
}
