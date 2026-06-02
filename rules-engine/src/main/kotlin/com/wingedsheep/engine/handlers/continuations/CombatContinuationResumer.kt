package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId

class CombatContinuationResumer(
    private val services: com.wingedsheep.engine.core.EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(DamageAssignmentContinuation::class) { state, continuation, response, _ ->
            resumeDamageAssignment(state, continuation, response)
        },
        resumer(CombatResolutionContinuation::class) { state, continuation, response, _ ->
            resumeCombatResolution(state, continuation, response)
        },
        resumer(AssignAsUnblockedContinuation::class) { state, continuation, response, _ ->
            resumeAssignAsUnblocked(state, continuation, response)
        },
        resumer(DamagePreventionContinuation::class, ::resumeDamagePrevention),
        resumer(DistributeDamageContinuation::class, ::resumeDistributeDamage),
        resumer(DeflectDamageSourceChoiceContinuation::class, ::resumeDeflectDamageSourceChoice),
        resumer(PreventDamageFromChosenSourceContinuation::class, ::resumePreventDamageFromChosenSource)
    )

    fun resumeDamageAssignment(
        state: GameState,
        continuation: DamageAssignmentContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        val assignments = when (response) {
            is DistributionResponse -> response.distribution
            is DamageAssignmentResponse -> response.assignments
            else -> return ExecutionResult.error(state, "Expected distribution or damage assignment response")
        }

        val newState = state.updateEntity(continuation.attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(
                    assignments
                )
            )
        }

        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    fun resumeAssignAsUnblocked(
        state: GameState,
        continuation: AssignAsUnblockedContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for assign-as-unblocked decision")
        }

        val newState = if (response.choice) {
            // Player chose to assign damage to the defending player — store a manual assignment
            val projected = state.projectedState
            val power = projected.getPower(continuation.attackerId) ?: 0
            state.updateEntity(continuation.attackerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(
                        mapOf(continuation.defendingPlayerId to power)
                    )
                )
            }
        } else {
            // Player chose to assign to blockers normally — mark with empty assignment
            // so the pre-check doesn't re-ask; proposeDamageAssignments will auto-distribute
            state.updateEntity(continuation.attackerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(emptyMap())
                )
            }
        }

        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    /**
     * Apply a [CombatResolutionResponse] (the combat-damage board).
     *
     * The current chooser is `continuation.pendingChoosers.first()`. We honor only the edges they
     * own (filtered by [DamageEdge.editableBy] on the cached `decisionShape`), bake those amounts
     * on top of the shape's current amounts, and:
     *
     * - if more choosers remain (CR 510.1c sequencing, or the CR 702.22j/k two-actor banding case),
     *   re-pause via [com.wingedsheep.engine.mechanics.combat.CombatManager.repauseCombatResolution]
     *   for the next chooser with the locked-in amounts shown;
     * - otherwise fold every edge into a per-source [DamageAssignmentComponent] (read straight off
     *   the cached edge objects — no edge-id parsing), apply any row-order overrides, and re-enter
     *   `applyCombatDamage` to run the damage pipeline.
     */
    fun resumeCombatResolution(
        state: GameState,
        continuation: CombatResolutionContinuation,
        response: DecisionResponse,
    ): ExecutionResult {
        if (response !is CombatResolutionResponse) {
            return ExecutionResult.error(state, "Expected combat resolution response for combat resolution decision")
        }

        val shape = continuation.decisionShape
        val submittingPlayer = continuation.pendingChoosers.firstOrNull()
        val remainingChoosers = continuation.pendingChoosers.drop(1)

        val edgeById = shape.edges.associateBy { it.id }
        val submittedByEdge = response.edges.associate { it.edgeId to it.amount }
        // Keep only edges this chooser owns; unknown ids and other-owner edges are dropped.
        val honoredByEdge = submittedByEdge.filter { (edgeId, _) ->
            submittingPlayer != null && edgeById[edgeId]?.editableBy == submittingPlayer
        }
        // Bake onto the shape's current amounts so the next chooser (if any) sees locked-in values.
        val accumulatedAmounts: Map<String, Int> = shape.edges.associate { it.id to it.amount } + honoredByEdge

        if (remainingChoosers.isNotEmpty()) {
            return services.combatManager.repauseCombatResolution(
                state = state,
                previous = shape,
                remainingChoosers = remainingChoosers,
                latestAmounts = accumulatedAmounts,
                firstStrike = continuation.firstStrike,
            )
        }

        // All choosers confirmed — write per-source DamageAssignmentComponents from the edge objects.
        val assignmentsBySource = mutableMapOf<EntityId, MutableMap<EntityId, Int>>()
        for ((edgeId, amount) in accumulatedAmounts) {
            val edge = edgeById[edgeId] ?: continue
            assignmentsBySource.getOrPut(edge.sourceId) { mutableMapOf() }[edge.targetId] = amount
        }

        var newState = state
        for ((sourceId, assignments) in assignmentsBySource) {
            newState = newState.updateEntity(sourceId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent(assignments)
                )
            }
        }
        for ((attackerId, order) in response.orderedBlockers) {
            newState = newState.updateEntity(attackerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent(order)
                )
            }
        }
        for ((blockerId, order) in response.orderedAttackers) {
            newState = newState.updateEntity(blockerId) { container ->
                container.with(
                    com.wingedsheep.engine.state.components.combat.AttackerOrderComponent(order)
                )
            }
        }

        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    fun resumeDamagePrevention(
        state: GameState,
        continuation: DamagePreventionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for damage prevention")
        }

        val updatedEffects = state.floatingEffects.toMutableList()
        val shieldIndex = updatedEffects.indexOfFirst { it.id == continuation.shieldEffectId }
        val originalShield = if (shieldIndex >= 0) updatedEffects.removeAt(shieldIndex) else null

        val timestamp = originalShield?.timestamp ?: state.timestamp
        var workingState = state
        for ((sourceId, preventionAmount) in response.distribution) {
            if (preventionAmount <= 0) continue
            val (effectId, advanced) = workingState.newEntity()
            workingState = advanced
            updatedEffects.add(
                ActiveFloatingEffect(
                    id = effectId,
                    effect = FloatingEffectData(
                        layer = Layer.ABILITY,
                        modification = SerializableModification.PreventNextDamage(preventionAmount, onlyFromSource = sourceId),
                        affectedEntities = setOf(continuation.recipientId)
                    ),
                    duration = originalShield?.duration ?: com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
                    sourceId = originalShield?.sourceId,
                    sourceName = originalShield?.sourceName,
                    controllerId = originalShield?.controllerId ?: continuation.recipientId,
                    timestamp = timestamp
                )
            )
        }

        val newState = workingState.copy(floatingEffects = updatedEffects)

        return services.combatManager.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
    }

    fun resumeDistributeDamage(
        state: GameState,
        continuation: DistributeDamageContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is DistributionResponse) {
            return ExecutionResult.error(state, "Expected distribution response for divided damage")
        }

        val distribution = response.distribution
        val events = mutableListOf<GameEvent>()
        var newState = state

        for ((targetId, damageAmount) in distribution) {
            if (damageAmount > 0) {
                val result = DamageUtils.dealDamageToTarget(
                    newState,
                    targetId,
                    damageAmount,
                    continuation.sourceId
                )

                if (!result.isSuccess) {
                    return ExecutionResult(newState, events, result.error)
                }

                newState = result.state
                events.addAll(result.events)
            }
        }

        return checkForMore(newState, events)
    }

    fun resumeDeflectDamageSourceChoice(
        state: GameState,
        continuation: DeflectDamageSourceChoiceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected cards selected response for source selection")
        }

        val chosenSourceId = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No source selected")

        // The spell that set up the shield is the source of the follow-up's reflected damage.
        val (effectiveState, reactionSourceId) = if (continuation.sourceId != null) {
            state to continuation.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }

        // Two linked objects, per CR: (1) the one-shot prevention shield, and (2) a delayed
        // triggered ability "When damage is prevented this way, …" that goes on the stack when the
        // shield fires. They are linked by the delayed trigger's id, carried on the shield and
        // echoed back by the DamagePreventedEvent so only this shield's trigger fires.
        val sourceName = continuation.sourceName
            ?: state.getEntity(reactionSourceId)?.get<CardComponent>()?.name
            ?: "Source"
        val delayedTriggerId = java.util.UUID.randomUUID().toString()

        var newState = effectiveState
        continuation.onPrevented?.let { onPrevented ->
            newState = newState.addDelayedTrigger(
                com.wingedsheep.engine.event.DelayedTriggeredAbility(
                    id = delayedTriggerId,
                    effect = onPrevented,
                    sourceId = reactionSourceId,
                    sourceName = sourceName,
                    controllerId = continuation.controllerId,
                    trigger = com.wingedsheep.sdk.scripting.TriggerSpec(
                        event = com.wingedsheep.sdk.scripting.EventPattern.DamagePreventedEvent
                    ),
                    // Scopes the fired trigger's context to the prevented source (so
                    // ControllerOfTriggeringEntity = "that source's controller").
                    watchedEntityId = chosenSourceId,
                    expiry = com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry.EndOfTurn
                )
            )
        }

        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            opponentId = null
        )
        newState = newState.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.PreventNextDamageFromChosenSourceShield(
                damageSourceId = chosenSourceId,
                linkId = delayedTriggerId
            ),
            affectedEntities = setOf(continuation.controllerId),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            context = context
        )

        return checkForMore(newState, emptyList())
    }

    fun resumePreventDamageFromChosenSource(
        state: GameState,
        continuation: PreventDamageFromChosenSourceContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected cards selected response for source selection")
        }

        val chosenSourceId = response.selectedCards.firstOrNull()
            ?: return ExecutionResult.error(state, "No source selected")

        val context = EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.controllerId,
            opponentId = null
        )
        val modification = if (continuation.amount == null) {
            // Prevent all damage from the chosen source for the rest of the turn (Samite Ministration)
            SerializableModification.PreventAllDamageFromSource(
                damageSourceId = chosenSourceId,
                gainLifeFromColors = continuation.gainLifeFromColors
            )
        } else {
            SerializableModification.PreventNextDamage(
                remainingAmount = continuation.amount,
                onlyFromSource = chosenSourceId
            )
        }
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = modification,
            affectedEntities = setOf(continuation.targetId),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            context = context
        )

        return checkForMore(newState, emptyList())
    }
}
