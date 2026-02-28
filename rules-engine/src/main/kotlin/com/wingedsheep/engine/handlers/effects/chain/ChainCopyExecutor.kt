package com.wingedsheep.engine.handlers.effects.chain

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.moveCardToZone
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChainAction
import com.wingedsheep.sdk.scripting.effects.ChainCopyCost
import com.wingedsheep.sdk.scripting.effects.ChainCopyEffect
import com.wingedsheep.sdk.scripting.effects.CopyRecipient
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Unified executor for all Chain of X effects.
 *
 * Executes the primary action, then determines who gets the copy offer,
 * checks prerequisites, and pauses for the yes/no decision.
 */
class ChainCopyExecutor(
    private val targetFinder: TargetFinder = TargetFinder(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ChainCopyEffect> {

    override val effectType: KClass<ChainCopyEffect> = ChainCopyEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: ChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        // Step 1: Execute the primary action
        return when (val action = effect.action) {
            is ChainAction.Destroy -> executeDestroy(state, effect, context)
            is ChainAction.BounceToHand -> executeBounce(state, effect, context)
            is ChainAction.DealDamage -> executeDamage(state, effect, action, context)
            is ChainAction.Discard -> executeDiscard(state, effect, action, context)
            is ChainAction.PreventAllDamageDealt -> executePreventDamage(state, effect, context)
        }
    }

    // =========================================================================
    // Primary action executors
    // =========================================================================

    private fun executeDestroy(
        state: GameState,
        effect: ChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        val targetControllerId = container.get<ControllerComponent>()?.playerId
            ?: container.get<CardComponent>()?.ownerId
            ?: return ExecutionResult.success(state)

        val destroyResult = destroyPermanent(state, targetId)
        if (!destroyResult.isSuccess) return destroyResult

        return offerChainCopy(
            destroyResult.state, destroyResult.events.toMutableList(),
            targetControllerId, effect, context
        )
    }

    private fun executeBounce(
        state: GameState,
        effect: ChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        val targetControllerId = container.get<ControllerComponent>()?.playerId
            ?: container.get<CardComponent>()?.ownerId
            ?: return ExecutionResult.success(state)

        val bounceResult = moveCardToZone(state, targetId, Zone.HAND)
        if (!bounceResult.isSuccess) return bounceResult

        return offerChainCopy(
            bounceResult.state, bounceResult.events.toMutableList(),
            targetControllerId, effect, context
        )
    }

    private fun executeDamage(
        state: GameState,
        effect: ChainCopyEffect,
        action: ChainAction.DealDamage,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.success(state)

        val affectedPlayerId = resolveAffectedPlayer(state, targetId)
            ?: return ExecutionResult.success(state)

        val damageResult = dealDamageToTarget(state, targetId, action.amount, context.sourceId)
        if (!damageResult.isSuccess) return damageResult

        return offerChainCopy(
            damageResult.state, damageResult.events.toMutableList(),
            affectedPlayerId, effect, context
        )
    }

    private fun executeDiscard(
        state: GameState,
        effect: ChainCopyEffect,
        action: ChainAction.Discard,
        context: EffectContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        val handZone = ZoneKey(targetPlayerId, Zone.HAND)
        val hand = state.getZone(handZone)

        // If hand has fewer cards than required, discard all immediately
        if (hand.size <= action.count) {
            val discardResult = discardCards(state, targetPlayerId, hand)
            if (!discardResult.isSuccess) return discardResult

            return offerChainCopy(
                discardResult.state, discardResult.events.toMutableList(),
                targetPlayerId, effect, context
            )
        }

        // Player must choose which cards to discard
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = targetPlayerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose ${action.count} card${if (action.count > 1) "s" else ""} to discard",
            options = hand,
            minSelections = action.count,
            maxSelections = action.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = ChainCopyPrimaryDiscardContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            effect = effect,
            playerId = targetPlayerId,
            sourceId = context.sourceId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    private fun executePreventDamage(
        state: GameState,
        effect: ChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        val targetControllerId = container.get<ControllerComponent>()?.playerId
            ?: container.get<CardComponent>()?.ownerId
            ?: return ExecutionResult.success(state)

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                modification = SerializableModification.PreventAllDamageDealtBy,
                affectedEntities = setOf(targetId)
            ),
            duration = com.wingedsheep.sdk.scripting.Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = effect.spellName,
            controllerId = context.controllerId,
            timestamp = state.timestamp
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return offerChainCopy(
            newState, mutableListOf(),
            targetControllerId, effect, context
        )
    }

    // =========================================================================
    // Chain copy offer logic
    // =========================================================================

    private fun offerChainCopy(
        state: GameState,
        events: MutableList<GameEvent>,
        recipientPlayerId: EntityId,
        effect: ChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        // Check cost prerequisites
        if (effect.copyCost is ChainCopyCost.SacrificeALand) {
            val controllerLands = findControllerLands(state, recipientPlayerId)
            if (controllerLands.isEmpty()) {
                return ExecutionResult.success(state, events)
            }
        }

        if (effect.copyCost is ChainCopyCost.DiscardACard) {
            val handZone = ZoneKey(recipientPlayerId, Zone.HAND)
            val hand = state.getZone(handZone)
            if (hand.isEmpty()) {
                return ExecutionResult.success(state, events)
            }
        }

        // Check if there are valid targets for a potential copy
        val legalTargets = targetFinder.findLegalTargets(
            state, effect.copyTargetRequirement, recipientPlayerId, context.sourceId
        )
        if (legalTargets.isEmpty()) {
            return ExecutionResult.success(state, events)
        }

        // Build the yes/no decision
        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: effect.spellName

        val prompt = when (effect.copyCost) {
            is ChainCopyCost.NoCost -> "Copy ${effect.spellName} and choose a new target?"
            is ChainCopyCost.SacrificeALand -> "Sacrifice a land to copy ${effect.spellName} and choose a new target?"
            is ChainCopyCost.DiscardACard -> "Discard a card to copy ${effect.spellName}?"
        }

        val (yesText, noText) = when (effect.copyCost) {
            is ChainCopyCost.NoCost -> "Copy" to "Decline"
            is ChainCopyCost.SacrificeALand -> "Sacrifice a land" to "Decline"
            is ChainCopyCost.DiscardACard -> "Discard and Copy" to "Decline"
        }

        val decision = YesNoDecision(
            id = decisionId,
            playerId = recipientPlayerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = yesText,
            noText = noText
        )

        val continuation = ChainCopyDecisionContinuation(
            decisionId = decisionId,
            effect = effect,
            copyControllerId = recipientPlayerId,
            sourceId = context.sourceId
        )

        val newState = state.withPendingDecision(decision).pushContinuation(continuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = recipientPlayerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    // =========================================================================
    // Utility methods
    // =========================================================================

    private fun resolveAffectedPlayer(state: GameState, targetId: EntityId): EntityId? {
        val entity = state.getEntity(targetId) ?: return null
        return if (entity.get<ControllerComponent>() != null) {
            entity.get<ControllerComponent>()!!.playerId
        } else {
            targetId
        }
    }

    private fun discardCards(
        state: GameState,
        playerId: EntityId,
        cardIds: List<EntityId>
    ): ExecutionResult {
        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in cardIds) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val cardNames = cardIds.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        return ExecutionResult.success(
            newState,
            listOf(CardsDiscardedEvent(playerId, cardIds, cardNames))
        )
    }

    fun findControllerLands(state: GameState, controllerId: EntityId): List<EntityId> {
        val projected = stateProjector.project(state)
        val controlledPermanents = projected.getBattlefieldControlledBy(controllerId)
        val context = PredicateContext(controllerId = controllerId)
        return controlledPermanents.filter { permanentId ->
            predicateEvaluator.matchesWithProjection(state, projected, permanentId, GameObjectFilter.Land, context)
        }
    }
}
