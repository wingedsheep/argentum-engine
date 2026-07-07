package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Get Lost (LCI #14) — {1}{W} Instant, Rare.
 *
 * "Destroy target creature, enchantment, or planeswalker.
 *  Its controller creates two Map tokens."
 *
 * Proves:
 *  1. Destroys a creature — target's controller (player 2) gets two Map tokens; caster gets none.
 *  2. Destroys an enchantment — target's controller gets two Map tokens.
 */
class GetLostScenarioTest : ScenarioTestBase() {

    /** Count Map tokens on a specific player's battlefield side. */
    private fun countMaps(game: TestGame, playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getBattlefield(playerId).count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Map"
        }
    }

    init {
        context("Get Lost destroys target creature, enchantment, or planeswalker") {

            test("destroys an opponent's creature and the opponent gets two Map tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Get Lost")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Centaur Courser")!!

                val result = game.castSpell(1, "Get Lost", targetId = creature)
                withClue("Casting Get Lost should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Centaur Courser should be destroyed") {
                    game.findPermanent("Centaur Courser") shouldBe null
                }
                withClue("Target's controller (player 2) should have exactly two Map tokens") {
                    countMaps(game, 2) shouldBe 2
                }
                withClue("Caster (player 1) should have no Map tokens") {
                    countMaps(game, 1) shouldBe 0
                }
            }

            test("destroys an opponent's enchantment and the opponent gets two Map tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Get Lost")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Test Enchantment")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val enchantment = game.findPermanent("Test Enchantment")!!

                val result = game.castSpell(1, "Get Lost", targetId = enchantment)
                withClue("Casting Get Lost on an enchantment should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Test Enchantment should be destroyed") {
                    game.findPermanent("Test Enchantment") shouldBe null
                }
                withClue("Target's controller (player 2) should have exactly two Map tokens") {
                    countMaps(game, 2) shouldBe 2
                }
            }
        }
    }
}
