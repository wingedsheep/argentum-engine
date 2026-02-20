package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for MayEffect.
 * Handles "You may [do something]" effects by presenting a yes/no choice.
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class MayEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<MayEffect> {

    override val effectType: KClass<MayEffect> = MayEffect::class

    override fun execute(
        state: GameState,
        effect: MayEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Create yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = effect.description,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION,
                triggeringEntityId = context.triggeringEntityId
            ),
            yesText = "Yes",
            noText = "No"
        )

        // Create continuation to resume after player's choice
        val continuation = MayAbilityContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            effectIfYes = effect.effect,
            effectIfNo = null,
            controllerId = context.controllerId,
            opponentId = context.opponentId,
            xValue = context.xValue,
            targets = context.targets,
            triggeringEntityId = context.triggeringEntityId,
            triggerDamageAmount = context.triggerDamageAmount
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
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }
}
