package com.wingedsheep.gym.contract

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.LegalAction

/**
 * Per-step mapping from observation-facing action IDs to executable engine
 * actions or decision responses.
 *
 * ## Lifecycle
 *
 * A new registry is built every time [TrainingObservation] is produced
 * (i.e., after every `reset()` / `step()`). IDs are **not stable across
 * steps** — the same integer means different things at different times.
 * Consumers must treat them as opaque handles valid only for the current
 * observation.
 *
 * This is documented as a contract guarantee: the server can regenerate
 * IDs freely between steps, and trainers must not cache them.
 */
class ActionRegistry private constructor(
    private val legalActionsById: Map<Int, LegalAction>,
    private val decisionResponsesById: Map<Int, DecisionResponse>
) {
    /** Resolves [actionId] to its engine-level representation. */
    fun resolve(actionId: Int): ResolvedAction {
        legalActionsById[actionId]?.let { return ResolvedAction.Legal(it) }
        decisionResponsesById[actionId]?.let { return ResolvedAction.Decision(it) }
        return ResolvedAction.Unknown
    }

    /** All legal-action entries in ID order. Empty when mid-decision. */
    val legalActions: List<Pair<Int, LegalAction>>
        get() = legalActionsById.entries.sortedBy { it.key }.map { it.key to it.value }

    /** All decision-response entries in ID order. Empty when not in a decision. */
    val decisionResponses: List<Pair<Int, DecisionResponse>>
        get() = decisionResponsesById.entries.sortedBy { it.key }.map { it.key to it.value }

    val size: Int get() = legalActionsById.size + decisionResponsesById.size

    companion object {
        val EMPTY = ActionRegistry(emptyMap(), emptyMap())

        /**
         * Build a registry for the legal-action case (no pending decision).
         * IDs are assigned in input order starting from 0.
         */
        fun ofLegalActions(actions: List<LegalAction>): ActionRegistry {
            val byId = HashMap<Int, LegalAction>(actions.size)
            actions.forEachIndexed { idx, action -> byId[idx] = action }
            return ActionRegistry(byId, emptyMap())
        }

        /**
         * Build a registry for a folded decision. IDs are assigned in input
         * order starting from 0. Used for simple decisions (YesNo,
         * ChooseNumber, ChooseMode, ChooseOption, ChooseColor, and
         * single-select SelectCards).
         */
        fun ofDecisionResponses(responses: List<DecisionResponse>): ActionRegistry {
            val byId = HashMap<Int, DecisionResponse>(responses.size)
            responses.forEachIndexed { idx, response -> byId[idx] = response }
            return ActionRegistry(emptyMap(), byId)
        }
    }
}

/** Result of [ActionRegistry.resolve]. */
sealed interface ResolvedAction {
    /** The ID maps to a concrete [LegalAction] — extract [LegalAction.action] to step. */
    data class Legal(val legalAction: LegalAction) : ResolvedAction {
        val action: GameAction get() = legalAction.action
    }

    /** The ID maps to a folded [DecisionResponse] — submit via SubmitDecision. */
    data class Decision(val response: DecisionResponse) : ResolvedAction

    /** The ID is not present in the current registry. */
    data object Unknown : ResolvedAction
}
