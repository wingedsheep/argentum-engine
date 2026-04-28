package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Step
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes when a "may play from exile" permission ends.
 *
 * Used by [GrantMayPlayFromExileEffect] to express durations like:
 *  - "until end of turn" → [EndOfTurn]
 *  - "for as long as it remains exiled" → [Permanent]
 *  - "until your next end step" → `UntilControllerStep(Step.END, includeCurrentTurn = true)`
 *  - "until end of your next turn" → `UntilControllerStep(Step.CLEANUP, includeCurrentTurn = false)`
 *  - "until your next upkeep" → `UntilControllerStep(Step.UPKEEP, includeCurrentTurn = false)`
 *
 * Implementation note: removal of expired permissions is currently driven by the cleanup
 * step (see CleanupPhaseManager). [UntilControllerStep] therefore behaves correctly for
 * end-of-turn-aligned steps (END, CLEANUP); other steps still expire at the cleanup of the
 * matching turn but not at the precise moment the step is entered.
 */
@Serializable
sealed interface MayPlayExpiry {
    val description: String

    /** Permission ends at the cleanup step of the current turn. */
    @SerialName("EndOfTurn")
    @Serializable
    data object EndOfTurn : MayPlayExpiry {
        override val description = "until end of turn"
    }

    /** Permission persists for as long as the card remains exiled. */
    @SerialName("Permanent")
    @Serializable
    data object Permanent : MayPlayExpiry {
        override val description = "for as long as it remains exiled"
    }

    /**
     * Permission ends at the controller's next [step]. When the trigger fires on the
     * controller's own turn and that step has not yet been reached, [includeCurrentTurn]
     * decides whether THIS turn's instance counts as "next":
     *  - `true` (default) — this turn's matching step counts. Use for "your next [step]" wording.
     *  - `false` — always extend to the controller's next turn. Use for "end of your next turn".
     */
    @SerialName("UntilControllerStep")
    @Serializable
    data class UntilControllerStep(
        val step: Step,
        val includeCurrentTurn: Boolean = true
    ) : MayPlayExpiry {
        override val description: String = if (includeCurrentTurn) {
            "until your next ${step.displayName.lowercase()}"
        } else {
            "until the ${step.displayName.lowercase()} of your next turn"
        }
    }

    companion object {
        /** "Until the end of your next turn" — never expires this turn, even on your own turn. */
        val UntilEndOfNextTurn: MayPlayExpiry =
            UntilControllerStep(Step.CLEANUP, includeCurrentTurn = false)

        /** "Until your next end step" — this turn's end step counts when on your own turn. */
        val UntilNextEndStep: MayPlayExpiry =
            UntilControllerStep(Step.END, includeCurrentTurn = true)
    }
}
