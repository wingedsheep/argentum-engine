package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Quarry Colossus (JOU #22): "When this creature enters, put target creature into its owner's
 * library just beneath the top X cards of that library, where X is the number of Plains you control."
 *
 * Proves the dynamic library depth: X counts the Plains as the trigger resolves, the creature lands
 * at index X of its owner's library (0 = top), and an X past the library's size puts it on the bottom.
 */
class QuarryColossusScenarioTest : ScenarioTestBase() {

    init {
        fun board(plains: Int, opponentLibrary: Int) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Quarry Colossus")
            .withLandsOnBattlefield(1, "Plains", plains)
            .withLandsOnBattlefield(1, "Mountain", 7 - plains)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Island")
            .apply { repeat(opponentLibrary) { withCardInLibrary(2, "Forest") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun ScenarioTestBase.TestGame.castColossusAt(target: String) {
            castSpell(1, "Quarry Colossus").error shouldBe null
            resolveStack()
            selectTargets(listOf(findPermanent(target)!!)).error shouldBe null
            resolveStack()
        }

        test("with three Plains the target goes just beneath the top three cards") {
            val game = board(plains = 3, opponentLibrary = 5)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castColossusAt("Grizzly Bears")

            game.findPermanent("Grizzly Bears") shouldBe null
            val library = game.state.getLibrary(game.player2Id)
            withClue("three cards above it, two below") {
                library.size shouldBe 6
                library.indexOf(bears) shouldBe 3
            }
        }

        test("with more Plains than cards in the library it goes to the bottom") {
            val game = board(plains = 7, opponentLibrary = 2)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castColossusAt("Grizzly Bears")

            val library = game.state.getLibrary(game.player2Id)
            library.size shouldBe 3
            library.last() shouldBe bears
        }

        test("X counts only the Plains you control, not the opponent's") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Quarry Colossus")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withLandsOnBattlefield(2, "Plains", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .apply { repeat(5) { withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castColossusAt("Grizzly Bears")

            game.state.getLibrary(game.player2Id).indexOf(bears) shouldBe 2
        }
    }
}
