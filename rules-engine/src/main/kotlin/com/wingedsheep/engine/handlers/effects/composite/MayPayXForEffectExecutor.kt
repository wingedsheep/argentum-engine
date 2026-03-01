package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MayPayXForEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for MayPayXForEffect.
 * Handles "You may pay {X}. If you do, [effect]." by presenting a number chooser
 * (0 to max affordable mana), paying the chosen amount, and executing the effect
 * with the chosen X value.
 */
class MayPayXForEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<MayPayXForEffect> {

    override val effectType: KClass<MayPayXForEffect> = MayPayXForEffect::class

    override fun execute(
        state: GameState,
        effect: MayPayXForEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        // Calculate max affordable X
        val manaSolver = ManaSolver()
        val maxAffordable = manaSolver.getAvailableManaCount(state, playerId)

        if (maxAffordable <= 0) {
            // Can't pay anything — skip silently
            return ExecutionResult.success(state)
        }

        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Create number chooser decision (0 to max, where 0 means decline)
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseNumberDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay {X}? Choose X (0 to decline)",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            minValue = 0,
            maxValue = maxAffordable
        )

        val continuation = MayPayXContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            effect = effect.effect,
            controllerId = context.controllerId,
            opponentId = context.opponentId,
            targets = context.targets,
            triggeringEntityId = context.triggeringEntityId,
            namedTargets = context.namedTargets,
            maxX = maxAffordable
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "CHOOSE_NUMBER",
                    prompt = decision.prompt
                )
            )
        )
    }
}
