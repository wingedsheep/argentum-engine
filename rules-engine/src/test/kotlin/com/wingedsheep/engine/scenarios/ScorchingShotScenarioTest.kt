package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scorching Shot (OTJ #145) — {R}{R} Sorcery.
 *
 *   "Scorching Shot deals 5 damage to target creature."
 *
 * Straight burn-to-creature spell. Verifies the 5 damage lands on the chosen creature: a
 * toughness-5-or-less creature dies, a toughness-6 creature survives.
 */
class ScorchingShotScenarioTest : ScenarioTestBase() {

    init {
        context("Scorching Shot") {

            test("deals 5 damage, killing a 4/4 creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scorching Shot")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val result = game.castSpell(1, "Scorching Shot", giant)
                withClue("Casting Scorching Shot should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("5 damage kills the 3/3 Hill Giant") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("5 damage is not lethal to a creature with toughness 6") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Scorching Shot")
                    .withCardOnBattlefield(2, "Redwood Treefolk") // 3/6
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val treefolk = game.findPermanent("Redwood Treefolk")!!
                val result = game.castSpell(1, "Scorching Shot", treefolk)
                withClue("Casting Scorching Shot should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("5 damage does not kill a creature with toughness greater than 5") {
                    game.isOnBattlefield("Redwood Treefolk") shouldBe true
                }
            }
        }
    }
}
