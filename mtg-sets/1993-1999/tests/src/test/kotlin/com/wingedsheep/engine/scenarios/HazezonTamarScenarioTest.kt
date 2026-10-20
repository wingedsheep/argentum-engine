package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hazezon Tamar (LEG #230).
 *
 * "When Hazezon enters, create X 1/1 Sand Warrior creature tokens that are red, green, and white
 * at the beginning of your next upkeep, where X is the number of lands you control at that time.
 * When Hazezon leaves the battlefield, exile all Sand Warriors."
 *
 * The tokens wait for the caster's own upkeep (not the opponent's intervening one), count lands at
 * that time, and are exiled when Hazezon leaves. Per the ruling, the delayed trigger fires even if
 * Hazezon left before it — and those later Sand Warriors stay.
 */
class HazezonTamarScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Hazezon", "Opponent")
        .withCardInHand(1, "Hazezon Tamar")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardInHand(2, "Boomerang")
        .withLandsOnBattlefield(2, "Island", 2)
        .apply {
            repeat(5) {
                withCardInLibrary(1, "Grizzly Bears")
                withCardInLibrary(2, "Grizzly Bears")
            }
        }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castHazezon() {
        castSpell(1, "Hazezon Tamar").error shouldBe null
        resolveStack()
        isOnBattlefield("Hazezon Tamar") shouldBe true
    }

    /** From any main phase, advance to the next upkeep (whoever's turn it is). */
    private fun TestGame.toNextUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    /** From an upkeep, advance to that turn's precombat main. */
    private fun TestGame.toMain() {
        passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
    }

    private fun TestGame.opponentBouncesHazezon() {
        castSpell(2, "Boomerang", findPermanent("Hazezon Tamar")!!).error shouldBe null
        resolveStack()
        isInHand(1, "Hazezon Tamar") shouldBe true
    }

    init {
        context("Hazezon Tamar") {

            test("tokens arrive on your next upkeep, one per land, and are exiled when Hazezon leaves") {
                val game = board()
                game.castHazezon()

                game.toNextUpkeep()
                withClue("the opponent's upkeep is not your next upkeep") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.findPermanents("Sand Warrior Token").shouldBeEmpty()
                }

                game.toMain()
                game.toNextUpkeep()
                game.state.activePlayerId shouldBe game.player1Id
                val warriors = game.findPermanents("Sand Warrior Token")
                withClue("X = the seven lands you control") { warriors.size shouldBe 7 }
                val projected = game.state.projectedState
                warriors.forEach { id ->
                    projected.getController(id) shouldBe game.player1Id
                    projected.getPower(id) shouldBe 1
                    projected.getToughness(id) shouldBe 1
                    projected.getColors(id) shouldBe setOf(Color.RED.name, Color.GREEN.name, Color.WHITE.name)
                    projected.hasSubtype(id, "Sand") shouldBe true
                    projected.hasSubtype(id, "Warrior") shouldBe true
                }

                // On the opponent's next turn, bounce Hazezon: every Sand Warrior is exiled.
                game.toMain()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.opponentBouncesHazezon()

                withClue("all Sand Warriors are gone") {
                    game.findPermanents("Sand Warrior Token").shouldBeEmpty()
                }
            }

            test("the delayed trigger still fires after Hazezon has left, and those tokens stay") {
                val game = board()
                game.castHazezon()

                // Opponent's turn: bounce Hazezon before its delayed trigger fires.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                game.opponentBouncesHazezon()

                game.toNextUpkeep()
                game.state.activePlayerId shouldBe game.player1Id
                withClue("the tokens are still created for the player who put Hazezon onto the battlefield") {
                    game.findPermanents("Sand Warrior Token").size shouldBe 7
                    game.findPermanents("Sand Warrior Token").forEach {
                        game.state.projectedState.getController(it) shouldBe game.player1Id
                    }
                }

                game.toMain()
                withClue("nothing exiles Sand Warriors created after Hazezon left") {
                    game.findPermanents("Sand Warrior Token").size shouldBe 7
                }
            }
        }
    }
}
