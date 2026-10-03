package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import kotlinx.serialization.Serializable

/** One addressable player question and the stored operation that consumes its answer. */
@Serializable
@ConsistentCopyVisibility
data class Suspension internal constructor(
    val question: PendingDecision,
    val answer: AnswerContinuation,
) : ContinuationFrame

/**
 * Create a NEW suspension after the caller has completed its preceding gameplay changes.
 *
 * Allocation, question/answer association, installation and the request event belong to this
 * operation. The factory lambda only describes the question; it is evaluated now and is never
 * stored. Automatic continuation work carries no routing ID. Outer execution layers propagate
 * the returned pause; reopening an existing question restores its [Suspension] without allocating.
 */
fun <D : PendingDecision> GameState.suspendForDecision(
    question: (String) -> D,
    answer: (D) -> AnswerContinuation,
    events: List<GameEvent> = emptyList(),
    eventType: String? = null,
): ExecutionResult {
    check(pendingDecision == null) { "Cannot replace an unanswered suspension with a new question" }
    val (id, allocated) = newRoutingId()
    val decision = question(id)
    require(decision.id == id) { "Question factory must use its allocated suspension identity" }
    val suspended = allocated.restoreSuspension(Suspension(decision, answer(decision)))
    return ExecutionResult.propagatePause(
        suspended,
        events + DecisionRequestedEvent(id, decision.playerId, eventType ?: decision.requestEventType(), decision.prompt),
    )
}

/** Variant for an answer payload that does not need fields derived while describing the question. */
fun GameState.suspendForDecision(
    question: (String) -> PendingDecision,
    answer: AnswerContinuation,
    events: List<GameEvent> = emptyList(),
    eventType: String? = null,
): ExecutionResult = suspendForDecision(question, { answer }, events, eventType)

/** Reinstall an existing question, for example after a nested mana ability. No allocation/event. */
fun GameState.restoreSuspension(suspension: Suspension): GameState {
    check(pendingDecision == null) { "Cannot overwrite an unanswered suspension" }
    return copy(continuationStack = continuationStack + suspension)
}

private fun PendingDecision.requestEventType(): String = when (this) {
    is PlayCardDecision -> "PLAY_CARD"
    is ChooseTargetsDecision -> "CHOOSE_TARGETS"
    is SelectCardsDecision -> "SELECT_CARDS"
    is SearchLibraryDecision -> "SEARCH_LIBRARY"
    is ReorderLibraryDecision -> "REORDER_LIBRARY"
    is YesNoDecision -> "YES_NO"
    is ChooseModeDecision -> "CHOOSE_MODE"
    is ChooseColorDecision -> "CHOOSE_COLOR"
    is ChooseNumberDecision -> "CHOOSE_NUMBER"
    is DistributeDecision -> "DISTRIBUTE"
    is OrderObjectsDecision -> "ORDER_OBJECTS"
    is SplitPilesDecision -> "SPLIT_PILES"
    is ChooseOptionDecision -> "CHOOSE_OPTION"
    is ChooseReplacementDecision -> "CHOOSE_REPLACEMENT"
    is BudgetModalDecision -> "BUDGET_MODAL"
    is AssignDamageDecision -> "ASSIGN_DAMAGE"
    is CombatResolutionDecision -> "COMBAT_RESOLUTION"
    is SelectManaSourcesDecision -> "SELECT_MANA_SOURCES"
    is BatchYesNoDecision -> "BATCH_YES_NO"
}
