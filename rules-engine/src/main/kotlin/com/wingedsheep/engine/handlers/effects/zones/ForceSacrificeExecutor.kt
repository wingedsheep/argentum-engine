package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.PermanentSnapshot
import com.wingedsheep.engine.state.components.stack.capturePermanentSnapshots
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import kotlin.reflect.KClass

/**
 * Executor for ForceSacrificeEffect.
 *
 * Handles "target player sacrifices a creature" (edict effects) where an opponent
 * is forced to sacrifice permanents of their choice. Supports multi-player targets
 * like Player.EachOpponent.
 *
 * Examples:
 * - Cabal Executioner: "Whenever ~ deals combat damage to a player, that player sacrifices a creature."
 * - Chainer's Edict: "Target player sacrifices a creature."
 * - The Eldest Reborn: "Each opponent sacrifices a creature or planeswalker."
 */
class ForceSacrificeExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler(),
    private val dynamicAmountEvaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator =
        com.wingedsheep.engine.handlers.DynamicAmountEvaluator()
) : EffectExecutor<ForceSacrificeEffect> {

    override val effectType: KClass<ForceSacrificeEffect> = ForceSacrificeEffect::class

    override fun execute(
        state: GameState,
        effect: ForceSacrificeEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "No valid player for force sacrifice")
        }

        // A dynamic count ("sacrifices half the creatures they control, rounded up") is evaluated
        // once at resolution; otherwise fall back to the fixed edict count. The amount is resolved
        // against the same context so a per-target reference (e.g. Player.ContextPlayer / TargetOpponent)
        // counts the chosen player's permanents.
        val count = effect.dynamicCount
            ?.let { dynamicAmountEvaluator.evaluate(state, it, context) }
            ?: effect.count

        return processPlayers(state, playerIds, effect.filter, count, context.sourceId)
    }

    /**
     * Process sacrifice for a list of players in order. Auto-sacrifices when possible,
     * pauses for a decision when a player must choose, storing remaining players
     * in the continuation.
     */
    fun processPlayers(
        state: GameState,
        playerIds: List<EntityId>,
        filter: GameObjectFilter,
        count: Int,
        sourceId: EntityId?
    ): EffectResult {
        val sourceName = if (sourceId != null) {
            state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"
        } else {
            "Unknown"
        }

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        val allSnapshots = mutableListOf<PermanentSnapshot>()

        for ((index, playerId) in playerIds.withIndex()) {
            val validPermanents = findValidPermanents(currentState, playerId, filter, sourceId)

            if (validPermanents.isEmpty()) {
                continue
            }

            if (validPermanents.size <= count) {
                // Auto-sacrifice without prompting
                val result = sacrificePermanents(currentState, playerId, validPermanents)
                currentState = result.state
                allEvents.addAll(result.events)
                allSnapshots.addAll(result.updatedSacrificedPermanents)
                continue
            }

            // Player must choose — pause and store remaining players in continuation
            val remainingPlayers = playerIds.drop(index + 1)
            return presentSacrificeDecision(
                currentState, playerId, sourceId, sourceName,
                validPermanents, minSelections = count, maxSelections = count,
                remainingPlayers = remainingPlayers, filter = filter, count = count,
                priorEvents = allEvents
            ).copy(updatedSacrificedPermanents = allSnapshots)
        }

        return EffectResult.success(currentState, allEvents)
            .copy(updatedSacrificedPermanents = allSnapshots)
    }

    private fun findValidPermanents(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        sourceId: EntityId?
    ): List<EntityId> {
        // Pass sourceId so source-relative filter predicates (e.g.
        // DealtCombatDamageToSourceControllerThisTurn — Witch-king of Angmar) resolve against
        // the edict's source rather than the sacrificing player.
        return BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, filter.youControl(), PredicateContext(controllerId = playerId, sourceId = sourceId)
        )
    }

    private fun presentSacrificeDecision(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId?,
        sourceName: String,
        validPermanents: List<EntityId>,
        minSelections: Int,
        maxSelections: Int,
        remainingPlayers: List<EntityId>,
        filter: GameObjectFilter,
        count: Int,
        priorEvents: List<GameEvent>
    ): EffectResult {
        // Describe the actual filter ("a land", "a creature", "an artifact"...) rather than
        // hardcoding "creature" — Serendib Djinn / Goblin Firebug sacrifice lands, edicts
        // sacrifice creatures, etc.
        val noun = filter.description
        val prompt = buildString {
            append("Choose ")
            if (minSelections == 1) {
                append(if (noun.firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) "an " else "a ")
                append(noun)
            } else {
                append("$minSelections ${noun}s")
            }
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
            sourceName = sourceName,
            remainingPlayers = remainingPlayers,
            filter = filter,
            count = count
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            priorEvents + decisionResult.events
        )
    }

    internal fun sacrificePermanents(
        state: GameState,
        playerId: EntityId,
        permanentIds: List<EntityId>
    ): EffectResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Capture P/T/subtype/supertype/controller snapshots BEFORE the zone change so
        // a follow-up sibling effect can read the sacrificed permanent's characteristics
        // (Rise of the Witch-king's "if you sacrificed a creature this way…", Nasty End-style
        // "was legendary?" gates). Mirrors SacrificeExecutor's capture path.
        val snapshots = if (permanentIds.isNotEmpty()) {
            capturePermanentSnapshots(permanentIds, newState.projectedState)
        } else {
            emptyList()
        }

        if (permanentIds.isNotEmpty()) {
            val permanentNames = permanentIds.map { id ->
                newState.getEntity(id)?.get<CardComponent>()?.name ?: "Unknown"
            }
            events.add(PermanentsSacrificedEvent(playerId, permanentIds, permanentNames))
            newState = ZoneTransitionService.trackPermanentSacrifice(newState, permanentIds, playerId)
        }

        for (permanentId in permanentIds) {
            val transitionResult = ZoneTransitionService.moveToZone(
                newState, permanentId, Zone.GRAVEYARD
            )
            newState = transitionResult.state
            events.addAll(transitionResult.events)
        }

        return EffectResult.success(newState, events)
            .copy(updatedSacrificedPermanents = snapshots)
    }
}
