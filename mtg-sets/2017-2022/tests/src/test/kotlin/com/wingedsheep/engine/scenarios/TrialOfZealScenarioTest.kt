package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Trial of Zeal (AKH #152).
 *
 *   When this enchantment enters, it deals 3 damage to any target.
 *   When a Cartouche you control enters, return this enchantment to its owner's hand.
 */
class TrialOfZealScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveAll() {
        var guard = 0
        resolveStack()
        while (hasPendingDecision() && guard++ < 10) {
            when (val decision = getPendingDecision()!!) {
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
            resolveStack()
        }
    }

    init {
        context("Trial of Zeal") {

            test("enters and deals 3 damage to a target player") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Trial of Zeal")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Trial of Zeal").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 17
                game.isOnBattlefield("Trial of Zeal") shouldBe true
            }

            test("enters and deals 3 damage to a target creature") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Trial of Zeal")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Trial of Zeal").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }

            test("a Cartouche entering under your control returns it to hand") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Trial of Zeal")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Cartouche of Solidarity")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Cartouche of Solidarity", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveAll()

                withClue("Trial of Zeal returns to its owner's hand") {
                    game.isInHand(1, "Trial of Zeal") shouldBe true
                    game.isOnBattlefield("Trial of Zeal") shouldBe false
                }
            }
        }
    }
}
