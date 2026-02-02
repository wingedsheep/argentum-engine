package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Composite Conditions
// =============================================================================

/**
 * Condition: All of the sub-conditions must be met (AND)
 */
@Serializable
data class AllConditions(val conditions: List<Condition>) : Condition {
    override val description: String = conditions.joinToString(" and ") { it.description }
}

/**
 * Condition: Any of the sub-conditions must be met (OR)
 */
@Serializable
data class AnyCondition(val conditions: List<Condition>) : Condition {
    override val description: String = conditions.joinToString(" or ") { it.description }
}

/**
 * Condition: The sub-condition must NOT be met
 */
@Serializable
data class NotCondition(val condition: Condition) : Condition {
    override val description: String = "if not (${condition.description})"
}
