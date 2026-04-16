package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class SearchLibraryHandler : AiDecisionHandler<SearchLibraryDecision> {
    override val decisionType: KClass<SearchLibraryDecision> = SearchLibraryDecision::class

    override fun autoResolve(decision: SearchLibraryDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: SearchLibraryDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Search library (${decision.filterDescription}):")
        sb.appendLine("Select ${decision.minSelections}-${decision.maxSelections} card(s):")
        for ((j, eid) in decision.options.withIndex()) {
            val info = decision.cards[eid]
            val name = info?.name ?: "Unknown"
            val cost = info?.manaCost ?: ""
            val type = info?.typeLine ?: ""
            sb.appendLine("  [${GameStateFormatter.actionLetter(j)}] $name $cost — $type")
        }
        if (decision.minSelections == 0) {
            sb.appendLine("  [${GameStateFormatter.actionLetter(decision.options.size)}] Fail to find (select nothing)")
        }
    }

    override fun parse(
        response: String,
        decision: SearchLibraryDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val indices = parser.parseMultipleSelections(response, decision.options.size - 1)
        val selected = if (indices != null) {
            indices.take(decision.maxSelections).map { decision.options[it] }
        } else {
            if (decision.options.isNotEmpty()) listOf(decision.options.first()) else emptyList()
        }
        return CardsSelectedResponse(decisionId = decision.id, selectedCards = selected)
    }

    override fun heuristic(decision: SearchLibraryDecision, state: ClientGameState): DecisionResponse {
        val selected = if (decision.options.isNotEmpty()) {
            decision.options.take(decision.maxSelections.coerceAtMost(1))
        } else {
            emptyList()
        }
        return CardsSelectedResponse(decisionId = decision.id, selectedCards = selected)
    }
}
