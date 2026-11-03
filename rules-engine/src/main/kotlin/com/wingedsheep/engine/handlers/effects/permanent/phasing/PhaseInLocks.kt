package com.wingedsheep.engine.handlers.effects.permanent.phasing

import com.wingedsheep.engine.core.standingStatics
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.CantPhaseIn

/**
 * "Permanents can't phase in" ([CantPhaseIn] — The City on the Edge of Forever's Human token).
 *
 * Its own object because the lock is global: it is owned by whichever permanent prints it, not by
 * the phased-out permanent, so every path that phases a permanent in — the untap step's
 * turn-based phase-in and a "phases out until [source] leaves" link — asks the same question.
 *
 * Callers read it once against the state *before* anything phases in, so a simultaneous phase-in
 * (CR 702.26a) is judged as a whole: a lock printer that is itself phasing in doesn't exist yet
 * (CR 702.26b) and stops nothing.
 */
object PhaseInLocks {

    /** True if a functioning [CantPhaseIn] static forbids every phase-in in [state]. */
    fun locked(state: GameState, cardRegistry: CardRegistry, predicateEvaluator: PredicateEvaluator): Boolean =
        standingStatics(state, cardRegistry, predicateEvaluator, CantPhaseIn::class) { _, _, _ -> true }
            .isNotEmpty()
}
