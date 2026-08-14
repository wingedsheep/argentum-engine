package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.DrawPhaseManager
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.ability.CycleCardHandler
import com.wingedsheep.engine.handlers.continuations.CoreAutoResumerModule
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
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
 * with [DrawPhaseManager] so the draw-step and
 * spell/ability paths go through exactly the same code.
 */
class DrawCardsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    cardRegistry: CardRegistry,
    effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)? = null,
    replacementProcessor: ReplacementEffectProcessor = ReplacementEffectProcessor()
) : EffectExecutor<DrawCardsEffect> {

    override val effectType: KClass<DrawCardsEffect> = DrawCardsEffect::class

    private val primitive = DrawCardPrimitive(cardRegistry)
    private val dispatcher = DrawReplacementDispatcher(effectExecutor, replacementProcessor)

    override fun execute(
        state: GameState,
        effect: DrawCardsEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "No valid player for draw")
        }

        val count = amountEvaluator.evaluate(state, effect.count, context)

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        for (playerId in playerIds) {
            val result = executeDraws(currentState, playerId, count, context = context)
            currentState = result.state
            allEvents.addAll(result.events)
            if (result.pendingDecision != null) {
                return EffectResult.paused(currentState, result.pendingDecision, allEvents)
            }
        }
        return EffectResult.success(currentState, allEvents)
    }

    /**
     * Execute a sequence of [count] draws for [playerId].
     *
     * This is a public API used by several call sites beyond this executor:
     *  - [DrawPhaseManager] when performing the draw-step draw,
     *  - [CoreAutoResumerModule] when auto-resuming `DrawReplacementRemainingDrawsContinuation`
     *    and `CycleDrawContinuation`,
     *  - [CycleCardHandler] for the draw that follows cycling.
     *
     * @param isDrawStep `true` when this is the active player's draw-step
     *     draw (sets `isDrawStep` on the shield consumer and static replacement
     *     checks), `false` for spell/ability draws.
     * @param emptyLibraryReason message on draw failure when library is empty.
     *     Draw-step callers pass `"Library is empty"`; spell/ability callers
     *     pass `"Empty library"`.
     * @param announce whether to run the CR 121.2a announcement check. Pass `false`
     *     when [count] is the tail of a draw instruction that was already announced —
     *     the resumers for a paused draw do, so `ModifyDrawAmount` isn't applied twice
     *     to the same instruction.
     */
    fun executeDraws(
        state: GameState,
        playerId: EntityId,
        count: Int,
        isDrawStep: Boolean = false,
        emptyLibraryReason: String = "Empty library",
        context: EffectContext? = null,
        announce: Boolean = true
    ): EffectResult {
        // Pre-loop announcement check (CR 121.2a): static replacement effects like
        // ModifyDrawAmount (e.g., Quantum Riddler's "draw that many cards plus one")
        // fire here against the total draw count before any individual card is drawn.
        var adjustedCount = count
        var currentState = state
        val announceResult = if (announce) {
            dispatcher.checkDrawAmount(currentState, playerId, count, isDrawStep, context)
        } else {
            null
        }
        when (announceResult) {
            is DrawReplacementDispatcher.DispatchResult.Modified -> {
                // Keep the chain the processor stamped: CR 614.5 covers "an event or any
                // modified events that may replace that event", so a sub-draw spawned while
                // resolving this instruction must not re-apply the same announcement effect.
                // DrawLoop clears it once the instruction finishes.
                currentState = announceResult.state
                adjustedCount = count + announceResult.delta
            }
            is DrawReplacementDispatcher.DispatchResult.Replaced -> {
                return EffectResult.success(announceResult.state, announceResult.events)
            }
            is DrawReplacementDispatcher.DispatchResult.Paused -> {
                return EffectResult.paused(
                    announceResult.result.state,
                    announceResult.result.pendingDecision!!,
                    announceResult.result.events
                )
            }
            is DrawReplacementDispatcher.DispatchResult.None -> { /* no replacement */ }
            null -> { /* announcement skipped, or no announcement replacement matched */ }
        }

        return DrawLoop.run(
            state = currentState,
            playerId = playerId,
            count = adjustedCount,
            primitive = primitive,
            dispatcher = dispatcher,
            isDrawStep = isDrawStep,
            emptyLibraryReason = emptyLibraryReason,
            context = context
        )
    }

}
