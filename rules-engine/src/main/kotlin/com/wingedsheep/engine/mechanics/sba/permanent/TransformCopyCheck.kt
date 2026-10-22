package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.types.TransformCopy
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState

/**
 * Settles a pending "As this creature transforms into …, it becomes a copy of …" choice
 * ([com.wingedsheep.sdk.scripting.BecomesCopyAsTransforms], CR 712.20). Not itself a state-based
 * action: the copy is part of the transform, and this is the first point after it where the engine
 * can ask. It runs ahead of every real state-based action, so nothing checks the permanent as its
 * printed face first.
 */
class TransformCopyCheck(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) : StateBasedActionCheck {
    override val name = "712.20 Becomes a copy as it transforms"
    override val order = SbaOrder.TRANSFORM_COPY

    override fun check(state: GameState): ExecutionResult =
        TransformCopy.settle(state, cardRegistry, predicateEvaluator) ?: ExecutionResult.success(state)
}
