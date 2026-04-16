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

}
