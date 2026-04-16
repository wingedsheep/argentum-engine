package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class DistributeHandler : AiDecisionHandler<DistributeDecision> {
    override val decisionType: KClass<DistributeDecision> = DistributeDecision::class

    override fun autoResolve(decision: DistributeDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: DistributeDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Distribute ${decision.totalAmount} among targets (min ${decision.minPerTarget} each):")
        for ((j, tid) in decision.targets.withIndex()) {
            val card = state.cards[tid]
            val name = card?.name ?: "Player"
            val label = labels[tid] ?: tid.value
            sb.appendLine("  [${GameStateFormatter.actionLetter(j)}] [$label] $name")
        }
        sb.appendLine("Reply with amounts per target (e.g., \"A:2, B:1\").")
    }

    override fun parse(
        response: String,
        decision: DistributeDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val dist = parser.parseDistribution(response, decision.targets.size, decision.totalAmount)
            ?: return null
        val entityDist = dist.entries.associate { (idx, amount) -> decision.targets[idx] to amount }
        return DistributionResponse(decisionId = decision.id, distribution = entityDist)
    }

}
