package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class YesNoHandler : AiDecisionHandler<YesNoDecision> {
    override val decisionType: KClass<YesNoDecision> = YesNoDecision::class

    override fun autoResolve(decision: YesNoDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: YesNoDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("[A] ${decision.yesText}")
        sb.appendLine("[B] ${decision.noText}")
    }

    override fun parse(
        response: String,
        decision: YesNoDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val choice = parser.parseYesNo(response) ?: return null
        return YesNoResponse(decisionId = decision.id, choice = choice)
    }

    override fun heuristic(decision: YesNoDecision, state: ClientGameState): DecisionResponse {
        // Default "yes" for beneficial effects, "no" for harmful ones.
        // Check if the prompt/yesText suggests something harmful to us.
        val promptLower = (decision.prompt + " " + decision.yesText).lowercase()
        val isHarmful = HARMFUL_KEYWORDS.any { promptLower.contains(it) }
        val isBeneficial = BENEFICIAL_KEYWORDS.any { promptLower.contains(it) }
        val choice = when {
            isHarmful && !isBeneficial -> false
            else -> true // Default to yes for neutral/beneficial effects
        }
        return YesNoResponse(decisionId = decision.id, choice = choice)
    }

    companion object {
        private val HARMFUL_KEYWORDS = listOf(
            "sacrifice", "discard", "pay life", "lose life", "exile your",
            "destroy your", "return your", "each player sacrifices",
            "each player discards", "all creatures get -"
        )
        private val BENEFICIAL_KEYWORDS = listOf(
            "draw", "gain life", "create", "put a +1/+1", "search your library",
            "return target", "destroy target", "exile target", "deal damage"
        )
    }
}
