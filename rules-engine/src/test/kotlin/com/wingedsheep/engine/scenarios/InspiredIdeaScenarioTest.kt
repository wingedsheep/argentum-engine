package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.PlayerMaximumHandSizeReductionComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Inspired Idea (VOW #64) — {2}{U} Sorcery with Cleave {3}{U}{U}.
 *
 * "Draw three cards. [Your maximum hand size is reduced by three for the rest of the game.]"
 *
 * Cleave (CR 702.148) removes the bracketed words when the alternative cost is paid. The two modes
 * differ only in their effect (the spell has no target):
 *  - Printed cast ({2}{U}, brackets present): draw three AND reduce maximum hand size by three for
 *    the rest of the game ([Effects.ReduceMaximumHandSize] → an accumulating
 *    [PlayerMaximumHandSizeReductionComponent]).
 *  - Cleaved cast ({3}{U}{U}, brackets removed): draw three, no downside.
 */
class InspiredIdeaScenarioTest : ScenarioTestBase() {

    private fun maxHandSizeReduction(game: TestGame, playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getEntity(playerId)
            ?.get<PlayerMaximumHandSizeReductionComponent>()?.amount ?: 0
    }

    init {
        context("Inspired Idea — cleave drops the maximum-hand-size downside") {

            test("printed cast draws three and reduces maximum hand size by three") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inspired Idea")
                    .withLandsOnBattlefield(1, "Island", 3)   // {2}{U}
                    // Cards to draw.
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Goblin Guide")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Inspired Idea").error shouldBe null
                game.resolveStack()

                withClue("printed cast drew three cards") {
                    game.handSize(1) shouldBe handBefore - 1 + 3   // spell left hand, drew three
                }
                withClue("printed cast reduced maximum hand size by three for the rest of the game") {
                    maxHandSizeReduction(game, 1) shouldBe 3
                }
            }

            test("cleaved cast draws three with NO maximum-hand-size reduction") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Inspired Idea")
                    .withLandsOnBattlefield(1, "Island", 5)   // {3}{U}{U}
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Goblin Guide")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpellWithCleave(1, "Inspired Idea").error shouldBe null
                game.resolveStack()

                withClue("cleaved cast drew three cards") {
                    game.handSize(1) shouldBe handBefore - 1 + 3
                }
                withClue("cleaved cast conferred NO maximum-hand-size reduction") {
                    maxHandSizeReduction(game, 1) shouldBe 0
                }
            }
        }
    }
}
