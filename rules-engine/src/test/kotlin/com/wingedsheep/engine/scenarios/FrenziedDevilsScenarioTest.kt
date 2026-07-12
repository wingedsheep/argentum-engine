package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Frenzied Devils (VOW #159) — {4}{R} Creature — Devil, 3/3.
 *
 *   Haste
 *   Whenever you cast a noncreature spell, this creature gets +2/+2 until end of turn.
 *
 * Exercises the "you cast a noncreature spell" trigger: casting an instant pumps Frenzied Devils
 * +2/+2 until end of turn. Also confirms the printed Haste keyword is present.
 */
class FrenziedDevilsScenarioTest : ScenarioTestBase() {

    init {
        context("Frenzied Devils — casting a noncreature spell pumps it") {

            test("has Haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Frenzied Devils", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devils = game.findPermanent("Frenzied Devils")!!
                withClue("Frenzied Devils has Haste") {
                    game.state.projectedState.hasKeyword(devils, Keyword.HASTE) shouldBe true
                }
            }

            test("casting a noncreature spell gives Frenzied Devils +2/+2 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Frenzied Devils", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val devils = game.findPermanent("Frenzied Devils")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Frenzied Devils starts at its base 3/3") {
                    game.state.projectedState.getPower(devils) shouldBe 3
                    game.state.projectedState.getToughness(devils) shouldBe 3
                }

                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack() // trigger + Lightning Bolt both resolve

                withClue("Frenzied Devils gets +2/+2 (becomes 5/5)") {
                    game.state.projectedState.getPower(devils) shouldBe 5
                    game.state.projectedState.getToughness(devils) shouldBe 5
                }
            }
        }
    }
}
