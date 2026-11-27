package com.wingedsheep.ai.engine

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.doubles.shouldBeLessThan

/**
 * A counterspell whose target must be the turn's Nth spell (Second Guess: "counter target spell
 * that's the second spell cast this turn", `StatePredicate.WasCast(nthSpellThisTurn = 2)`) answers
 * one spell a turn at most, so the effect heuristic prices it below an unrestricted two-mana counter.
 */
class NthSpellCounterRatingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("Second Guess's heuristic rating sits below Counterspell's") {
            LimitedCardRater.heuristicRate(card("Second Guess")) shouldBeLessThan
                LimitedCardRater.heuristicRate(card("Counterspell"))
        }
    }
}
