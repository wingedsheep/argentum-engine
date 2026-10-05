package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameRestartRequest
import com.wingedsheep.engine.core.TurnStartFollowUp
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.RestartGameEffect
import kotlin.reflect.KClass

/**
 * Executor for [RestartGameEffect] (CR 727, "restart the game").
 *
 * The restart replaces the whole game, so it can't happen in the middle of a resolution whose
 * stack object, resolution bookkeeping and priority hand-off are still to be wrapped up. Like
 * "end the turn" ([EndTheTurnExecutor]) this executor only records the request on the state, and
 * the settle boundary ([com.wingedsheep.engine.core.Settler]) carries it out through
 * [com.wingedsheep.engine.core.GameRestarter] once the resolution finishes.
 *
 * The exempted cards are read from the pipeline now. The ability's remaining instructions run later
 * in a fresh context of their own (CR 727.4): the old game's targets and choices mean nothing in the
 * new one, so only the controller, the source and the exempt collection are kept.
 */
class RestartGameExecutor : EffectExecutor<RestartGameEffect> {

    override val effectType: KClass<RestartGameEffect> = RestartGameEffect::class

    override fun execute(
        state: GameState,
        effect: RestartGameEffect,
        context: EffectContext
    ): EffectResult {
        val exempt = effect.exempt?.let { context.pipeline.storedCollections[it] }.orEmpty()
        val followUp = effect.afterRestart?.let { instructions ->
            TurnStartFollowUp(
                effect = instructions,
                context = EffectContext(
                    sourceId = context.sourceId,
                    controllerId = context.controllerId,
                    pipeline = PipelineState(
                        storedCollections = effect.exempt?.let { mapOf(it to exempt) }.orEmpty()
                    )
                )
            )
        }
        return EffectResult.success(
            state.copy(pendingRestart = GameRestartRequest(context.controllerId, exempt, followUp))
        )
    }
}
