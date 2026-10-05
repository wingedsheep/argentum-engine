package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Shaper Apprentice (XLN #75) — {1}{U} Creature — Merfolk Wizard 2/1
 *
 * This creature has flying as long as you control another Merfolk.
 *
 * Proves the clause reads *another* Merfolk (the Apprentice is not enough on its own), that it is
 * *you control* (an opponent's Merfolk does not count), and that two Apprentices light each other up.
 */
class ShaperApprenticeScenarioTest : ScenarioTestBase() {

    init {
        context("Shaper Apprentice") {

            test("has flying while you control another Merfolk") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shaper Apprentice")
                    .withCardOnBattlefield(1, "River Sneak")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val apprentice = game.findPermanent("Shaper Apprentice")!!
                withClue("River Sneak is another Merfolk you control") {
                    game.state.projectedState.hasKeyword(apprentice, Keyword.FLYING) shouldBe true
                }
            }

            test("no flying alone, and an opponent's Merfolk does not count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shaper Apprentice")
                    .withCardOnBattlefield(2, "River Sneak")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val apprentice = game.findPermanent("Shaper Apprentice")!!
                withClue("The Apprentice is not another Merfolk to itself, and the opponent's Sneak isn't yours") {
                    game.state.projectedState.hasKeyword(apprentice, Keyword.FLYING) shouldBe false
                }
            }

            test("two Apprentices each count the other") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shaper Apprentice")
                    .withCardOnBattlefield(1, "Shaper Apprentice")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val apprentices = game.findAllPermanents("Shaper Apprentice")
                apprentices.size shouldBe 2
                apprentices.forEach { id ->
                    game.state.projectedState.hasKeyword(id, Keyword.FLYING) shouldBe true
                }
            }
        }
    }
}
