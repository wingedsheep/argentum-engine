package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fateful Absence (MID #18) — destroy target creature or planeswalker; its controller investigates.
 *
 * The Clue goes to the destroyed permanent's controller, resolved after it has already left the
 * battlefield (last-known information), not to the caster.
 */
class FatefulAbsenceScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Fateful Absence")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Jace Beleren")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.cluesOf(playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return findPermanents("Clue").count { state.projectedState.getController(it) == playerId }
    }

    init {
        test("destroying an opponent's creature gives that opponent the Clue") {
            val game = game()
            game.castSpell(1, "Fateful Absence", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.cluesOf(2) shouldBe 1
            game.cluesOf(1) shouldBe 0
        }

        test("destroying your own creature gives you the Clue") {
            val game = game()
            game.castSpell(1, "Fateful Absence", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.cluesOf(1) shouldBe 1
            game.cluesOf(2) shouldBe 0
        }

        test("can destroy a planeswalker, whose controller investigates") {
            val game = game()
            game.castSpell(1, "Fateful Absence", game.findPermanent("Jace Beleren")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Jace Beleren") shouldBe true
            game.cluesOf(2) shouldBe 1
        }
    }
}
