package com.wingedsheep.engine.handlers.effects.permanent.phasing

import com.wingedsheep.engine.core.standingStatics
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantPhaseIn
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * "Permanents can't phase in" ([CantPhaseIn] — The City on the Edge of Forever's Human token).
 *
 * Its own object because the lock is global: it is owned by whichever permanent prints it, not by
 * the phased-out permanent, so every path that phases a permanent in — the untap step's
 * turn-based phase-in and a "phases out until [source] leaves" link — asks the same question.
 *
 * [of] reads the standing statics once against the state *before* anything phases in, so a
 * simultaneous phase-in (CR 702.26a) is judged as a whole: a lock printer that is itself phasing in
 * doesn't exist yet and stops nothing.
 */
object PhaseInLocks {

    /** A reader over the [CantPhaseIn] statics functioning in [state]; cheap when there are none. */
    fun of(state: GameState, cardRegistry: CardRegistry, predicateEvaluator: PredicateEvaluator): Lock {
        val locks = mutableListOf<Pair<CantPhaseIn, PredicateContext>>()
        standingStatics(state, cardRegistry, predicateEvaluator, CantPhaseIn::class) { ability, source, controller ->
            locks += ability to PredicateContext(controllerId = controller, sourceId = source)
            true
        }
        return Lock(state, predicateEvaluator, locks)
    }

    class Lock internal constructor(
        private val state: GameState,
        private val predicateEvaluator: PredicateEvaluator,
        private val locks: List<Pair<CantPhaseIn, PredicateContext>>
    ) {
        /**
         * True if a [CantPhaseIn] forbids phased-out [entityId] from phasing in. A phased-out
         * permanent has no projection entry, so the filter falls back to its base card.
         */
        fun forbids(entityId: EntityId): Boolean = locks.any { (lock, context) ->
            lock.filter == GameObjectFilter.Any ||
                predicateEvaluator.matches(state, state.projectedState, entityId, lock.filter, context)
        }
    }
}
