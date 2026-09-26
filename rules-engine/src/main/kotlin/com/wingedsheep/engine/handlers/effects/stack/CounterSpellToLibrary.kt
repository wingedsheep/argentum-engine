package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CounterToLibraryPositionContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.mechanics.stack.SpellCounterer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CounterDestination

/**
 * Counter [spellId] into its owner's library per [destination] — the one entry point for
 * [CounterDestination.Library], shared by the plain counter and the "unless they pay" counter.
 *
 * One position counters straight away. Several are Hinder's "your choice of the top or bottom":
 * the *counter's* controller ([countererId]) chooses (Hinder's 2020-08-07 ruling), and only when
 * the spell would really land there — an uncounterable spell, or one a counter replacement
 * exiles instead, is handed to the counterer without a prompt, which then does nothing or
 * exiles it.
 */
internal fun counterSpellToLibrary(
    state: GameState,
    counterer: SpellCounterer,
    spellId: EntityId,
    destination: CounterDestination.Library,
    countererId: EntityId,
    sourceId: EntityId?
): ExecutionResult {
    val positions = destination.positions
    if (positions.size == 1 || !counterer.wouldReachCounterDestination(state, spellId, countererId)) {
        return counterer.counterSpellToLibrary(state, spellId, positions.first(), countererId)
    }
    val spellName = state.getEntity(spellId)?.get<CardComponent>()?.name ?: "the spell"
    val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
    val options = positions.map { it.label }
    val decision = { decisionId: String ->
        ChooseOptionDecision(
            id = decisionId,
            playerId = countererId,
            prompt = "Put $spellName on the ${positions.joinToString(" or ") { it.phrase }} of its owner's library",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = options
        )
    }
    val continuation = CounterToLibraryPositionContinuation(
        spellId = spellId,
        countererId = countererId,
        sourceId = sourceId,
        positions = positions
    )
    return state.suspendForDecision(decision, continuation)
}
