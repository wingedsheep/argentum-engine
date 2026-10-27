package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Canyon Jerboa (ZNR #7) — {2}{W} Creature — Mouse, 1/2.
 *
 *   Landfall — Whenever a land you control enters, creatures you control get +1/+1 until end of turn.
 */
class CanyonJerboaScenarioTest : ScenarioTestBase() {

    init {
        context("Canyon Jerboa's landfall trigger") {

            test("a land you control entering pumps each creature you control, not the opponent's, until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Canyon Jerboa")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jerboa = game.findPermanent("Canyon Jerboa")!!
                val myBears = game.findPermanent("Grizzly Bears")!!
                val theirGiant = game.findPermanent("Hill Giant")!!

                val plains = game.findCardsInHand(1, "Plains").single()
                game.execute(PlayLand(game.player1Id, plains)).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("the Jerboa itself is a creature you control: 1/2 -> 2/3") {
                    projected.getPower(jerboa) shouldBe 2
                    projected.getToughness(jerboa) shouldBe 3
                }
                withClue("your Bears get +1/+1") {
                    projected.getPower(myBears) shouldBe 3
                    projected.getToughness(myBears) shouldBe 3
                }
                withClue("the opponent's Hill Giant is untouched") {
                    projected.getPower(theirGiant) shouldBe 3
                    projected.getToughness(theirGiant) shouldBe 3
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the bonus lasts only until end of turn") {
                    game.state.projectedState.getPower(myBears) shouldBe 2
                    game.state.projectedState.getPower(jerboa) shouldBe 1
                }
            }
        }
    }
}
