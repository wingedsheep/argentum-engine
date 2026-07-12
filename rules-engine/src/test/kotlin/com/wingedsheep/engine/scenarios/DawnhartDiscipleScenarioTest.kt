package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Dawnhart Disciple (VOW #196) — {1}{G} Creature — Human Warlock, 2/2.
 *
 *   Whenever another Human you control enters, this creature gets +1/+1 until end of turn.
 *
 * Exercises the "another Human enters" trigger: casting a second Human creature you control
 * pumps Dawnhart Disciple +1/+1 until end of turn.
 */
class DawnhartDiscipleScenarioTest : ScenarioTestBase() {

    init {
        context("Dawnhart Disciple — another Human entering pumps it") {

            test("another Human you control entering gives Dawnhart Disciple +1/+1 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dawnhart Disciple", summoningSickness = false)
                    .withCardInHand(1, "Glory Seeker") // another Human you control
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val disciple = game.findPermanent("Dawnhart Disciple")!!

                withClue("Dawnhart Disciple starts at its base 2/2") {
                    game.state.projectedState.getPower(disciple) shouldBe 2
                    game.state.projectedState.getToughness(disciple) shouldBe 2
                }

                game.castSpell(1, "Glory Seeker").error shouldBe null
                game.resolveStack() // Glory Seeker enters → triggers Dawnhart Disciple's pump

                withClue("Dawnhart Disciple gets +1/+1 (becomes 3/3)") {
                    game.state.projectedState.getPower(disciple) shouldBe 3
                    game.state.projectedState.getToughness(disciple) shouldBe 3
                }
            }
        }
    }
}
