package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Executor for [DrawCardsEffect] — "Draw X cards" or "Target player draws X cards".
 *
 * This class is a thin driver over [DrawLoop]; the actual mechanics of a
 * single-card draw live in [DrawCardPrimitive] and the replacement-effect
 * pipeline lives in [DrawReplacementDispatcher]. Both collaborators are shared
 * with [com.wingedsheep.engine.core.DrawPhaseManager] so the draw-step and
 * spell/ability paths go through exactly the same code.
 */
class DrawCardsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    cardRegistry: CardRegistry,
    effectExecutor: ((GameState, Effect, EffectContext) -> ExecutionResult)? = null
) : EffectExecutor<DrawCardsEffect> {

    override val effectType: KClass<DrawCardsEffect> = DrawCardsEffect::class

    private val primitive = DrawCardPrimitive(cardRegistry)
    private val dispatcher = DrawReplacementDispatcher(cardRegistry, effectExecutor)

    override fun execute(
        state: GameState,
        effect: DrawCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return ExecutionResult.error(state, "No valid player for draw")
        }

        val count = amountEvaluator.evaluate(state, effect.count, context)

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        for (playerId in playerIds) {
            val result = executeDraws(currentState, playerId, count)
            currentState = result.state
            allEvents.addAll(result.events)
            if (result.pendingDecision != null) {
                return ExecutionResult.paused(currentState, result.pendingDecision, allEvents)
            }
        }
        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Execute a sequence of [count] draws for [playerId] as a spell/ability
     * draw (i.e., not the draw-step draw).
     *
     * This is a public API used by several call sites beyond this executor:
     *  - [com.wingedsheep.engine.handlers.continuations.DrawReplacementContinuationResumer]
     *    when resuming a paused draw after a replacement decision,
     *  - [com.wingedsheep.engine.handlers.continuations.CoreAutoResumerModule]
     *    when auto-resuming `DrawReplacementRemainingDrawsContinuation` and
     *    `CycleDrawContinuation`,
     *  - [com.wingedsheep.engine.handlers.actions.ability.CycleCardHandler]
     *    for the draw that follows cycling,
     *  - [ReadTheRunesExecutor] for the initial "draw X cards" phase.
     *
     * @param skipPrompts historical flag meaning "this resumption has already
     *     handled decisions for the draw — skip both the static replacement
     *     and prompt-on-draw checks in the loop, but still honor the shield
     *     consumer". Setting this is how resume paths avoid re-asking the same
     *     question they just resumed from.
     */
    fun executeDraws(
        state: GameState,
        playerId: EntityId,
        count: Int,
        skipPrompts: Boolean = false
    ): ExecutionResult {
        return DrawLoop.run(
            state = state,
            playerId = playerId,
            count = count,
            primitive = primitive,
            dispatcher = dispatcher,
            isDrawStep = false,
            skipStaticReplacement = skipPrompts,
            skipPromptOnDraw = skipPrompts,
            emptyLibraryReason = "Empty library"
        )
    }

    /**
     * Expose the prompt-on-draw check so
     * [com.wingedsheep.engine.handlers.continuations.DrawReplacementContinuationResumer]
     * can re-ask after a player declines a prompt (to check for other
     * `promptOnDraw` abilities they might still activate). The shape of this
     * delegate mirrors the pre-refactor method signature so that the resumer
     * doesn't have to learn about the dispatcher abstraction.
     */
    internal fun checkPromptOnDraw(
        state: GameState,
        playerId: EntityId,
        remainingDrawCount: Int,
        drawnCardsSoFar: List<EntityId>,
        declinedSourceIds: List<EntityId> = emptyList()
    ): ExecutionResult? = dispatcher.checkPromptOnDraw(
        state = state,
        playerId = playerId,
        drawCount = remainingDrawCount,
        drawnCardsSoFar = drawnCardsSoFar,
        isDrawStep = false,
        declinedSourceIds = declinedSourceIds
    )
}
