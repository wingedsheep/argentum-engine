package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class BudgetModalHandler : AiDecisionHandler<BudgetModalDecision> {
    override val decisionType: KClass<BudgetModalDecision> = BudgetModalDecision::class

    override fun autoResolve(decision: BudgetModalDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: BudgetModalDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("  Budget: ${decision.budget} pawprints")
        for ((j, mode) in decision.modes.withIndex()) {
            sb.appendLine("  [$j] (cost ${mode.cost}) ${mode.description}")
        }
        sb.appendLine("  Reply with mode indices (e.g., '0,0,1' for mode 0 twice + mode 1 once)")
    }

    override fun parse(
        response: String,
        decision: BudgetModalDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val trimmed = response.trim()
        if (trimmed.isEmpty() || trimmed == "none") {
            return BudgetModalResponse(decisionId = decision.id, selectedModeIndices = emptyList())
        }
        val indices = trimmed.split(",").mapNotNull { it.trim().toIntOrNull() }
        return BudgetModalResponse(decisionId = decision.id, selectedModeIndices = indices)
    }

    override fun heuristic(decision: BudgetModalDecision, state: ClientGameState): DecisionResponse {
        // Greedy: pick cheapest mode repeatedly until budget exhausted
        val sorted = decision.modes.withIndex().sortedBy { it.value.cost }
        val selected = mutableListOf<Int>()
        var remaining = decision.budget
        for ((idx, mode) in sorted) {
            while (mode.cost <= remaining) {
                selected.add(idx)
                remaining -= mode.cost
            }
        }
        return BudgetModalResponse(decisionId = decision.id, selectedModeIndices = selected)
    }
}
