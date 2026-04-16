package com.wingedsheep.ai.llm.decision.handlers

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.ai.llm.AiResponseParser
import com.wingedsheep.ai.llm.GameStateFormatter
import com.wingedsheep.ai.llm.flattenOracle
import com.wingedsheep.ai.llm.decision.AiDecisionHandler
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.reflect.KClass

class SelectCardsHandler : AiDecisionHandler<SelectCardsDecision> {
    override val decisionType: KClass<SelectCardsDecision> = SelectCardsDecision::class

    override fun autoResolve(decision: SelectCardsDecision): DecisionResponse {
        throw UnsupportedOperationException()
    }

    override fun format(
        sb: StringBuilder,
        decision: SelectCardsDecision,
        state: ClientGameState,
        labels: Map<EntityId, String>
    ) {
        sb.appendLine("Select ${decision.minSelections}-${decision.maxSelections} card(s):")
        for ((j, eid) in decision.options.withIndex()) {
            val card = state.cards[eid]
            val info = decision.cardInfo?.get(eid)
            val name = card?.name ?: info?.name ?: "Unknown"
            val letter = GameStateFormatter.actionLetter(j)
            val details = buildString {
                val cost = card?.manaCost ?: info?.manaCost
                if (!cost.isNullOrBlank()) append(" $cost")
                val type = card?.typeLine ?: info?.typeLine
                if (!type.isNullOrBlank()) append(" — $type")
                if (card?.power != null && card.toughness != null) append(" ${card.power}/${card.toughness}")
                val oracle = card?.oracleText
                if (!oracle.isNullOrBlank()) append(" — \"${oracle.flattenOracle()}\"")
            }
            sb.appendLine("  [$letter] $name$details")
        }
    }

    override fun parse(
        response: String,
        decision: SelectCardsDecision,
        state: ClientGameState,
        parser: AiResponseParser
    ): DecisionResponse? {
        val indices = parser.parseMultipleSelections(response, decision.options.size - 1)
        return if (indices != null) {
            val selected = indices.map { decision.options[it] }
            CardsSelectedResponse(decisionId = decision.id, selectedCards = selected)
        } else null
    }

}
