package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Shocker, Unshakable (SPM #89) — {4}{R}{R} Legendary Creature —
 * Human Rogue Villain, 5/5.
 *
 *   During your turn, Shocker has first strike.
 *   Vibro-Shock Gauntlets — When Shocker enters, he deals 2 damage to target creature and
 *   2 damage to that creature's controller.
 *
 * Exercises the time-restricted static first strike (on your turn vs. an opponent's turn) and
 * the ETB dealing 2 to the target creature plus 2 to that creature's controller.
 */
class ShockerUnshakableScenarioTest : ScenarioTestBase() {

    init {
        context("Shocker, Unshakable") {

            test("has first strike during its controller's turn but not during an opponent's turn") {
                val onTurn = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shocker, Unshakable")
                    .withActivePlayer(1)
                    .build()

                val shockerOnTurn = onTurn.findPermanent("Shocker, Unshakable")!!
                withClue("5/5 with first strike on your turn") {
                    onTurn.state.projectedState.getPower(shockerOnTurn) shouldBe 5
                    onTurn.state.projectedState.getToughness(shockerOnTurn) shouldBe 5
                    onTurn.state.projectedState.hasKeyword(shockerOnTurn, Keyword.FIRST_STRIKE) shouldBe true
                }

                val offTurn = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shocker, Unshakable")
                    .withActivePlayer(2)
                    .build()

                val shockerOffTurn = offTurn.findPermanent("Shocker, Unshakable")!!
                withClue("no first strike during an opponent's turn") {
                    offTurn.state.projectedState.hasKeyword(shockerOffTurn, Keyword.FIRST_STRIKE) shouldBe false
                }
            }

            test("ETB deals 2 damage to target creature and 2 damage to that creature's controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shocker, Unshakable")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 opponent creature
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val opponentLifeBefore = game.getLifeTotal(2)

                game.castSpell(1, "Shocker, Unshakable").error shouldBe null
                game.resolveStack() // Shocker enters → Vibro-Shock Gauntlets asks for a target

                val result = game.selectTargets(listOf(bears))
                withClue("Targeting the opponent's Grizzly Bears is legal: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("2 damage to a 2/2 kills Grizzly Bears") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("2 damage to that creature's controller (Player2)") {
                    game.getLifeTotal(2) shouldBe opponentLifeBefore - 2
                }
                withClue("Shocker is on the battlefield") {
                    game.isOnBattlefield("Shocker, Unshakable") shouldBe true
                }
            }
        }
    }
}
