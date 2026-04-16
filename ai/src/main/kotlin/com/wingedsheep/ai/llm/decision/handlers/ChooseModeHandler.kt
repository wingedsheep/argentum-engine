package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ModesChosenResponse
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class ChooseModeHandler : AiDecisionHandler<ChooseModeDecision> {
    override val decisionType: KClass<ChooseModeDecision> = ChooseModeDecision::class

    override fun autoResolve(decision: ChooseModeDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: ChooseModeDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Choose ${decision.minModes}-${decision.maxModes} mode(s):")
        for (mode in decision.modes) {
            val letter = GameStateFormatter.actionLetter(mode.index)
            val available = if (mode.available) "" else " (unavailable)"
            sb.appendLine("  [$letter] ${mode.text}$available")
        }
    }

    override fun parse(
        response: String,
        decision: ChooseModeDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val index = parser.parseActionChoice(response, decision.modes.size - 1) ?: return null
        return ModesChosenResponse(decisionId = decision.id, selectedModes = listOf(decision.modes[index].index))
    }

}
