package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Volcanic Salvo (M21 #172) — {10}{R}{R} Sorcery.
 *
 * "This spell costs {X} less to cast, where X is the total power of creatures you control."
 *
 * The two rulings worth pinning: X counts creatures you *control* (a stolen creature counts for
 * the thief, not its owner), and a creature with negative power subtracts from the total — only
 * the total is floored at 0.
 */
class VolcanicSalvoScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.salvoGeneric(playerNumber: Int): Int =
            CostCalculator(cardRegistry, predicateEvaluator = services.predicateEvaluator).calculateEffectiveCost(
                state,
                cardRegistry.requireCard("Volcanic Salvo"),
                if (playerNumber == 1) player1Id else player2Id,
                fromZone = Zone.HAND,
            ).genericAmount

        test("a creature with negative power subtracts from the total") {
            // Player 2's Waker of Waves gives Player 1's creatures -1/-0:
            // Grizzly Bears 2/2 -> 1/2, Wall of Wood 0/3 -> -1/3, total 0.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Wall of Wood")
                .withCardOnBattlefield(2, "Waker of Waves")
                .build()

            withClue("1 + (-1) = 0, so the cost stays {10}{R}{R}") {
                game.salvoGeneric(1) shouldBe 10
            }
        }

        test("a stolen creature counts for its controller, not its owner") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Act of Treason")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("before the steal, the Bears discount Player 2's Salvo") {
                game.salvoGeneric(1) shouldBe 10
                game.salvoGeneric(2) shouldBe 8
            }

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Act of Treason", bears).error shouldBe null
            game.resolveStack()

            withClue("after Act of Treason, the Bears discount Player 1's Salvo instead") {
                game.salvoGeneric(1) shouldBe 8
                game.salvoGeneric(2) shouldBe 10
            }
        }
    }
}
