package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Flourishing Defenses — "Whenever a -1/-1 counter is put on a creature, you may
 * create a 1/1 green Elf Warrior creature token."
 *
 * The ruling: it triggers once for each -1/-1 counter, so a creature entering with two -1/-1 counters
 * (Morselhoarder) triggers it twice, and each "you may" is answered on its own.
 */
class FlourishingDefensesScenarioTest : ScenarioTestBase() {

    init {
        context("Flourishing Defenses") {

            test("a creature entering with two -1/-1 counters triggers it twice") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Flourishing Defenses")
                    .withCardInHand(1, "Morselhoarder")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .build()

                val cast = game.castSpell(1, "Morselhoarder")
                withClue("cast: ${cast.error}") { cast.error shouldBe null }

                val answers = ArrayDeque(listOf(true, false))
                var decisions = 0
                game.resolveStack()
                while (game.hasPendingDecision()) {
                    decisions++
                    game.answerYesNo(answers.removeFirst())
                    game.resolveStack()
                }

                withClue("one may-decision per counter") { decisions shouldBe 2 }
                withClue("accepting one of the two triggers makes one Elf Warrior") {
                    game.findPermanents("Elf Warrior Token").size shouldBe 1
                }
            }
        }
    }
}
