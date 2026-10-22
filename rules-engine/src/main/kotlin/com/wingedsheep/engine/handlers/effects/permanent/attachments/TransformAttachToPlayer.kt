package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TransformAttachToPlayerContinuation
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.copy.CopyAuraEntry
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PendingTransformAttachComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * The choice behind [com.wingedsheep.sdk.scripting.AttachToPlayerAsTransforms] — "As this permanent
 * transforms into Curse of Leeches, attach it to a player."
 *
 * `flipDfcInPlace` marks the permanent with [PendingTransformAttachComponent] as it turns over; this
 * settles the mark. CR 712.20 applies an "As [this permanent] transforms …" ability while the
 * permanent transforms, so the choice has to land before anything can observe the Aura unattached:
 *
 *  - a day/night transform happens in the untap step's turn-based action, where nobody gets
 *    priority — `BeginningPhaseManager.performUntapStep` settles it before the step ends, so the
 *    enchanted player's upkeep trigger sees the attachment that same turn;
 *  - any other transform (an effect making it day, a daybound permanent entering) is settled by
 *    [com.wingedsheep.engine.mechanics.sba.permanent.TransformAttachCheck], which runs before the
 *    CR 704.5m check would put an unattached Aura into the graveyard.
 *
 * Attaching doesn't target, so hexproof doesn't stop it; a player the Aura can't legally enchant —
 * one its enchant ability excludes, or one with protection from it (CR 702.16c) — can't be chosen.
 * With nobody to choose, the mark is dropped and 704.5m applies.
 */
object TransformAttachToPlayer {

    /**
     * Settle every pending transform attachment on the battlefield. Returns null when there is none
     * (callers carry on untouched); otherwise either the paused result asking about the first one
     * that needs a choice, or a finished result carrying the attach events of the ones that didn't.
     */
    fun settle(
        state: GameState,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        events: List<GameEvent> = emptyList()
    ): ExecutionResult? {
        val pending = state.getBattlefield().filter { state.getEntity(it)?.has<PendingTransformAttachComponent>() == true }
        if (pending.isEmpty()) return null

        var newState = state
        val allEvents = events.toMutableList()
        for (auraId in pending) {
            val candidates = candidates(newState, auraId, cardRegistry, predicateEvaluator)
            when (candidates.size) {
                0 -> newState = newState.updateEntity(auraId) { it.without<PendingTransformAttachComponent>() }
                1 -> {
                    val (attached, attachEvents) = attach(newState, auraId, candidates.single())
                    newState = attached
                    allEvents += attachEvents
                }
                else -> return ask(newState, auraId, candidates, allEvents)
            }
        }
        return ExecutionResult.success(newState, allEvents)
    }

    /**
     * Attach [auraId] to the chosen [playerId] and clear its mark. A permanent that has since left the
     * battlefield, or turned back over (the mark is gone), is left alone.
     */
    fun attach(state: GameState, auraId: EntityId, playerId: EntityId): Pair<GameState, List<GameEvent>> {
        val container = state.getEntity(auraId) ?: return state to emptyList()
        if (auraId !in state.getBattlefield() || !container.has<PendingTransformAttachComponent>()) {
            return state to emptyList()
        }
        val unmarked = state.updateEntity(auraId) { it.without<PendingTransformAttachComponent>() }
        val controllerId = unmarked.projectedState.getController(auraId) ?: playerId
        return AttachmentMover.attach(unmarked, auraId, playerId, controllerId)
    }

    /**
     * The players [auraId] could legally enchant (CR 303.4) — its enchant ability and player
     * protection, but not targeting restrictions such as hexproof, since attaching doesn't target —
     * opponents first in turn order from its controller, then its controller. The same host test an
     * Aura put onto the battlefield without being cast gets ([CopyAuraEntry.legalHosts]).
     */
    fun candidates(
        state: GameState,
        auraId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): List<EntityId> {
        val controllerId = state.projectedState.getController(auraId) ?: return emptyList()
        val card = state.getEntity(auraId)?.get<CardComponent>() ?: return emptyList()
        val legal = CopyAuraEntry.legalHosts(
            state, auraId, card, controllerId, cardRegistry, TargetFinder(predicateEvaluator), predicateEvaluator
        ).toSet()
        val players = state.activePlayers
        val start = players.indexOf(controllerId).coerceAtLeast(0)
        return (players.drop(start + 1) + players.take(start + 1)).filter { it in legal }
    }

    private fun ask(
        state: GameState,
        auraId: EntityId,
        candidates: List<EntityId>,
        events: List<GameEvent>
    ): ExecutionResult {
        val chooserId = state.projectedState.getController(auraId)!!
        val name = state.getEntity(auraId)?.get<CardComponent>()?.name ?: "this Aura"
        val question = { decisionId: String -> ChooseOptionDecision(
            id = decisionId,
            playerId = chooserId,
            prompt = "Choose a player to attach $name to",
            context = DecisionContext(
                sourceId = auraId,
                sourceName = name,
                phase = DecisionPhase.STATE_BASED
            ),
            options = candidates.map { playerId ->
                val playerName = state.getEntity(playerId)?.get<PlayerComponent>()?.name ?: "Player ${playerId.value}"
                if (playerId == chooserId) "$playerName (you)" else playerName
            }
        ) }
        return state.suspendForDecision(question, TransformAttachToPlayerContinuation(auraId, candidates), events)
    }
}
