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

    override fun heuristic(decision: DistributeDecision, state: ClientGameState): DecisionResponse {
        val myId = state.viewingPlayerId
        // Concentrate damage to kill creatures, prioritizing opponent's biggest threats.
        // Sort opponent targets by toughness (try to kill the biggest we can), then own targets last.
        val targets = decision.targets.sortedWith(compareBy<EntityId> { tid ->
            val card = state.cards[tid]
            if (card != null && card.controllerId == myId) 1 else 0 // Opponent's targets first
        }.thenByDescending { tid ->
            state.cards[tid]?.toughness ?: 0 // Biggest first among opponent's
        })

        val dist = mutableMapOf<EntityId, Int>()
        var remaining = decision.totalAmount

        // Assign minimum required to each target first
        for (tid in targets) {
            val min = decision.minPerTarget.coerceAtLeast(0)
            dist[tid] = min
            remaining -= min
        }

        // Then pile remaining damage onto the best targets (try to reach lethal toughness)
        for (tid in targets) {
            if (remaining <= 0) break
            val card = state.cards[tid]
            val toughness = card?.toughness ?: 1
            val damage = card?.damage ?: 0
            val currentAssigned = dist[tid] ?: 0
            val neededToKill = (toughness - damage - currentAssigned).coerceAtLeast(0)
            val maxForTarget = decision.maxPerTarget[tid] ?: Int.MAX_VALUE
            val extra = neededToKill.coerceAtMost(remaining).coerceAtMost(maxForTarget - currentAssigned)
            if (extra > 0) {
                dist[tid] = currentAssigned + extra
                remaining -= extra
            }
        }

        // If still remaining, dump on first target
        if (remaining > 0 && targets.isNotEmpty()) {
            val firstTarget = targets.first()
            dist[firstTarget] = (dist[firstTarget] ?: 0) + remaining
        }

        return DistributionResponse(decisionId = decision.id, distribution = dist)
    }
}
