package com.wingedsheep.engine.support

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse

/**
 * The only answer an unpayable payment question allows, or null when [decision] is not one.
 *
 * Whether a payment question is asked is decided from public mana only: a player with cards in
 * hand might hold a Spirit Guide, so they are asked even when they can't pay, and the question is
 * shaped so refusing is the only answer — a yes/no with `yesAvailable = false`, a mana window
 * with `payAvailable = false`, a number chooser pinned to 0, a selection capped at nothing
 * (`MaxAffordablePayment`), or a choose-one left with one option. Production auto-answers those
 * shapes (`ForcedDecisionAutoAnswer`, `TrivialDecisions`); a test drives them with
 * [answerForcedPayments].
 */
fun forcedPaymentAnswer(decision: PendingDecision?): DecisionResponse? = when (decision) {
    is YesNoDecision -> if (!decision.yesAvailable) YesNoResponse(decision.id, false) else null
    is SelectManaSourcesDecision ->
        if (!decision.payAvailable && decision.canDecline) ManaSourcesSelectedResponse(decision.id, declined = true) else null
    is ChooseNumberDecision ->
        if (decision.minValue == 0 && decision.maxValue == 0) NumberChosenResponse(decision.id, 0) else null
    is SelectCardsDecision -> if (decision.maxSelections == 0) CardsSelectedResponse(decision.id, emptyList()) else null
    is ChooseOptionDecision -> if (decision.options.size == 1) OptionChosenResponse(decision.id, 0) else null
    else -> null
}

/**
 * Answers every forced payment question in a row (see [forcedPaymentAnswer]), failing when none is
 * pending, so a test states that the player was asked and could only refuse.
 */
fun GameTestDriver.answerForcedPayments(): ExecutionResult {
    var answer = forcedPaymentAnswer(pendingDecision)
        ?: error("Expected a forced payment question, but the pending decision is $pendingDecision")
    var result: ExecutionResult
    do {
        result = submitDecision(pendingDecision!!.playerId, answer)
        answer = forcedPaymentAnswer(pendingDecision) ?: break
    } while (true)
    return result
}

/** [ScenarioTestBase.TestGame] twin of [GameTestDriver.answerForcedPayments]. */
fun ScenarioTestBase.TestGame.answerForcedPayments(): ExecutionResult {
    var answer = forcedPaymentAnswer(getPendingDecision())
        ?: error("Expected a forced payment question, but the pending decision is ${getPendingDecision()}")
    var result: ExecutionResult
    do {
        result = submitDecision(answer)
        answer = forcedPaymentAnswer(getPendingDecision()) ?: break
    } while (true)
    return result
}
