package com.wingedsheep.engine.handlers.effects.permanent.protection

import com.wingedsheep.engine.core.ChooseColorThenContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChooseColorThenEffect] — generic "choose a color, then run X".
 *
 * Pauses for a color decision and pushes a [ChooseColorThenContinuation] carrying
 * the inner effect plus a snapshot of the current [EffectContext]. The resumer
 * dispatches the inner effect with `chosenColor` set, so atomic per-color grants
 * read the chosen color uniformly via `EffectContext`.
 */
class ChooseColorThenExecutor(
    private val decisionHandler: DecisionHandler
) : EffectExecutor<ChooseColorThenEffect> {

    override val effectType: KClass<ChooseColorThenEffect> = ChooseColorThenEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseColorThenEffect,
        context: EffectContext
    ): EffectResult {
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"

        val decisionResult = decisionHandler.createColorDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = effect.prompt,
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = ChooseColorThenContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            then = effect.then,
            baseContext = context
        )

        return EffectResult.paused(
            decisionResult.state.pushContinuation(continuation),
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }
}
