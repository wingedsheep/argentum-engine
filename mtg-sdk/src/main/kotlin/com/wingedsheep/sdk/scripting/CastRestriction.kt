package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import kotlinx.serialization.SerialName
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
     * Restrict casting based on timing rules (instant speed vs sorcery speed).
     * Uses the shared [TimingRule] to ensure consistent definition of timing across
     * both spell restrictions and activated ability timing.
     *
     * Example: An instant that says "Cast only as a sorcery" would use
     * TimingRequirement(TimingRule.SorcerySpeed).
     */
    @SerialName("TimingRequirement")
    @Serializable
    data class TimingRequirement(val timing: TimingRule) : CastRestriction

    /**
     * Restrict casting to a specific phase.
     * Example: "Cast only during combat."
     */
    @SerialName("OnlyDuringPhase")
    @Serializable
    data class OnlyDuringPhase(val phase: Phase) : CastRestriction

    /**
     * Restrict casting to a specific step.
     * Example: "Cast only during the declare attackers step."
     */
    @SerialName("OnlyDuringStep")
    @Serializable
    data class OnlyDuringStep(val step: Step) : CastRestriction

    /**
     * Restrict casting based on a game condition.
     * Example: "Cast only if you've been attacked this step."
     */
    @SerialName("CastOnlyIfCondition")
    @Serializable
    data class OnlyIfCondition(val condition: Condition) : CastRestriction

    /**
     * Composite restriction requiring multiple conditions.
     * Example: "Cast only during the declare attackers step and only if you've been attacked."
     */
    @SerialName("CastAll")
    @Serializable
    data class All(val restrictions: List<CastRestriction>) : CastRestriction
}
