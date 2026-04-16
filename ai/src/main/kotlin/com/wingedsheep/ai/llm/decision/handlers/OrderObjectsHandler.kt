package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class OrderObjectsHandler : AiDecisionHandler<OrderObjectsDecision> {
    override val decisionType: KClass<OrderObjectsDecision> = OrderObjectsDecision::class

    override fun autoResolve(decision: OrderObjectsDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: OrderObjectsDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Order these objects (first receives priority):")
        for ((j, eid) in decision.objects.withIndex()) {
            val card = state.cards[eid]
            val info = decision.cardInfo?.get(eid)
            val name = card?.name ?: info?.name ?: "Unknown"
            sb.appendLine("  [${GameStateFormatter.actionLetter(j)}] $name")
        }
        sb.appendLine("Reply with the order (e.g., \"B, A, C\").")
    }

    override fun parse(
        response: String,
        decision: OrderObjectsDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val ordering = parser.parseOrdering(response, decision.objects.size) ?: return null
        return OrderedResponse(decisionId = decision.id, orderedObjects = ordering.map { decision.objects[it] })
    }

    override fun heuristic(decision: OrderObjectsDecision, state: ClientGameState): DecisionResponse {
        return OrderedResponse(decisionId = decision.id, orderedObjects = decision.objects)
    }
}
