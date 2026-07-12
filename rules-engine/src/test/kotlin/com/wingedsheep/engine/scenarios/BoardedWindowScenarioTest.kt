package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Boarded Window (VOW #253) — {3} Artifact.
 *
 *   Creatures attacking you get -1/-0.
 *   At the beginning of each end step, if you were dealt 4 or more damage this turn, exile this
 *   artifact.
 *
 * Exercises the static -1/-0 debuff on opponent's attackers and the end-step self-exile once the
 * controller has taken 4+ damage this turn.
 */
class BoardedWindowScenarioTest : ScenarioTestBase() {

    init {
        context("Boarded Window") {

            test("gives creatures attacking the controller -1/-0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boarded Window")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false) // 3/3
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null

                withClue("Hill Giant attacking the Boarded Window controller becomes 2/3") {
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }

            test("does not affect creatures that are not attacking the controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boarded Window")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                withClue("Hill Giant is unaffected while not attacking") {
                    game.state.projectedState.getPower(giant) shouldBe 3
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }

            test("exiles itself at the end step once the controller took 4+ damage this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boarded Window")
                    .withCardInHand(1, "Stoke the Flames")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // The controller deals 4 damage to themself to cross the threshold this turn.
                game.castSpellTargetingPlayer(1, "Stoke the Flames", 1).error shouldBe null
                game.resolveStack()

                withClue("the controller took 4 damage this turn") {
                    game.getLifeTotal(1) shouldBe 16
                }
                withClue("Boarded Window is still on the battlefield before the end step") {
                    game.isOnBattlefield("Boarded Window") shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Boarded Window exiled itself at the end step") {
                    game.isOnBattlefield("Boarded Window") shouldBe false
                    game.isInExile(1, "Boarded Window") shouldBe true
                }
            }

            test("stays on the battlefield at the end step with less than 4 damage taken") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Boarded Window")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("no damage taken this turn -> Boarded Window is not exiled") {
                    game.isOnBattlefield("Boarded Window") shouldBe true
                }
            }
        }
    }
}
