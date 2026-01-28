package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Natural Spring - a sorcery that gains life for its controller
 * without requiring targeting (uses "You gain 8 life" rather than "Target player gains 8 life").
 */
class NaturalSpringScenarioTest : ScenarioTestBase() {

    init {
        context("Natural Spring life gain") {

            test("casting Natural Spring gains 8 life for controller without targeting") {
                // Natural Spring costs {3}{G}{G} and says "You gain 8 life"
                // It should NOT require any targeting - it automatically targets the controller
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Natural Spring")
                    .withLandsOnBattlefield(1, "Forest", 5) // 5 green mana for {3}{G}{G}
                    .withLifeTotal(1, 10) // Start at 10 life
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Player 1 should start at 10 life") {
                    game.getLifeTotal(1) shouldBe 10
                }

                // Cast Natural Spring without specifying a target (no targeting required)
                val castResult = game.castSpell(1, "Natural Spring")
                withClue("Casting Natural Spring should succeed without targeting: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify life was gained
                withClue("Player 1 should have gained 8 life (10 -> 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }

                // Verify opponent's life is unchanged
                withClue("Player 2's life should be unchanged at 20") {
                    game.getLifeTotal(2) shouldBe 20
                }

                // Verify the spell went to graveyard
                withClue("Natural Spring should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Natural Spring") shouldBe true
                }
            }

            test("Natural Spring does not create a pending decision for targeting") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Natural Spring")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Natural Spring
                val castResult = game.castSpell(1, "Natural Spring")
                withClue("Casting should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Verify no pending decision was created (no targeting required)
                withClue("No pending decision should exist - spell doesn't require targeting") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
