package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import kotlin.reflect.KClass

/**
 * Executor for SacrificeEffect.
 *
 * Handles "sacrifice a creature" / "sacrifice N permanents" effects where the
 * controller must choose which permanents to sacrifice.
 *
 * Examples:
 * - Accursed Centaur: "When ~ enters the battlefield, sacrifice a creature."
 * - Scapeshift: "Sacrifice any number of lands..."
 *
 * The sacrifice is mandatory (not optional) - the player must sacrifice if they
 * have valid permanents. If they don't have enough, nothing happens.
 */
class SacrificeExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<SacrificeEffect> {

    override val effectType: KClass<SacrificeEffect> = SacrificeEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: SacrificeEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val sourceId = context.sourceId

        val sourceName = if (sourceId != null) {
            state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
        } else {
            "Unknown"
        }

        // Find all valid permanents on the battlefield that the player controls
        val validPermanents = findValidPermanents(state, controllerId, effect)

        if (effect.any) {
            // "Sacrifice any number of..." - player chooses 0 to all
            return presentSacrificeDecision(
                state, controllerId, sourceId, sourceName,
                validPermanents, minSelections = 0, maxSelections = validPermanents.size
            )
        }

        // Standard sacrifice: must sacrifice exactly `count` permanents
        if (validPermanents.size < effect.count) {
            // Not enough valid permanents - effect does nothing (per MTG rules)
            return ExecutionResult.success(state)
        }

        if (validPermanents.size == effect.count) {
            // Exactly enough - auto-sacrifice without prompting
            return sacrificePermanents(state, controllerId, validPermanents)
        }

        // More than enough - player must choose which to sacrifice
        return presentSacrificeDecision(
            state, controllerId, sourceId, sourceName,
            validPermanents, minSelections = effect.count, maxSelections = effect.count
        )
    }

    private fun findValidPermanents(
        state: GameState,
        controllerId: EntityId,
        effect: SacrificeEffect
    ): List<EntityId> {
        // Use projected state to account for control-changing effects (e.g., Threaten)
        val projected = stateProjector.project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(controllerId)
        val context = PredicateContext(controllerId = controllerId)

        return controlledPermanents.filter { permanentId ->
            predicateEvaluator.matches(state, permanentId, effect.filter, context)
        }
    }

    private fun presentSacrificeDecision(
        state: GameState,
        controllerId: EntityId,
        sourceId: EntityId?,
        sourceName: String,
        validPermanents: List<EntityId>,
        minSelections: Int,
        maxSelections: Int
    ): ExecutionResult {
        val prompt = buildString {
            append("Choose ")
            when {
                minSelections == 0 && maxSelections > 0 -> append("any number of permanents")
                minSelections == 1 -> append("a permanent")
                else -> append("$minSelections permanents")
            }
            append(" to sacrifice")
        }

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
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
            playerId = controllerId,
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
        controllerId: EntityId,
        permanentIds: List<EntityId>
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (permanentId in permanentIds) {
            val container = newState.getEntity(permanentId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Find the actual zone the permanent is in (may differ from controller's zone for stolen creatures)
            val currentZone = newState.zones.entries.find { (_, cards) -> permanentId in cards }?.key
                ?: continue

            // Sacrificed cards go to their owner's graveyard, not the controller's
            val ownerId = container.get<OwnerComponent>()?.playerId
                ?: cardComponent.ownerId
                ?: controllerId
            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

            newState = newState.removeFromZone(currentZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)

            events.add(
                ZoneChangeEvent(
                    entityId = permanentId,
                    entityName = cardComponent.name,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.GRAVEYARD,
                    ownerId = ownerId
                )
            )
        }

        if (permanentIds.isNotEmpty()) {
            val permanentNames = permanentIds.map { id ->
                state.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            events.add(0, PermanentsSacrificedEvent(controllerId, permanentIds, permanentNames))
        }

        return ExecutionResult.success(newState, events)
    }
}
