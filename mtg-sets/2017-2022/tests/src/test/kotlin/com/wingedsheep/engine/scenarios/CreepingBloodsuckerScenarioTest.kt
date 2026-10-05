package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Creeping Bloodsucker (J22 #21) — {1}{B} Creature — Vampire, 1/2.
 *
 * "At the beginning of your upkeep, this creature deals 1 damage to each opponent. You gain life
 * equal to the damage dealt this way."
 *
 * The life gain reads the creature's actual damage tally (before/after delta), so this proves the
 * delta read works off a permanent source inside a triggered ability, and that only your own
 * upkeep fires it.
 */
class CreepingBloodsuckerScenarioTest : ScenarioTestBase() {

    init {
        context("Creeping Bloodsucker") {

            test("your upkeep: 1 damage to the opponent and you gain 1") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Creeping Bloodsucker")
                    // Start on Bob's turn so the next upkeep reached is Alice's.
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Bob was dealt 1 damage and Alice gained exactly that much") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
            }

            test("the opponent's upkeep does not trigger it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Creeping Bloodsucker")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("it's Bob's upkeep, so no damage and no life gain") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.getLifeTotal(1) shouldBe 20
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
