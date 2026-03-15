package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.cleanupReverseAttachmentLink
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.stripBattlefieldComponents
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
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
 * Gathers all valid options from battlefield (permanents you control), hand,
 * and graveyard, then presents a single selection decision. If total available
 * is less than count, exiles everything automatically.
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
        val playerId = EffectExecutorUtils.resolvePlayerTarget(effect.target, context)
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
         */
        fun exileEntities(
            state: GameState,
            playerId: EntityId,
            entityIds: List<EntityId>
        ): ExecutionResult {
            var newState = state
            val events = mutableListOf<GameEvent>()

            for (entityId in entityIds) {
                val container = newState.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                // Find the current zone of this entity
                val currentZoneEntry = newState.zones.entries.find { (_, cards) -> entityId in cards }
                    ?: continue

                val fromZone = currentZoneEntry.key
                val ownerId = container.get<OwnerComponent>()?.playerId
                    ?: cardComponent.ownerId
                    ?: playerId
                val exileZone = ZoneKey(ownerId, Zone.EXILE)

                // Strip battlefield components if leaving the battlefield
                if (fromZone.zoneType == Zone.BATTLEFIELD) {
                    newState = cleanupReverseAttachmentLink(newState, entityId)
                    newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }
                }

                newState = newState.removeFromZone(fromZone, entityId)
                newState = newState.addToZone(exileZone, entityId)

                events.add(
                    ZoneChangeEvent(
                        entityId = entityId,
                        entityName = cardComponent.name,
                        fromZone = fromZone.zoneType,
                        toZone = Zone.EXILE,
                        ownerId = ownerId
                    )
                )
            }

            return ExecutionResult.success(newState, events)
        }
    }
}
