package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ForceExileMultiZoneEffect
import kotlin.reflect.KClass

/**
 * Executor for ForceExileMultiZoneEffect.
 *
 * Handles "for each 1 life you lost, exile a permanent you control or a card
 * from your hand or graveyard" (Lich's Mastery).
 *
 * Delegates zone movement to [ZoneTransitionService] for consistent cleanup
 * (now includes cleanupCombatReferences and removeFloatingEffectsTargeting
 * which were previously missing for battlefield permanents).
 */
class ForceExileMultiZoneExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ForceExileMultiZoneEffect> {

    override val effectType: KClass<ForceExileMultiZoneEffect> = ForceExileMultiZoneEffect::class

    private val amountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: ForceExileMultiZoneEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = context.resolvePlayerTarget(effect.target)
            ?: return ExecutionResult.error(state, "No valid player for force exile multi-zone")

        val count = amountEvaluator.evaluate(state, effect.count, context)
        if (count <= 0) {
            return ExecutionResult.success(state)
        }

        val sourceId = context.sourceId
        val sourceName = if (sourceId != null) {
            state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
        } else {
            "Unknown"
        }

        // Gather all valid options from the three zones
        val allOptions = gatherOptions(state, playerId)

        if (allOptions.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val exileCount = minOf(count, allOptions.size)

        if (allOptions.size <= exileCount) {
            // Auto-exile everything — no choice needed
            return exileEntities(state, playerId, allOptions)
        }

        // Player must choose which to exile
        val prompt = "Choose $exileCount permanent(s) or card(s) to exile"

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = allOptions,
            minSelections = exileCount,
            maxSelections = exileCount,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true
        )

        val continuation = ExileMultiZoneContinuation(
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

    companion object {
        /**
         * Gather all valid exile options from battlefield, hand, and graveyard.
         */
        fun gatherOptions(state: GameState, playerId: EntityId): List<EntityId> {
            val options = mutableListOf<EntityId>()

            // Permanents controlled by the player on the battlefield
            val projected = state.projectedState
            val controlledPermanents = projected.getBattlefieldControlledBy(playerId)
            options.addAll(controlledPermanents)

            // Cards in hand
            val handZone = ZoneKey(playerId, Zone.HAND)
            options.addAll(state.getZone(handZone))

            // Cards in graveyard
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            options.addAll(state.getZone(graveyardZone))

            return options
        }

        /**
         * Exile a list of entities from wherever they currently are.
         * Delegates to [ZoneTransitionService] for consistent cleanup.
         */
        fun exileEntities(
            state: GameState,
            playerId: EntityId,
            entityIds: List<EntityId>
        ): ExecutionResult {
            var currentState = state
            val allEvents = mutableListOf<GameEvent>()

            for (entityId in entityIds) {
                val transitionResult = ZoneTransitionService.moveToZone(
                    currentState, entityId, Zone.EXILE,
                    ZoneEntryOptions(skipZoneChangeRedirect = true)
                )
                currentState = transitionResult.state
                allEvents.addAll(transitionResult.events)
            }

            return ExecutionResult.success(currentState, allEvents)
        }
    }
}
