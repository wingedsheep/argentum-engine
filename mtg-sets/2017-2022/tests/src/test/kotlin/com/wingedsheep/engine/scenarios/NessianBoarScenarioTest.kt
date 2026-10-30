package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nessian Boar (THB) — "All creatures able to block this creature do so. Whenever this creature
 * becomes blocked by a creature, that creature's controller draws a card."
 *
 * Covers the Lure requirement binding only untapped creatures (a tapped one is exempt, per the
 * first ruling) and the draw firing once per blocker for the blocker's controller.
 */
class NessianBoarScenarioTest : ScenarioTestBase() {

    private fun attackingBoar(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Nessian Boar")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Llanowar Elves", tapped = true)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()

        game.declareAttackers(mapOf("Nessian Boar" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        test("every untapped creature must block, and each blocker's controller draws a card") {
            val game = attackingBoar()

            withClue("leaving an able creature out of the block is illegal") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Nessian Boar"))).error shouldNotBe null
            }

            val handBefore = game.handSize(2)
            game.declareBlockers(
                mapOf("Grizzly Bears" to listOf("Nessian Boar"), "Hill Giant" to listOf("Nessian Boar"))
            ).error shouldBe null

            var guard = 0
            game.resolveStack()
            while (game.hasPendingDecision() && guard++ < 10) {
                val decision = game.getPendingDecision() as OrderObjectsDecision
                game.submitDecision(OrderedResponse(decision.id, decision.objects))
                game.resolveStack()
            }

            withClue("one draw per blocking creature, for the defending player") {
                game.handSize(2) shouldBe handBefore + 2
                game.handSize(1) shouldBe 0
            }
        }
    }
}
