package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class SelectManaSourcesHandler : AiDecisionHandler<SelectManaSourcesDecision> {
    override val decisionType: KClass<SelectManaSourcesDecision> = SelectManaSourcesDecision::class

    override fun canAutoResolve(decision: SelectManaSourcesDecision): Boolean = true

    override fun autoResolve(decision: SelectManaSourcesDecision): DecisionResponse {
        return ManaSourcesSelectedResponse(decisionId = decision.id, autoPay = true)
    }

    override fun format(
        sb: StringBuilder,
        decision: SelectManaSourcesDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Select mana sources to pay ${decision.requiredCost}:")
        sb.appendLine("Reply: [A] Auto Pay")
    }

    override fun parse(
        response: String,
        decision: SelectManaSourcesDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse {
        return ManaSourcesSelectedResponse(decisionId = decision.id, autoPay = true)
    }

}
