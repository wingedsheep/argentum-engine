package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Storm World (LEG) — {R} World Enchantment: "At the beginning of each player's upkeep, this
 * enchantment deals X damage to that player, where X is 4 minus the number of cards in their hand."
 */
class StormWorldScenarioTest : ScenarioTestBase() {

    private fun upkeepOf(activePlayer: Int, handSize: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Storm World")
            .withActivePlayer(activePlayer)
            .inPhase(Phase.BEGINNING, Step.UNTAP)
        repeat(handSize) { builder.withCardInHand(activePlayer, "Mountain") }
        val game = builder.build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        context("Storm World") {
            test("deals 4 minus hand size to the opponent on their upkeep") {
                val game = upkeepOf(activePlayer = 2, handSize = 1)
                game.getLifeTotal(2) shouldBe 17
                game.getLifeTotal(1) shouldBe 20
            }

            test("also hits its controller on their own upkeep") {
                val game = upkeepOf(activePlayer = 1, handSize = 0)
                game.getLifeTotal(1) shouldBe 16
            }

            test("deals no damage to a player holding four or more cards") {
                val game = upkeepOf(activePlayer = 2, handSize = 6)
                game.getLifeTotal(2) shouldBe 20
            }

            test("the world rule: casting another world enchantment puts Storm World into the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Storm World")
                    .withCardInHand(1, "Gravity Sphere")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gravity Sphere")
                game.resolveStack()

                game.isOnBattlefield("Gravity Sphere") shouldBe true
                game.isOnBattlefield("Storm World") shouldBe false
                game.isInGraveyard(2, "Storm World") shouldBe true
            }
        }
    }
}
