package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ModalEffect.
 * Handles "Choose one —" / "Choose two —" modal spells by presenting a mode selection.
 *
 * Flow:
 * 1. Present mode options to the player (ChooseOptionDecision)
 * 2. Push ModalContinuation with mode data
 * 3. ContinuationHandler handles the response: executes chosen mode's effect,
 *    pausing for target selection if needed.
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class ModalEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<ModalEffect> {

    override val effectType: KClass<ModalEffect> = ModalEffect::class

    override fun execute(
        state: GameState,
        effect: ModalEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Build mode descriptions for the decision
        val modeDescriptions = effect.modes.map { it.description }

        // Create option decision for mode selection
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Choose a mode for ${sourceName ?: "modal spell"}",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = modeDescriptions
        )

        // Create continuation to resume after player's choice
        val continuation = ModalContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            modes = effect.modes,
            xValue = context.xValue,
            opponentId = context.opponentId,
            triggeringEntityId = context.triggeringEntityId
        )

        // Push continuation and return paused state
        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }
}
