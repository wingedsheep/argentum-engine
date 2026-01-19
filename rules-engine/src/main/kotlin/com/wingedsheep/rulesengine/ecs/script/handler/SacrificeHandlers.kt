package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.SacrificeUnlessEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.decision.CardOption
import com.wingedsheep.rulesengine.decision.SacrificeUnlessDecision
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.EffectContinuation
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlin.reflect.KClass

/**
 * Handler for SacrificeUnlessEffect.
 *
 * Implements the "sacrifice X unless you sacrifice Y" pattern.
 * Examples:
 * - Primeval Force: "sacrifice it unless you sacrifice three Forests"
 *
 * Flow:
 * 1. Find permanents that can pay the cost
 * 2. If not enough permanents exist, automatically sacrifice the source
 * 3. If enough exist, present choice to player via pending decision
 * 4. Execute player's choice through continuation
 */
class SacrificeUnlessHandler : BaseEffectHandler<SacrificeUnlessEffect>() {
    override val effectClass: KClass<SacrificeUnlessEffect> = SacrificeUnlessEffect::class

    override fun execute(
        state: GameState,
        effect: SacrificeUnlessEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val sourceId = context.sourceId

        // Resolve which permanent to sacrifice if cost not paid
        val permanentToSacrifice = when (effect.permanentToSacrifice) {
            is EffectTarget.Self -> sourceId
            else -> sourceId // Default to source for now
        }

        // Get permanent name for display
        val permanentName = state.getEntity(permanentToSacrifice)
            ?.get<CardComponent>()?.definition?.name ?: "permanent"

        // Find valid permanents to sacrifice as cost payment
        val validCostTargets = findValidCostTargets(state, playerId, effect)

        // If player can't pay the cost, automatically sacrifice the permanent
        if (validCostTargets.size < effect.cost.count) {
            return sacrificePermanent(state, permanentToSacrifice, permanentName, playerId)
        }

        // Player has a choice - create pending decision
        return createPendingDecision(
            state = state,
            playerId = playerId,
            permanentToSacrifice = permanentToSacrifice,
            permanentName = permanentName,
            validCostTargets = validCostTargets,
            effect = effect,
            context = context
        )
    }

    /**
     * Find permanents on the battlefield controlled by the player that match the cost filter.
     */
    private fun findValidCostTargets(
        state: GameState,
        playerId: EntityId,
        effect: SacrificeUnlessEffect
    ): List<EntityId> {
        return state.getBattlefield().filter { entityId ->
            val entity = state.getEntity(entityId) ?: return@filter false
            val controller = entity.get<ControllerComponent>()
            val cardComponent = entity.get<CardComponent>()

            // Must be controlled by the player
            controller?.controllerId == playerId &&
            // Must match the filter
            cardComponent != null &&
            SearchLibraryHandler.matchesFilter(cardComponent.definition, effect.cost.filter)
        }
    }

    /**
     * Create a pending decision for the player to choose whether to pay the cost.
     */
    private fun createPendingDecision(
        state: GameState,
        playerId: EntityId,
        permanentToSacrifice: EntityId,
        permanentName: String,
        validCostTargets: List<EntityId>,
        effect: SacrificeUnlessEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val cardOptions = validCostTargets.map { entityId ->
            val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
            val def = cardComponent?.definition
            CardOption(
                entityId = entityId,
                name = def?.name ?: "Unknown",
                typeLine = def?.typeLine?.toString(),
                manaCost = def?.manaCost?.toString()
            )
        }

        val decision = SacrificeUnlessDecision.create(
            playerId = playerId,
            description = "Sacrifice $permanentName or ${effect.cost.description}?",
            permanentToSacrifice = permanentToSacrifice,
            permanentName = permanentName,
            costDescription = effect.cost.description,
            validCostTargets = cardOptions,
            requiredCount = effect.cost.count,
            sourceEntityId = context.sourceId
        )

        // Continuation to handle player's choice
        val continuation = EffectContinuation { selectedIds ->
            if (selectedIds.isEmpty()) {
                // Player chose not to pay - sacrifice the permanent
                sacrificePermanent(state, permanentToSacrifice, permanentName, playerId)
            } else {
                // Player chose to pay the cost - sacrifice the selected permanents
                sacrificeForCost(state, selectedIds, permanentName, playerId)
            }
        }

        return ExecutionResult(
            state = state,
            events = emptyList(),
            pendingDecision = decision,
            continuation = continuation
        )
    }

    /**
     * Sacrifice a permanent (the source that wasn't paid for).
     */
    private fun sacrificePermanent(
        state: GameState,
        permanentId: EntityId,
        permanentName: String,
        playerId: EntityId
    ): ExecutionResult {
        // Move from battlefield to graveyard
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, playerId)
        val newState = state
            .removeFromZone(permanentId, ZoneId.BATTLEFIELD)
            .addToZone(permanentId, graveyardZone)

        return ExecutionResult(
            state = newState,
            events = listOf(
                EffectEvent.PermanentSacrificed(permanentId, permanentName, playerId)
            )
        )
    }

    /**
     * Sacrifice permanents to pay the cost.
     */
    private fun sacrificeForCost(
        state: GameState,
        permanentIds: List<EntityId>,
        costDescription: String,
        playerId: EntityId
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EffectEvent>()
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, playerId)

        for (permanentId in permanentIds) {
            val permanentName = currentState.getEntity(permanentId)
                ?.get<CardComponent>()?.definition?.name ?: "permanent"

            currentState = currentState
                .removeFromZone(permanentId, ZoneId.BATTLEFIELD)
                .addToZone(permanentId, graveyardZone)

            events.add(EffectEvent.PermanentSacrificed(permanentId, permanentName, playerId))
        }

        return ExecutionResult(
            state = currentState,
            events = events
        )
    }
}
