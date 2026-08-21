package com.wingedsheep.sdk.core

/**
 * A named part of a turn as *printed cards talk about it* — the granularity a card uses when it
 * offers a player a choice between "draw step, main phase, or combat phase" (Fatespinner).
 *
 * Deliberately coarser than [Step] and finer than [Phase]: one entry covers every [Step] that
 * belongs to it, which is what makes "skip each instance of the chosen step or phase this turn"
 * expressible as a single value. [MAIN_PHASE] covers both the precombat and the postcombat main
 * phase (CR 505.1 — they are individually and collectively the main phase), and [COMBAT_PHASE]
 * covers all five combat steps *and* any additional combat phase created that turn.
 */
enum class TurnPart(val displayName: String) {
    /** The draw step (CR 504). */
    DRAW_STEP("draw step"),

    /** Both main phases (CR 505.1). */
    MAIN_PHASE("main phase"),

    /** Every step of every combat phase this turn (CR 506). */
    COMBAT_PHASE("combat phase");

    /** True when [step] belongs to this part of the turn. */
    fun covers(step: Step): Boolean = when (this) {
        DRAW_STEP -> step == Step.DRAW
        MAIN_PHASE -> step.isMainPhase
        COMBAT_PHASE -> step.phase == Phase.COMBAT
    }
}
