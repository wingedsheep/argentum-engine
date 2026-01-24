package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import kotlinx.serialization.Serializable

/**
 * Represents timing and conditional restrictions on when a spell can be cast.
 * Used for cards like "Cast this spell only during the declare attackers step."
 *
 * The engine enforces these restrictions during legality checks.
 */
@Serializable
sealed interface CastRestriction {

    /**
     * Restrict casting to a specific phase.
     * Example: "Cast only during combat."
     */
    @Serializable
    data class OnlyDuringPhase(val phase: Phase) : CastRestriction

    /**
     * Restrict casting to a specific step.
     * Example: "Cast only during the declare attackers step."
     */
    @Serializable
    data class OnlyDuringStep(val step: Step) : CastRestriction

    /**
     * Restrict casting based on a game condition.
     * Example: "Cast only if you've been attacked this step."
     */
    @Serializable
    data class OnlyIfCondition(val condition: Condition) : CastRestriction

    /**
     * Composite restriction requiring multiple conditions.
     * Example: "Cast only during the declare attackers step and only if you've been attacked."
     */
    @Serializable
    data class All(val restrictions: List<CastRestriction>) : CastRestriction
}
