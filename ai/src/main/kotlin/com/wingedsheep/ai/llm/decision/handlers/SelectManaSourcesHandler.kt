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

    override fun autoResolve(decision: SelectManaSourcesDecision): DecisionResponse =
        bestEffortResponse(decision)

    /**
     * Auto-pay only works when the solver actually found a solution. When [autoPaySuggestion] is
     * empty — the cost is payable only by sacrificing a Treasure, or by an ability the solver can't
     * auto-tap at all — submitting `autoPay = true` errors inside the resumer and the engine
     * re-raises the same decision, looping forever. Mirrors the engine AI's `DecisionResponder`.
     */
    private fun bestEffortResponse(decision: SelectManaSourcesDecision): ManaSourcesSelectedResponse {
        if (decision.autoPaySuggestion.isNotEmpty()) {
            return ManaSourcesSelectedResponse(decisionId = decision.id, autoPay = true)
        }
        // Optional payment with no clean solution — don't burn permanents on it.
        if (decision.canDecline) {
            return ManaSourcesSelectedResponse(decisionId = decision.id, declined = true)
        }
        // Mandatory (ward, counter-unless-pays): offer everything and let the resumer sort it out.
        // Sub-cost sources are skipped — the AI can't answer the follow-up tap prompt.
        return ManaSourcesSelectedResponse(
            decisionId = decision.id,
            autoPay = false,
            selectedSources = decision.availableSources
                .filterNot { it.requiresTappingAnotherPermanent }
                .map { it.entityId }
        )
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
    ): DecisionResponse = bestEffortResponse(decision)
}
