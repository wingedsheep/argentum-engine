package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Dream Spoilers. */
class DreamSpoilersScenarioTest : ScenarioTestBase() {

    init {
        context("Dream Spoilers — only on an opponent's turn") {
            test("casting an instant on the opponent's turn shrinks their creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dream Spoilers", summoningSickness = false)
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                // Shock goes at the opponent's face so the only shrink comes from Dream Spoilers.
                game.castSpellTargetingPlayer(1, "Shock", 2)
                if (game.hasPendingDecision()) game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the opponent lost 2 life to Shock") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("3/3 Hill Giant gets -1/-1 from the Dream Spoilers trigger") {
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 2
                }
            }

            test("casting a spell on your own turn does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dream Spoilers", summoningSickness = false)
                    .withCardInHand(1, "Giant Growth")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Giant Growth", bears)
                game.resolveStack()

                withClue("no pending target decision — the trigger never fired on our own turn") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("the opponent's Hill Giant is untouched") {
                    game.state.projectedState.getPower(giant) shouldBe 3
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }
        }
    }
}
