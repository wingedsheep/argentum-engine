package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class ChooseColorHandler : AiDecisionHandler<ChooseColorDecision> {
    override val decisionType: KClass<ChooseColorDecision> = ChooseColorDecision::class

    override fun autoResolve(decision: ChooseColorDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: ChooseColorDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Choose a color:")
        val colors = decision.availableColors.toList()
        for ((j, color) in colors.withIndex()) {
            sb.appendLine("  [${GameStateFormatter.actionLetter(j)}] ${color.name}")
        }
    }

    override fun parse(
        response: String,
        decision: ChooseColorDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val colors = decision.availableColors.toList()
        val index = parser.parseActionChoice(response, colors.size - 1) ?: return null
        return ColorChosenResponse(decisionId = decision.id, color = colors[index])
    }

}
