package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents timing and conditional restrictions on when an activated ability can be used.
 * Used for abilities like "Activate only during your turn, before attackers are declared."
 *
 * The engine enforces these restrictions during legality checks.
 */
@Serializable
sealed interface ActivationRestriction {

    /**
     * Restrict activation to only during your turn.
     * Example: "Activate only during your turn."
     */
    @SerialName("OnlyDuringYourTurn")
    @Serializable
    data object OnlyDuringYourTurn : ActivationRestriction

    /**
     * Restrict activation to before a specific step.
     * Example: "Activate only before attackers are declared."
     */
    @SerialName("BeforeStep")
    @Serializable
    data class BeforeStep(val step: Step) : ActivationRestriction

    /**
     * Restrict activation to during a specific phase.
     * Example: "Activate only during combat."
     */
    @SerialName("DuringPhase")
    @Serializable
    data class DuringPhase(val phase: Phase) : ActivationRestriction

    /**
     * Restrict activation to during a specific step.
     * Example: "Activate only during the declare blockers step."
     */
    @SerialName("DuringStep")
    @Serializable
    data class DuringStep(val step: Step) : ActivationRestriction

    /**
     * Restrict activation based on a game condition.
     * Example: "Activate only if you control no creatures."
     */
    @SerialName("ActivationOnlyIfCondition")
    @Serializable
    data class OnlyIfCondition(val condition: Condition) : ActivationRestriction

    /**
     * Composite restriction requiring multiple conditions.
     * Example: "Activate only during your turn, before attackers are declared."
     */
    @SerialName("ActivationAll")
    @Serializable
    data class All(val restrictions: List<ActivationRestriction>) : ActivationRestriction {
        constructor(vararg restrictions: ActivationRestriction) : this(restrictions.toList())
    }
}
