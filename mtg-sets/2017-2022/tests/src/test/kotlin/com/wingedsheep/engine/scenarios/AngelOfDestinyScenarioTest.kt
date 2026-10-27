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
        .withCardInHand(1, "Murder")
        .withLandsOnBattlefield(1, "Swamp", 3)
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

        test("at your end step with exactly 15 life over your starting total, the player it attacked loses") {
            val game = angelAttacks(mapOf("Angel of Destiny" to 2), startingLife = 31)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("31 + 2 + 2 = 35 = 20 + 15") { game.getLifeTotal(1) shouldBe 35 }
            game.state.getEntity(game.player2Id)?.has<PlayerLostComponent>() shouldBe true
            game.state.gameOver shouldBe true
        }

        test("the attacked player still loses when the Angel is destroyed with its trigger on the stack") {
            val game = angelAttacks(mapOf("Angel of Destiny" to 2), startingLife = 31)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.stack.size shouldBe 1

            game.castSpell(1, "Murder", game.findPermanent("Angel of Destiny")).error shouldBe null
            game.resolveStack()

            withClue("the Angel is gone") { game.isInGraveyard(1, "Angel of Destiny") shouldBe true }
            withClue("the ability uses the Angel's last known attack record (CR 608.2h)") {
                game.state.getEntity(game.player2Id)?.has<PlayerLostComponent>() shouldBe true
            }
        }

        test("nobody loses when the Angel itself didn't attack") {
            val game = angelAttacks(mapOf("Grizzly Bears" to 2), startingLife = 40)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.state.getEntity(game.player2Id)?.has<PlayerLostComponent>() shouldBe false
            game.state.gameOver shouldBe false
        }

        test("nobody loses one life below the threshold") {
            val game = angelAttacks(mapOf("Angel of Destiny" to 2), startingLife = 30)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("30 + 2 + 2 = 34, one short") { game.getLifeTotal(1) shouldBe 34 }
            game.state.gameOver shouldBe false
        }
    }
}
