package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.PilesSplitResponse
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class SplitPilesHandler : AiDecisionHandler<SplitPilesDecision> {
    override val decisionType: KClass<SplitPilesDecision> = SplitPilesDecision::class

    override fun autoResolve(decision: SplitPilesDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: SplitPilesDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Split into ${decision.numberOfPiles} piles:")
        if (decision.pileLabels.isNotEmpty()) {
            sb.appendLine("Piles: ${decision.pileLabels.joinToString(", ")}")
        }
        for ((j, eid) in decision.cards.withIndex()) {
            val card = state.cards[eid]
            val info = decision.cardInfo?.get(eid)
            val name = card?.name ?: info?.name ?: "Unknown"
            sb.appendLine("  [${GameStateFormatter.actionLetter(j)}] $name")
        }
        sb.appendLine("Reply with pile assignment (e.g., \"Pile 1: A, B; Pile 2: C\").")
    }

    override fun parse(
        response: String,
        decision: SplitPilesDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val pile1Indices = parser.parseMultipleSelections(response, decision.cards.size - 1)
        return if (pile1Indices != null) {
            val pile1 = pile1Indices.map { decision.cards[it] }
            val pile2 = decision.cards.filter { it !in pile1 }
            PilesSplitResponse(decisionId = decision.id, piles = listOf(pile1, pile2))
        } else null
    }

    override fun heuristic(decision: SplitPilesDecision, state: ClientGameState): DecisionResponse {
        val half = decision.cards.size / 2
        return PilesSplitResponse(
            decisionId = decision.id,
            piles = listOf(decision.cards.take(half), decision.cards.drop(half))
        )
    }
}
