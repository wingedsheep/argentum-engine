package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Attercop (HOB #116) — {1}{G} Creature — Spider 2/1.
 *
 * "Reach, deathtouch
 *  Landfall — Whenever a land you control enters, this creature gets +1/+1 until end of turn."
 *
 * Covers the printed evasion keywords, the landfall trigger's stat change, and the
 * "you control" scoping that must keep an opponent's land drop from pumping it.
 */
class AttercopScenarioTest : ScenarioTestBase() {

    init {
        context("Attercop") {

            test("it is a 2/1 with reach and deathtouch") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Attercop")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val attercop = game.findPermanent("Attercop")!!
                val projected = game.state.projectedState
                withClue("printed characteristics") {
                    projected.getPower(attercop) shouldBe 2
                    projected.getToughness(attercop) shouldBe 1
                    projected.hasKeyword(attercop, Keyword.REACH) shouldBe true
                    projected.hasKeyword(attercop, Keyword.DEATHTOUCH) shouldBe true
                }
            }

            test("a land you control entering pumps it to 3/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Attercop")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val attercop = game.findPermanent("Attercop")!!
                game.state.projectedState.getPower(attercop) shouldBe 2

                game.execute(
                    PlayLand(game.player1Id, game.findCardsInHand(1, "Forest").single())
                ).error shouldBe null
                game.resolveStack()

                withClue("landfall gave +1/+1 until end of turn") {
                    game.state.projectedState.getPower(attercop) shouldBe 3
                    game.state.projectedState.getToughness(attercop) shouldBe 2
                }
            }

            test("an opponent's land entering does not pump it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Attercop")
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val attercop = game.findPermanent("Attercop")!!

                game.execute(
                    PlayLand(game.player2Id, game.findCardsInHand(2, "Forest").single())
                ).error shouldBe null
                game.resolveStack()

                withClue("the trigger is scoped to lands *you* control") {
                    game.state.projectedState.getPower(attercop) shouldBe 2
                    game.state.projectedState.getToughness(attercop) shouldBe 1
                }
            }
        }
    }
}
