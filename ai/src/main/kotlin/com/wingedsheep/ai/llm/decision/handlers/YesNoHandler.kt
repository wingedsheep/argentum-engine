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

    /** "Yes" is impossible, so "no" is the only answer: nothing to ask the model. */
    override fun canAutoResolve(decision: YesNoDecision): Boolean = !decision.yesAvailable

    override fun autoResolve(decision: YesNoDecision): DecisionResponse =
        YesNoResponse(decisionId = decision.id, choice = false)

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
