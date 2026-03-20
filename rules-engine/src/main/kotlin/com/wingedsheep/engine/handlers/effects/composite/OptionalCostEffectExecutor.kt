package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for OptionalCostEffect.
 * Handles "You may [cost]. If you do, [ifPaid]. Otherwise, [ifNotPaid]." effects.
 *
 * Presents a yes/no choice. If yes, executes [cost] then [ifPaid].
 * If no, executes [ifNotPaid] (if present).
 *
 * Used for Gift mechanics and similar optional cost patterns.
 */
class OptionalCostEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<OptionalCostEffect> {

    override val effectType: KClass<OptionalCostEffect> = OptionalCostEffect::class

    override fun execute(
        state: GameState,
        effect: OptionalCostEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.controllerId

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

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

        // If yes: execute cost then ifPaid
        val effectIfYes = CompositeEffect(listOf(effect.cost, effect.ifPaid))

        val continuation = MayAbilityContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceName = sourceName,
            effectIfYes = effectIfYes,
            effectIfNo = effect.ifNotPaid,
            effectContext = context
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
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }
}
