package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SacrificeUnlessSacrificePermanentEffect
import kotlin.reflect.KClass

/**
 * Executor for SacrificeUnlessSacrificePermanentEffect.
 *
 * "When [this creature] enters the battlefield, sacrifice it unless you sacrifice N [permanent type]."
 *
 * Examples:
 * - Plant Elemental: "sacrifice it unless you sacrifice a Forest"
 * - Primeval Force: "sacrifice it unless you sacrifice three Forests"
 *
 * The player is presented with a selection of valid permanents to sacrifice.
 * If they select exactly the required count, those permanents are sacrificed and the source survives.
 * If they select 0 (or don't have enough), the source is sacrificed instead.
 */
class SacrificeUnlessSacrificePermanentExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<SacrificeUnlessSacrificePermanentEffect> {

    override val effectType: KClass<SacrificeUnlessSacrificePermanentEffect> =
        SacrificeUnlessSacrificePermanentEffect::class

    override fun execute(
        state: GameState,
        effect: SacrificeUnlessSacrificePermanentEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for sacrifice unless effect")

        val controllerId = context.controllerId

        // Find source card info
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source entity not found")
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source has no card component")

        // Check if the source is still on the battlefield (could have been removed by other effects)
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
        if (sourceId !in state.getZone(battlefieldZone)) {
            // Source is no longer on battlefield - nothing to do
            return ExecutionResult.success(state)
        }

        // Find all valid permanents of the required type that the player controls
        val validPermanents = findValidPermanents(state, controllerId, effect.permanentType, sourceId)

        // If the player doesn't have enough permanents, automatically sacrifice the source
        if (validPermanents.size < effect.count) {
            return sacrificeSource(state, controllerId, sourceId, sourceCard.name)
        }

        // Player has enough - present the decision
        // Use minSelections = 0 to allow declining (which sacrifices the source)
        val prompt = buildPrompt(effect.permanentType, effect.count, sourceCard.name)

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceCard.name,
            prompt = prompt,
            options = validPermanents,
            minSelections = 0,
            maxSelections = effect.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        // Push continuation to handle the response
        val continuation = SacrificeUnlessSacrificeContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceCard.name,
            permanentType = effect.permanentType,
            requiredCount = effect.count
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Find all permanents of the specified type that the player controls.
     * Excludes the source permanent itself.
     */
    private fun findValidPermanents(
        state: GameState,
        playerId: EntityId,
        permanentType: String,
        sourceId: EntityId
    ): List<EntityId> {
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val battlefield = state.getZone(battlefieldZone)

        val targetSubtype = Subtype.of(permanentType)

        return battlefield.filter { permanentId ->
            if (permanentId == sourceId) return@filter false

            val container = state.getEntity(permanentId) ?: return@filter false
            val card = container.get<CardComponent>() ?: return@filter false

            // Check if the permanent has the required subtype
            targetSubtype in card.typeLine.subtypes
        }
    }

    /**
     * Build the prompt message for the decision.
     */
    private fun buildPrompt(permanentType: String, count: Int, sourceName: String): String {
        val typeText = if (count == 1) {
            val article = if (permanentType.first().lowercaseChar() in "aeiou") "a" else "a"
            "$article $permanentType"
        } else {
            "$count ${permanentType}s"
        }
        return "Sacrifice $typeText to keep $sourceName, or skip to sacrifice $sourceName"
    }

    companion object {
        /**
         * Sacrifice the source permanent.
         */
        fun sacrificeSource(
            state: GameState,
            playerId: EntityId,
            sourceId: EntityId,
            sourceName: String
        ): ExecutionResult {
            val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
            val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

            var newState = state.removeFromZone(battlefieldZone, sourceId)
            newState = newState.addToZone(graveyardZone, sourceId)

            val events = listOf(
                PermanentsSacrificedEvent(playerId, listOf(sourceId)),
                ZoneChangeEvent(
                    entityId = sourceId,
                    entityName = sourceName,
                    fromZone = ZoneType.BATTLEFIELD,
                    toZone = ZoneType.GRAVEYARD,
                    ownerId = playerId
                )
            )

            return ExecutionResult.success(newState, events)
        }
    }
}
