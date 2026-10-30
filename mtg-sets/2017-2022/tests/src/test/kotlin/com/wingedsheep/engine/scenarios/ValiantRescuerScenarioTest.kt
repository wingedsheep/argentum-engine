package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Valiant Rescuer (IKO) — "Whenever you cycle another card for the first time
 * each turn, create a 1/1 white Human Soldier creature token." Cycling {2}.
 *
 * Pins `CycleEvent.firstTimeEachTurn`: only the controller's first cycle of a turn matches, the
 * count is per player (an opponent's cycle doesn't use it up, a cycle made before the Rescuer
 * entered does), typecycling counts as cycling, and the count resets each turn.
 */
class ValiantRescuerScenarioTest : ScenarioTestBase() {

    private fun soldiers(game: TestGame): Int = game.findPermanents("Human Soldier Token").size

    private fun setup(rescuerOnBattlefield: Boolean = true, vararg inHand: String): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withLandsOnBattlefield(1, "Plains", 8)
            .withLandsOnBattlefield(2, "Mountain", 2)
            .withCardInHand(2, "Lava Serpent")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (rescuerOnBattlefield) builder = builder.withCardOnBattlefield(1, "Valiant Rescuer")
        for (card in inHand) builder = builder.withCardInHand(1, card)
        repeat(6) { builder = builder.withCardInLibrary(1, "Mountain") }
        repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    init {
        context("Valiant Rescuer") {
            test("only the first cycle of the turn creates a Soldier") {
                val game = setup(true, "Lava Serpent", "Drannith Stinger")

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                game.resolveStack()
                soldiers(game) shouldBe 1

                game.cycleCard(1, "Drannith Stinger").error shouldBe null
                game.resolveStack()
                withClue("the second cycle this turn doesn't trigger") {
                    soldiers(game) shouldBe 1
                }
            }

            test("an opponent's cycle neither triggers it nor uses up the first time") {
                val game = setup(true, "Lava Serpent")

                game.passPriority()
                game.cycleCard(2, "Lava Serpent").error shouldBe null
                soldiers(game) shouldBe 0
                game.passPriority()

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                game.resolveStack()
                soldiers(game) shouldBe 1
            }

            test("a card cycled before the Rescuer entered uses up the first time") {
                val game = setup(false, "Lava Serpent", "Drannith Stinger", "Valiant Rescuer")

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Valiant Rescuer").error shouldBe null
                game.resolveStack()
                game.findPermanents("Valiant Rescuer").size shouldBe 1

                game.cycleCard(1, "Drannith Stinger").error shouldBe null
                game.resolveStack()
                soldiers(game) shouldBe 0
            }

            test("typecycling counts as the first cycle") {
                val game = setup(true, "Twisted Abomination", "Lava Serpent")

                game.typecycleCard(1, "Twisted Abomination").error shouldBe null
                withClue("the empty Swamp search is answered first") {
                    game.selectCards(emptyList()).error shouldBe null
                }
                game.resolveStack()
                soldiers(game) shouldBe 1

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                game.resolveStack()
                soldiers(game) shouldBe 1
            }

            test("the first time resets each turn, including the opponent's turn") {
                val game = setup(true, "Lava Serpent", "Drannith Stinger")

                game.cycleCard(1, "Lava Serpent").error shouldBe null
                game.resolveStack()
                soldiers(game) shouldBe 1

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.passPriority()
                game.cycleCard(1, "Drannith Stinger").error shouldBe null
                game.resolveStack()
                withClue("a new turn's first cycle triggers again") {
                    soldiers(game) shouldBe 2
                }
            }
        }
    }
}
