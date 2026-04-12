package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.BudgetModalEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for BudgetModalEffect.
 *
 * Handles "Choose up to N {P} worth of modes" spells (Bloomburrow Season cycle).
 * Presents a BudgetModalDecision with budget and mode info. The player selects
 * modes (with repeats) in a single overlay, then the chosen modes execute
 * in printed order.
 */
class BudgetModalEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<BudgetModalEffect> {

    override val effectType: KClass<BudgetModalEffect> = BudgetModalEffect::class

    override fun execute(
        state: GameState,
        effect: BudgetModalEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.controllerId
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = BudgetModalDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Choose modes for ${sourceName ?: "budget modal spell"}",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            budget = effect.budget,
            modes = effect.modes.map { mode ->
                BudgetModeOption(cost = mode.cost, description = mode.description)
            }
        )

        val continuation = BudgetModalContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            modes = effect.modes,
            remainingBudget = effect.budget,
            selectedModeIndices = emptyList(),
            opponentId = context.opponentId
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "BUDGET_MODAL",
                    prompt = decision.prompt
                )
            )
        )
    }
}
