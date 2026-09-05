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
 * - **Multiple legal targets, or a single one the player may decline** → creates
 *   [ChooseTargetsDecision], pushes [SelectTargetPipelineContinuation], returns paused
 *
 * The requirement is single-target by construction — [createDecision] offers one slot — so a
 * requirement asking for more is rejected up front rather than turned into a decision no response
 * can satisfy.
 */
class SelectTargetPipelineExecutor(
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<SelectTargetEffect> {

    override val effectType: KClass<SelectTargetEffect> = SelectTargetEffect::class

    override fun execute(
        state: GameState,
        effect: SelectTargetEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val sourceId = context.sourceId

        val legalTargets = targetFinder.findLegalTargets(
            state = state,
            requirement = effect.requirement,
            controllerId = controllerId,
            sourceId = sourceId,
            // Carry the resolving ability's granter so a target filter can exclude it via
            // StatePredicate.IsGrantingPermanent — e.g. Dire Blunderbuss's "an artifact other than
            // Dire Blunderbuss" (CR 201.5a). Only granterId is threaded; other context fields keep
            // their prior (null) defaults so no existing SelectTargetEffect changes behavior.
            pipelineContext = com.wingedsheep.engine.handlers.PredicateContext(
                controllerId = controllerId,
                granterId = context.granterId
            )
        )

        if (legalTargets.isEmpty()) {
            // No legal targets — store empty collection, pipeline continues gracefully
            return EffectResult.success(state).copy(
                updatedCollections = mapOf(effect.storeAs to emptyList())
            )
        }

        if (legalTargets.size == 1 && effect.requirement.requiresExactlyOneTarget) {
            // Single mandatory legal target — auto-select
            return EffectResult.success(state).copy(
                updatedCollections = mapOf(effect.storeAs to legalTargets)
            )
        }

        // Multiple legal targets or an optional singleton — pause for player decision
        return createDecision(state, context, effect, legalTargets)
    }

    private fun createDecision(
        state: GameState,
        context: EffectContext,
        effect: SelectTargetEffect,
        legalTargets: List<EntityId>
    ): EffectResult {
        val decisionId = UUID.randomUUID().toString()
        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        require(effect.requirement.count == 1) {
            "SelectTargetEffect offers one target slot, but ${effect.requirement.description} asks " +
                "for ${effect.requirement.count}"
        }
        val requirementInfo = TargetRequirementInfo(
            index = 0,
            description = effect.requirement.description,
            minTargets = effect.requirement.effectiveMinCount,
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
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            storeAs = effect.storeAs,
            storedCollections = context.pipeline.storedCollections
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
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
