package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * *Why* a permanent became tapped — the cause carried on the tap event so a trigger can say
 * "becomes tapped **to pay a teamwork cost**" (Agent Maria Hill) rather than merely "becomes
 * tapped".
 *
 * Tapping is a single transition (CR 701.26a — only untapped permanents can be tapped), and this
 * enum is the vocabulary for naming what caused it. It is deliberately an *enum, not a boolean*:
 * crew, saddle, convoke, declaring an attacker and mana abilities are all distinct tap causes that
 * already exist in the engine, and a future card naming any of them adds a constant here plus a
 * classification at that tap site — without disturbing the cards that already read [TEAMWORK].
 *
 * **Where a cause can be attached — three sites, not one.** Nearly every tap runs through the tap
 * atom (`com.wingedsheep.engine.core.tap`), which takes the reason as a parameter; that is the
 * first place to look. But two sites build a `com.wingedsheep.engine.core.TappedEvent` *by hand*
 * and never call the atom, because their permanent is sacrificed rather than left tapped
 * (a `{T}, Sacrifice this: Add …` mana source, e.g. a Treasure):
 * `ManaPaymentWindow.tapOrSacrifice` and `ManaPaymentContinuationResumer`. Both pass no reason and
 * so report [UNSPECIFIED] — the honest answer for a mana tap today — but neither inherits a change
 * made to the atom, so anyone classifying a mana-flavoured cause must update all three by hand.
 *
 * **Only [TEAMWORK] is classified today; every other tap site reports [UNSPECIFIED].** That is the
 * honest default rather than an approximation: a tap whose cause the engine has not been taught to
 * name must not silently masquerade as some other cause. Under-claiming makes a "becomes tapped for
 * reason X" trigger stay silent when it should have fired only if X is one of the *unclassified*
 * causes — which no card reads yet; over-claiming would make it fire wrongly for every card that
 * reads X. To classify a further cause: add the constant, pass it at that cause's tap site (one
 * chokepoint per cause — e.g. `AttackPhaseManager` for attack taps, `CrewVehicleHandler` for crew;
 * for a mana cause, all three sites listed above), and cover both directions with a test.
 *
 * Matched by [EventPattern.TapEvent.reason]; a null there means "any cause", which is what every
 * pre-existing "becomes tapped" trigger keeps meaning.
 */
@Serializable
enum class TapReason {
    /**
     * The cause is not classified. The default for every tap the engine performs — attacking,
     * crew, saddle, convoke, mana abilities, a `{T}` activation cost, and any "tap target
     * permanent" effect. Never matches a trigger that asks for a specific cause.
     */
    UNSPECIFIED,

    /**
     * The permanent was tapped to pay a spell's **teamwork** additional cost (CR 702.194a — "as an
     * additional cost to cast this spell, you may tap any number of creatures you control with
     * total power N or more"). Stamped by the cast handler on the creatures the caster tapped for a
     * cost declared under [ChoiceSlot.TEAMWORK], and read by "whenever this becomes tapped to pay a
     * teamwork cost" (Agent Maria Hill).
     */
    TEAMWORK;

    /** Trailing clause for a trigger's printed description ("… becomes tapped **to pay a teamwork cost**"). */
    val description: String
        get() = when (this) {
            UNSPECIFIED -> ""
            TEAMWORK -> "to pay a teamwork cost"
        }

    companion object {
        /**
         * The tap cause that an optional additional cost declared under [slot] gives the permanents
         * it taps (CR 601.2b — the cost is paid because the caster declared that mechanic).
         *
         * Keying off the *declared choice slot* rather than off the cost atom is what keeps the
         * classification honest: `CostAtom.VariablePermanents(action = TAP)` is a generic atom that
         * any mechanic may reuse, so the atom alone cannot say a tap was a teamwork tap — the slot
         * the caster declared can. Slots with no named tap cause map to [UNSPECIFIED].
         */
        fun forChoiceSlot(slot: ChoiceSlot?): TapReason = when (slot) {
            ChoiceSlot.TEAMWORK -> TEAMWORK
            else -> UNSPECIFIED
        }
    }
}
