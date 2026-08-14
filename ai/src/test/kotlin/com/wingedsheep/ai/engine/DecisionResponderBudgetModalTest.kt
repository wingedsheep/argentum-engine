package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.core.BudgetModeOption
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.por.PortalSet
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * The greedy budget-modal responder repeats the cheapest mode until the budget is
 * exhausted. A zero-cost mode never consumes budget, so the loop used to spin
 * forever — rare in production, but a rollout engine would hit it constantly.
 */
class DecisionResponderBudgetModalTest : FunSpec({
    timeout = 30_000L  // a regression here hangs rather than fails

    val allCards = PortalSet.cards + PortalSet.basicLands

    fun responderAndState(): Pair<DecisionResponder, com.wingedsheep.engine.state.GameState> {
        val registry = CardRegistry().apply { register(allCards) }
        val driver = GameTestDriver().apply {
            registerCards(allCards)
            initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        }
        val simulator = GameSimulator(registry)
        return DecisionResponder(simulator, AIPlayer.defaultEvaluator()) to driver.state
    }

    fun decisionWith(budget: Int, vararg costs: Int) = BudgetModalDecision(
        id = "budget-modal-test",
        playerId = com.wingedsheep.sdk.model.EntityId("p1"),
        prompt = "Choose modes",
        context = DecisionContext(),
        budget = budget,
        modes = costs.mapIndexed { i, cost -> BudgetModeOption(cost, "mode $i") }
    )

    test("a zero-cost mode is taken once instead of looping forever") {
        val (responder, state) = responderAndState()
        val decision = decisionWith(budget = 3, 0, 2)

        val response = responder.respond(state, decision, decision.playerId) as BudgetModalResponse

        // Mode 0 is free (taken once); mode 1 costs 2 and fits once in the budget of 3.
        response.selectedModeIndices shouldContainExactly listOf(0, 1)
    }

    test("positive-cost modes still repeat until the budget is exhausted") {
        val (responder, state) = responderAndState()
        val decision = decisionWith(budget = 5, 2, 3)

        val response = responder.respond(state, decision, decision.playerId) as BudgetModalResponse

        // Greedy takes the cheapest twice (4 of 5), then nothing else fits.
        response.selectedModeIndices shouldContainExactly listOf(0, 0)
    }

    test("no mode is selected when nothing is affordable") {
        val (responder, state) = responderAndState()
        val decision = decisionWith(budget = 1, 2, 3)

        val response = responder.respond(state, decision, decision.playerId) as BudgetModalResponse

        response.selectedModeIndices.size shouldBe 0
    }
})
