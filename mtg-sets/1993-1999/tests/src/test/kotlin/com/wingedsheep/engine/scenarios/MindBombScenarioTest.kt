package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mind Bomb — "Each player may discard up to three cards. Mind Bomb deals damage
 * to each player equal to 3 minus the number of cards they discarded this way."
 *
 * Each player's damage comes from *their own* discard, so the case that matters is an asymmetric
 * one: one player pitches cards and the other doesn't, and the two life totals must move by
 * different amounts. A version that counted one shared pile — or leaked one player's count into the
 * next iteration — would move them together.
 */
class MindBombScenarioTest : ScenarioTestBase() {

    init {
        context("Mind Bomb") {

            test("nobody discards: everyone takes the full 3") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mind Bomb")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mind Bomb").error shouldBe null
                game.resolveStack()
                var guard = 0
                while (game.hasPendingDecision() && guard++ < 20) {
                    game.skipSelection()
                    game.resolveStack()
                }

                withClue("3 minus zero discarded, on both sides") {
                    game.getLifeTotal(1) shouldBe 17
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("the count is per player, not shared") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mind Bomb")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Craw Wurm")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mind Bomb").error shouldBe null
                game.resolveStack()

                var guard = 0
                var first = true
                while (game.hasPendingDecision() && guard++ < 20) {
                    val decision = game.getPendingDecision()
                    if (first && decision is SelectCardsDecision) {
                        // I pitch two; the opponent will pitch none.
                        game.selectCards(decision.options.take(2))
                        first = false
                    } else {
                        game.skipSelection()
                    }
                    game.resolveStack()
                }

                withClue("I discarded two, so 3 - 2 = 1 damage to me") {
                    game.getLifeTotal(1) shouldBe 19
                }
                withClue("the opponent discarded none, so they take the full 3") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }
    }
}
