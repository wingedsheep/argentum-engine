package com.wingedsheep.ai.llm.decision

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

/**
 * Self-contained handler for a single PendingDecision type.
 *
 * Each handler encapsulates:
 * - Formatting the decision for the LLM
 * - Parsing the LLM's response
 * - Optional auto-resolve (skip LLM entirely)
 *
 * When the LLM fails, the [com.wingedsheep.ai.llm.LlmAiPlayerController] delegates
 * to its fallback [com.wingedsheep.ai.AiPlayerController] (typically the engine AI).
 *
 * Adding a new decision type = one new handler file + one registry line.
 */
interface AiDecisionHandler<D : PendingDecision> {
    val decisionType: KClass<D>

    /** True if this decision can be resolved without querying the LLM. */
    fun canAutoResolve(decision: D): Boolean = false

    /** Fast-path resolution (only called when [canAutoResolve] returns true). */
    fun autoResolve(decision: D): DecisionResponse

    /** Format the decision as LLM-readable text appended to [sb]. */
    fun format(
        sb: StringBuilder,
        decision: D,
        state: ClientGameState,
        labels: Map<EntityId, String>
    )

    /** Parse the LLM response into a [DecisionResponse], or null if unparseable. */
    fun parse(
        response: String,
        decision: D,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse?
}
