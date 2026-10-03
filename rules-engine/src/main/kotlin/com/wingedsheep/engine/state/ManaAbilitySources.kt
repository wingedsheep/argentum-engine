package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.ManaAbilitySourcesContinuation
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.model.EntityId

/** Nested restrictions intersect. Existing floating mana is unaffected. */
fun GameState.manaAbilitySourceAllowed(
    playerId: EntityId,
    sourceId: EntityId,
    evaluator: PredicateEvaluator,
): Boolean = continuationStack.all { frame ->
    frame !is ManaAbilitySourcesContinuation || frame.playerId != playerId ||
        evaluator.matches(this, projectedState, sourceId, frame.sources,
            PredicateContext.fromEffectContext(frame.context.copy(controllerId = playerId)))
}
