package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Throne of the God-Pharaoh (AKH #237) — {2} Legendary Artifact.
 *
 *   At the beginning of your end step, each opponent loses life equal to the number of tapped
 *   creatures you control.
 */
class ThroneOfTheGodPharaohScenarioTest : ScenarioTestBase() {
    init {
        context("Throne of the God-Pharaoh") {

            test("opponent loses life equal to your tapped creatures only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Throne of the God-Pharaoh")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = true)
                    .withCardOnBattlefield(1, "Llanowar Elves") // untapped — doesn't count
                    .withCardOnBattlefield(2, "Savannah Lions", tapped = true) // opponent's — doesn't count
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("two tapped creatures you control → opponent loses 2") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
