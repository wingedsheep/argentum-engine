package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import kotlin.reflect.KClass

/**
 * Executor for ForceSacrificeEffect.
 *
 * Handles "target player sacrifices a creature" (edict effects) where an opponent
 * is forced to sacrifice permanents of their choice.
 *
 * Examples:
 * - Cabal Executioner: "Whenever ~ deals combat damage to a player, that player sacrifices a creature."
 * - Chainer's Edict: "Target player sacrifices a creature."
 */
class ForceSacrificeExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ForceSacrificeEffect> {

    override val effectType: KClass<ForceSacrificeEffect> = ForceSacrificeEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: ForceSacrificeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for force sacrifice")

        val sourceId = context.sourceId
        val sourceName = if (sourceId != null) {
            state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
        } else {
            "Unknown"
        }

        // Find valid permanents on the target player's battlefield
        val validPermanents = findValidPermanents(state, targetPlayerId, effect)

        if (validPermanents.isEmpty()) {
            // No valid permanents - effect does nothing
            return ExecutionResult.success(state)
        }

        if (validPermanents.size <= effect.count) {
            // Exactly enough or fewer - auto-sacrifice without prompting
            return sacrificePermanents(state, targetPlayerId, validPermanents)
        }

        // More than enough - target player must choose which to sacrifice
        return presentSacrificeDecision(
            state, targetPlayerId, sourceId, sourceName,
            validPermanents, minSelections = effect.count, maxSelections = effect.count
        )
    }

    private fun findValidPermanents(
        state: GameState,
        playerId: EntityId,
        effect: ForceSacrificeEffect
    ): List<EntityId> {
        // Use projected state to account for control-changing effects (e.g., Threaten).
        // A player sacrifices permanents they *control*, which may differ from what's in
        // their zone key when control-changing effects are in play.
        val projected = stateProjector.project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(playerId)
        val context = PredicateContext(controllerId = playerId)

        return controlledPermanents.filter { permanentId ->
            predicateEvaluator.matches(state, permanentId, effect.filter, context)
        }
    }

    private fun presentSacrificeDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        sourceName: String,
        validPermanents: List<EntityId>,
        minSelections: Int,
        maxSelections: Int
    ): ExecutionResult {
        val prompt = buildString {
            append("Choose ")
            if (minSelections == 1) append("a creature") else append("$minSelections creatures")
            append(" to sacrifice")
        }

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validPermanents,
            minSelections = minSelections,
            maxSelections = maxSelections,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true
        )

        val continuation = SacrificeContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = playerId,
            sourceId = sourceId,
            sourceName = sourceName
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    private fun sacrificePermanents(
        state: GameState,
        playerId: EntityId,
        permanentIds: List<EntityId>
    ): ExecutionResult {
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (permanentId in permanentIds) {
            if (permanentId !in newState.getZone(battlefieldZone)) continue

            val permanentName = newState.getEntity(permanentId)?.get<CardComponent>()?.name ?: "Unknown"
            newState = newState.removeFromZone(battlefieldZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)

            events.add(
                ZoneChangeEvent(
                    entityId = permanentId,
                    entityName = permanentName,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.GRAVEYARD,
                    ownerId = playerId
                )
            )
        }

        if (permanentIds.isNotEmpty()) {
            val permanentNames = permanentIds.map { id ->
                state.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            events.add(0, PermanentsSacrificedEvent(playerId, permanentIds, permanentNames))
        }

        return ExecutionResult.success(newState, events)
    }
}
