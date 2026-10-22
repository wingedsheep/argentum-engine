package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.permanent.attachments.TransformAttachToPlayer
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState

/**
 * Settles a pending "As this permanent transforms into …, attach it to a player" choice
 * ([com.wingedsheep.sdk.scripting.AttachToPlayerAsTransforms], CR 712.20) for a permanent that
 * transformed outside the untap step — an effect that makes it day, or the day/night state-based
 * sweep. Not itself a state-based action: the choice is part of the transform, and this is the first
 * point after it where the engine can ask. It runs right after the day/night sweep and well before
 * the CR 704.5m check, so the Aura is attached before anything treats it as unattached.
 */
class TransformAttachCheck(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) : StateBasedActionCheck {
    override val name = "712.20 Attach as it transforms"
    override val order = SbaOrder.TRANSFORM_ATTACH

    override fun check(state: GameState): ExecutionResult =
        TransformAttachToPlayer.settle(state, cardRegistry, predicateEvaluator) ?: ExecutionResult.success(state)
}
