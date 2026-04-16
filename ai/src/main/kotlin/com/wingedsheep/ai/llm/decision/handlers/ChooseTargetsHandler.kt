package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class ChooseTargetsHandler : AiDecisionHandler<ChooseTargetsDecision> {
    override val decisionType: KClass<ChooseTargetsDecision> = ChooseTargetsDecision::class

    override fun autoResolve(decision: ChooseTargetsDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: ChooseTargetsDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        val multiTarget = decision.targetRequirements.size > 1
        for (req in decision.targetRequirements) {
            sb.appendLine("Target ${req.index + 1}: ${req.description} (choose ${req.minTargets}-${req.maxTargets})")
            val validIds = decision.legalTargets[req.index] ?: emptyList()
            for ((j, tid) in validIds.withIndex()) {
                val card = state.cards[tid]
                val label = labels[tid] ?: tid.value
                val letter = GameStateFormatter.actionLetter(j)
                if (card != null) {
                    val owner = if (card.controllerId == state.viewingPlayerId) "your" else "opponent's"
                    val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                    val keywords = card.keywords.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") { it.name.lowercase() }?.let { " [$it]" } ?: ""
                    sb.appendLine("  [$letter] [$label] $owner ${card.name}$stats$keywords")
                } else {
                    val playerName = if (tid == state.viewingPlayerId) "you" else "opponent"
                    val life = state.players.find { it.playerId == tid }?.life
                    val lifeStr = if (life != null) " (life: $life)" else ""
                    sb.appendLine("  [$letter] [$label] $playerName$lifeStr")
                }
            }
        }
        if (multiTarget) {
            sb.appendLine()
            sb.appendLine("Reply with one letter per target, separated by commas.")
            sb.appendLine("Example: \"A, B\" means Target 1 = A, Target 2 = B")
            sb.appendLine("Each letter refers to the options listed under that target.")
        }
    }

    override fun parse(
        response: String,
        decision: ChooseTargetsDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        return if (decision.targetRequirements.size == 1) {
            val req = decision.targetRequirements[0]
            val validTargets = decision.legalTargets[req.index] ?: return null
            val index = parser.parseActionChoice(response, validTargets.size - 1) ?: return null
            TargetsResponse(
                decisionId = decision.id,
                selectedTargets = mapOf(req.index to listOf(validTargets[index]))
            )
        } else {
            // Multi-target: parse comma-separated letters, one per requirement
            val maxIndices = decision.targetRequirements.map { req ->
                (decision.legalTargets[req.index]?.size ?: 1) - 1
            }
            val indices = parser.parseMultiTargetSelections(response, maxIndices)
            if (indices != null && indices.size == decision.targetRequirements.size) {
                val result = decision.targetRequirements.zip(indices).associate { (req, idx) ->
                    val validTargets = decision.legalTargets[req.index] ?: return null
                    if (idx >= validTargets.size) return null
                    req.index to listOf(validTargets[idx])
                }
                TargetsResponse(decisionId = decision.id, selectedTargets = result)
            } else {
                // Fallback: try single letter for all requirements
                val result = mutableMapOf<Int, List<EntityId>>()
                for (req in decision.targetRequirements) {
                    val validTargets = decision.legalTargets[req.index] ?: continue
                    val index = parser.parseActionChoice(response, validTargets.size - 1)
                    if (index != null) {
                        result[req.index] = listOf(validTargets[index])
                    }
                }
                if (result.isNotEmpty()) {
                    TargetsResponse(decisionId = decision.id, selectedTargets = result)
                } else null
            }
        }
    }

}
