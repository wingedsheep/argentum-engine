package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId

class CombatContinuationResumer(
    private val ctx: ContinuationContext
) {

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

        return ctx.combatManager?.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
            ?: ExecutionResult.success(newState)
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

        val timestamp = originalShield?.timestamp ?: System.currentTimeMillis()
        for ((sourceId, preventionAmount) in response.distribution) {
            if (preventionAmount <= 0) continue
            updatedEffects.add(
                ActiveFloatingEffect(
                    id = EntityId(java.util.UUID.randomUUID().toString()),
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

        val newState = state.copy(floatingEffects = updatedEffects)

        return ctx.combatManager?.applyCombatDamage(newState, firstStrike = continuation.firstStrike)
            ?: ExecutionResult.success(newState)
    }

    fun resumeBlockerOrder(
        state: GameState,
        continuation: BlockerOrderContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OrderedResponse) {
            return ExecutionResult.error(state, "Expected ordered response for blocker ordering")
        }

        var newState = state.updateEntity(continuation.attackerId) { container ->
            container.with(
                com.wingedsheep.engine.state.components.combat.DamageAssignmentOrderComponent(
                    response.orderedObjects
                )
            )
        }

        val events = mutableListOf<GameEvent>(
            BlockerOrderDeclaredEvent(continuation.attackerId, response.orderedObjects)
        )

        if (continuation.remainingAttackers.isNotEmpty()) {
            val nextAttacker = continuation.remainingAttackers.first()
            val nextRemaining = continuation.remainingAttackers.drop(1)

            val attackerContainer = newState.getEntity(nextAttacker)!!
            val attackerCard = attackerContainer.get<CardComponent>()!!
            val blockedComponent = attackerContainer.get<com.wingedsheep.engine.state.components.combat.BlockedComponent>()!!
            val blockerIds = blockedComponent.blockerIds

            val cardInfo = blockerIds.associateWith { blockerId ->
                val blockerContainer = newState.getEntity(blockerId)
                val isFaceDown = blockerContainer?.has<FaceDownComponent>() == true
                if (isFaceDown) {
                    SearchCardInfo(
                        name = "Morph",
                        manaCost = "{3}",
                        typeLine = "Creature"
                    )
                } else {
                    val blockerCard = blockerContainer?.get<CardComponent>()
                    SearchCardInfo(
                        name = blockerCard?.name ?: "Unknown",
                        manaCost = blockerCard?.manaCost?.toString() ?: "",
                        typeLine = blockerCard?.typeLine?.toString() ?: ""
                    )
                }
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = OrderObjectsDecision(
                id = decisionId,
                playerId = continuation.attackingPlayerId,
                prompt = "Order damage assignment for ${attackerCard.name}",
                context = DecisionContext(
                    sourceId = nextAttacker,
                    sourceName = attackerCard.name,
                    phase = DecisionPhase.COMBAT
                ),
                objects = blockerIds,
                cardInfo = cardInfo
            )

            val nextContinuation = BlockerOrderContinuation(
                decisionId = decisionId,
                attackingPlayerId = continuation.attackingPlayerId,
                attackerId = nextAttacker,
                attackerName = attackerCard.name,
                remainingAttackers = nextRemaining
            )

            newState = newState
                .withPendingDecision(decision)
                .pushContinuation(nextContinuation)

            events.add(
                DecisionRequestedEvent(
                    decisionId = decision.id,
                    playerId = continuation.attackingPlayerId,
                    decisionType = "ORDER_BLOCKERS",
                    prompt = decision.prompt
                )
            )

            return ExecutionResult.paused(newState, decision, events)
        }

        val activePlayer = newState.activePlayerId
        val stateWithPriority = if (activePlayer != null) {
            newState.withPriority(activePlayer)
        } else {
            newState
        }
        return checkForMore(stateWithPriority, events)
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
                val result = EffectExecutorUtils.dealDamageToTarget(
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
}
