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
 *  - "for as long as the granting permanent is on the battlefield" → [WhileSourceOnBattlefield]
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
     * Permission persists indefinitely — across turns, and it survives the granting source
     * leaving play (the permission's lifecycle is owned by the game state, not the source) —
     * EXCEPT that it is revoked the moment that *same source* grants another such permission,
     * i.e. exiles another card. Only one card exiled by a given source is playable at a time;
     * each new exile supersedes the previous.
     *
     * Models "you may play that card until you exile another card with this [permanent]"
     * (Superior Foes of Spider-Man). The engine keys the superseding on the granting effect's
     * source id; a grant with no source id can't identify its siblings and so behaves like
     * [Permanent].
     */
    @SerialName("UntilSourceExilesAnother")
    @Serializable
    data object UntilSourceExilesAnother : MayPlayExpiry {
        override val description = "until you exile another card with this permanent"
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

    /**
     * Permission lasts for as long as the grant's controller controls the **granting permanent** —
     * "you may cast it for as long as you control this creature" (Taster of Wares). The exiled card
     * stays exiled either way; only the permission ends.
     *
     * Mirrors [com.wingedsheep.sdk.scripting.Duration.WhileYouControlSource] and shares its two
     * halves: the window closes when the source leaves the battlefield **or** when the source's
     * *projected* controller stops being the grant's controller, so a Threaten-style steal of the
     * source ends it just as a destroy would.
     *
     * One-way, per CR 611.2b — "it doesn't start and immediately stop again, and it doesn't last
     * forever". Two consequences the engine implements:
     *  - The grant is never created at all if the source is already gone (or already stolen) when
     *    the granting ability resolves — the rule's Master Thief example.
     *  - Once the window closes the permission is physically removed, so regaining control of the
     *    source, or a new copy of it entering, does not revive it.
     *
     * Distinct from [Permanent], which survives the source leaving play, and from
     * [UntilSourceExilesAnother], which ends only when that same source exiles another card.
     * Requires a source id on the grant; without one the window can never be evaluated and the
     * permission is not created.
     */
    @SerialName("WhileYouControlSource")
    @Serializable
    data class WhileYouControlSource(
        val sourceDescription: String = "this permanent"
    ) : MayPlayExpiry {
        override val description = "for as long as you control $sourceDescription"
    }

    /**
     * Permission lasts for as long as the **granting permanent stays on the battlefield** —
     * regardless of who controls it, and regardless of who holds the permission.
     *
     * The controller-blind sibling of [WhileYouControlSource]. It exists because a card can hand
     * this permission to a player who does *not* control the source: Shared Fate ("Each player may
     * look at cards they exiled with this enchantment, and they may play lands and cast spells
     * from among those cards") grants to every player, including the opponents its controller
     * never shares a permanent with. Keying that window off "you control the source" would revoke
     * every opponent's grant on the first state-based check.
     *
     * The window closes on the source's *zone*, so a Threaten-style steal of the source leaves the
     * permission intact — which is right for a static-ability-shaped grant: the second sentence of
     * Shared Fate keeps functioning under a new controller, and only stops when the enchantment
     * itself is gone. That is the difference the ruling turns on ("If the Shared Fate which was
     * responsible for a card being exiled leaves the battlefield, putting another Shared Fate onto
     * the battlefield will not allow you to play that card again").
     *
     * One-way, per CR 611.2b, on the same terms as [WhileYouControlSource]: no permission is
     * created at all if the source has already left when the grant would apply, and once the
     * window closes the permission is physically removed rather than gated, so the source
     * returning does not revive it. Requires a source id; without one the window can never be
     * evaluated and the permission is not created.
     */
    @SerialName("WhileSourceOnBattlefield")
    @Serializable
    data class WhileSourceOnBattlefield(
        val sourceDescription: String = "this permanent"
    ) : MayPlayExpiry {
        override val description = "for as long as $sourceDescription remains on the battlefield"
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
