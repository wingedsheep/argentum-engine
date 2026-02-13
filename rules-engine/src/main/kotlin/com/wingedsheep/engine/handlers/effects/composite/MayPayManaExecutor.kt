package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Effect
import com.wingedsheep.sdk.scripting.MayPayManaEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for MayPayManaEffect.
 * Handles "You may pay {cost}. If you do, [effect]." by checking mana availability,
 * presenting a yes/no choice, and auto-tapping lands if the player agrees.
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class MayPayManaExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<MayPayManaEffect> {

    override val effectType: KClass<MayPayManaEffect> = MayPayManaEffect::class

    override fun execute(
        state: GameState,
        effect: MayPayManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        // Check if the player can pay the mana cost
        val manaSolver = ManaSolver()
        if (!manaSolver.canPay(state, playerId, effect.cost)) {
            // Can't pay — skip silently
            return ExecutionResult.success(state)
        }

        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Create yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay ${effect.cost}?",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Pay ${effect.cost}",
            noText = "Don't pay"
        )

        // Create continuation to resume after player's choice
        val continuation = MayPayManaContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            manaCost = effect.cost,
            effect = effect.effect,
            controllerId = context.controllerId,
            opponentId = context.opponentId,
            xValue = context.xValue,
            targets = context.targets,
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
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }
}
