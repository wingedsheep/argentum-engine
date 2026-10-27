package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Angel of Destiny (ZNR #2) — {3}{W}{W} 2/6 flying, double strike.
 *
 * "Whenever a creature you control deals combat damage to a player, you and that player each gain
 * that much life. At the beginning of your end step, if you have at least 15 life more than your
 * starting life total, each player this creature attacked this turn loses the game."
 */
class AngelOfDestinyScenarioTest : ScenarioTestBase() {

    private fun angelAttacks(attackWith: Map<String, Int>, startingLife: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Angel of Destiny", summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withLifeTotal(1, startingLife)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()
        .also { it.declareAttackers(attackWith).error shouldBe null }

    init {
        test("each combat damage event makes you and the damaged player each gain that much life") {
            val game = angelAttacks(mapOf("Angel of Destiny" to 2, "Grizzly Bears" to 2), startingLife = 20)
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            // First strike: Angel 2 → P2 18, then both gain 2 (P1 22, P2 20).
            // Regular: Angel 2 + Bears 2 → P2 16, two triggers of 2 each (P1 26, P2 20).
            withClue("you gain the damage of every hit") { game.getLifeTotal(1) shouldBe 26 }
            withClue("the damaged player gains back what they were dealt") { game.getLifeTotal(2) shouldBe 20 }
        }

        test("at your end step with 15+ life over your starting total, the player it attacked loses") {
            val game = angelAttacks(mapOf("Angel of Destiny" to 2), startingLife = 33)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("33 + 2 + 2 = 37 ≥ 20 + 15") { game.getLifeTotal(1) shouldBe 37 }
            game.state.getEntity(game.player2Id)?.has<PlayerLostComponent>() shouldBe true
            game.state.gameOver shouldBe true
        }

        test("nobody loses when the Angel itself didn't attack") {
            val game = angelAttacks(mapOf("Grizzly Bears" to 2), startingLife = 40)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.state.getEntity(game.player2Id)?.has<PlayerLostComponent>() shouldBe false
            game.state.gameOver shouldBe false
        }

        test("nobody loses below the life threshold") {
            val game = angelAttacks(mapOf("Angel of Destiny" to 2), startingLife = 20)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 24
            game.state.gameOver shouldBe false
        }
    }
}
