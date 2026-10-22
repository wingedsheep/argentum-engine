package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.CopiableCharacteristicsChangedEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TransformCopyContinuation
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PendingTransformCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.copiableCardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.BecomesCopyAsTransforms
import com.wingedsheep.sdk.scripting.Duration

/**
 * The choice behind [BecomesCopyAsTransforms] — "As this creature transforms into Olag, Ludevic's
 * Hubris, it becomes a copy of a creature card exiled with it, except …".
 *
 * `flipDfcInPlace` marks the permanent with [PendingTransformCopyComponent] as it turns over; this
 * settles the mark. CR 712.20 applies an "As [this permanent] transforms …" ability while the
 * permanent transforms, so the copy is made at the first point after the flip where the engine can
 * ask — the state-based action pass that follows the transform
 * ([com.wingedsheep.engine.mechanics.sba.permanent.TransformCopyCheck]), which runs before any player
 * gets priority and ahead of every real state-based action. Triggers on the transform itself are
 * detected from its [com.wingedsheep.engine.core.TransformedEvent] at the settle boundary, before this
 * pass, so a "transforms into [quality]" trigger sees the printed face.
 *
 * With one candidate card the copy is made without asking; with several, the permanent's controller
 * picks one; with none it stays its printed face. Either way the face's counters are put on it next.
 */
object TransformCopy {

    /**
     * Settle every pending transform copy on the battlefield. Returns null when there is none
     * (callers carry on untouched); otherwise either the paused result asking about the first one
     * that needs a choice, or a finished result carrying the events of the ones that didn't.
     */
    fun settle(
        state: GameState,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        events: List<GameEvent> = emptyList()
    ): ExecutionResult? {
        val pending = state.getBattlefield().filter { state.getEntity(it)?.has<PendingTransformCopyComponent>() == true }
        if (pending.isEmpty()) return null

        var newState = state
        val allEvents = events.toMutableList()
        for (permanentId in pending) {
            val ability = abilityOf(newState, permanentId, cardRegistry)
            if (ability == null) {
                newState = newState.updateEntity(permanentId) { it.without<PendingTransformCopyComponent>() }
                continue
            }
            val candidates = candidates(newState, permanentId, ability, predicateEvaluator)
            if (candidates.size > 1) return ask(newState, permanentId, ability, candidates, allEvents)
            val (copied, copyEvents) = apply(newState, permanentId, candidates.singleOrNull(), cardRegistry, predicateEvaluator)
            newState = copied
            allEvents += copyEvents
        }
        return ExecutionResult.success(newState, allEvents)
    }

    /**
     * Make [permanentId] a copy of [copiedId] (none: it stays its printed face), put the face's
     * counters on it, and clear its mark. A permanent that has since left the battlefield or lost the
     * mark is left alone.
     */
    fun apply(
        state: GameState,
        permanentId: EntityId,
        copiedId: EntityId?,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): Pair<GameState, List<GameEvent>> {
        val container = state.getEntity(permanentId) ?: return state to emptyList()
        if (permanentId !in state.getBattlefield() || !container.has<PendingTransformCopyComponent>()) {
            return state to emptyList()
        }
        // Read before the copy replaces the face: the replacement is the transformed-into face's own.
        val ability = abilityOf(state, permanentId, cardRegistry)
        var newState = state.updateEntity(permanentId) { it.without<PendingTransformCopyComponent>() }
        if (ability == null) return newState to emptyList()
        val controllerId = newState.projectedState.getController(permanentId)
            ?: container.get<CardComponent>()?.ownerId
            ?: return newState to emptyList()
        val events = mutableListOf<GameEvent>()

        val copiedCard = copiedId?.let { newState.getEntity(it)?.copiableCardComponent() }
        if (copiedCard != null) {
            newState = becomeCopyInPlace(
                newState, permanentId, copiedCard, ability.exceptions, Duration.Permanent, controllerId, cardRegistry
            ) ?: newState
            events += CopiableCharacteristicsChangedEvent(permanentId)
        }

        ability.counterCount?.let { amount ->
            val count = predicateEvaluator.amounts.evaluate(
                newState, amount, EffectContext(sourceId = permanentId, controllerId = controllerId)
            )
            val name = newState.getEntity(permanentId)?.get<CardComponent>()?.name ?: ""
            val (withCounters, counterEvents) = EntersWithReplacements.placeEntryCounters(
                newState, permanentId, ability.counterType, count, controllerId, name,
                predicateEvaluator = predicateEvaluator
            )
            newState = withCounters
            events += counterEvents
        }
        return newState to events
    }

    /** The [BecomesCopyAsTransforms] of the face [permanentId] is showing, if any. */
    private fun abilityOf(state: GameState, permanentId: EntityId, cardRegistry: CardRegistry): BecomesCopyAsTransforms? {
        val definitionId = state.getEntity(permanentId)?.get<CardComponent>()?.cardDefinitionId ?: return null
        return cardRegistry.getCard(definitionId)?.script?.replacementEffects
            ?.filterIsInstance<BecomesCopyAsTransforms>()?.firstOrNull()
    }

    /** The cards [permanentId] could become a copy of, read with it as the source (CR 607.2a). */
    private fun candidates(
        state: GameState,
        permanentId: EntityId,
        ability: BecomesCopyAsTransforms,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        val controllerId = state.projectedState.getController(permanentId) ?: return emptyList()
        val pool = when (ability.copyFromZone) {
            Zone.BATTLEFIELD -> state.getBattlefield()
            else -> state.turnOrder.flatMap { state.getZone(com.wingedsheep.engine.state.ZoneKey(it, ability.copyFromZone)) }
        }
        val context = PredicateContext(controllerId = controllerId, sourceId = permanentId)
        return pool.filter { candidateId ->
            candidateId != permanentId &&
                predicateEvaluator.matches(state, state.projectedState, candidateId, ability.copyFilter, context)
        }
    }

    private fun ask(
        state: GameState,
        permanentId: EntityId,
        ability: BecomesCopyAsTransforms,
        candidates: List<EntityId>,
        events: List<GameEvent>
    ): ExecutionResult {
        val chooserId = state.projectedState.getController(permanentId)!!
        val name = state.getEntity(permanentId)?.get<CardComponent>()?.name ?: "this creature"
        val question = { decisionId: String -> SelectCardsDecision(
            id = decisionId,
            playerId = chooserId,
            prompt = "Choose a card for $name to become a copy of",
            context = DecisionContext(
                sourceId = permanentId,
                sourceName = name,
                phase = DecisionPhase.STATE_BASED
            ),
            options = candidates,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = ability.copyFromZone == Zone.BATTLEFIELD
        ) }
        return state.suspendForDecision(question, TransformCopyContinuation(permanentId, candidates), events)
    }
}
