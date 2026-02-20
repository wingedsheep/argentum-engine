package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.SelectTargetEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for SelectTargetEffect — mid-resolution pipeline targeting.
 *
 * Finds legal targets using [TargetFinder], then:
 * - **No legal targets** → stores empty collection, pipeline continues
 * - **Single legal target (non-optional)** → auto-selects, stores in [updatedCollections]
 * - **Multiple legal targets** → creates [ChooseTargetsDecision], pushes
 *   [SelectTargetPipelineContinuation], returns paused
 */
class SelectTargetPipelineExecutor(
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<SelectTargetEffect> {

    override val effectType: KClass<SelectTargetEffect> = SelectTargetEffect::class

    override fun execute(
        state: GameState,
        effect: SelectTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val sourceId = context.sourceId

        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = effect.requirement,
            controllerId = controllerId,
            sourceId = sourceId
        )

        if (legalTargets.isEmpty()) {
            // No legal targets — store empty collection, pipeline continues gracefully
            return ExecutionResult.success(state).copy(
                updatedCollections = mapOf(effect.storeAs to emptyList())
            )
        }

        if (legalTargets.size == 1) {
            // Single legal target — auto-select
            return ExecutionResult.success(state).copy(
                updatedCollections = mapOf(effect.storeAs to legalTargets)
            )
        }

        // Multiple legal targets — pause for player decision
        return createDecision(state, context, effect, legalTargets)
    }

    private fun createDecision(
        state: GameState,
        context: EffectContext,
        effect: SelectTargetEffect,
        legalTargets: List<EntityId>
    ): ExecutionResult {
        val decisionId = UUID.randomUUID().toString()
        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val requirementInfo = TargetRequirementInfo(
            index = 0,
            description = effect.requirement.description,
            minTargets = 1,
            maxTargets = 1
        )

        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = effect.description,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            targetRequirements = listOf(requirementInfo),
            legalTargets = mapOf(0 to legalTargets)
        )

        val continuation = SelectTargetPipelineContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            storeAs = effect.storeAs,
            storedCollections = context.storedCollections
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "CHOOSE_TARGETS",
                    prompt = decision.prompt
                )
            )
        )
    }
}
