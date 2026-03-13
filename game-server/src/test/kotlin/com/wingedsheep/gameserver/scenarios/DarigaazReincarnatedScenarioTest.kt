package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Darigaaz Reincarnated.
 *
 * Card reference:
 * - Darigaaz Reincarnated ({4}{B}{R}{G}): Legendary Creature — Dragon 7/7
 *   Flying, trample, haste
 *   If Darigaaz Reincarnated would die, instead exile it with three egg counters on it.
 *   At the beginning of your upkeep, if Darigaaz is exiled with an egg counter on it,
 *   remove an egg counter from it. Then if Darigaaz has no egg counters on it,
 *   return it to the battlefield.
 */
class DarigaazReincarnatedScenarioTest : ScenarioTestBase() {

    private fun getExile(game: TestGame, playerNumber: Int): List<com.wingedsheep.sdk.model.EntityId> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId)
    }

    private fun getEggCounters(game: TestGame, entityId: com.wingedsheep.sdk.model.EntityId): Int {
        return game.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.EGG) ?: 0
    }

    /**
     * Pass through the current player's turn and the opponent's turn,
     * arriving at the next precombat main of the player whose turn it should be.
     */
    private fun passFullRound(game: TestGame) {
        // Pass to the end step to leave current precombat main
        game.passUntilPhase(Phase.ENDING, Step.END)
        // Pass to next player's precombat main
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        // Now pass that player's turn too
        game.passUntilPhase(Phase.ENDING, Step.END)
        // And arrive at our next precombat main
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
    }

    init {
        context("Darigaaz Reincarnated") {

            test("when Darigaaz would die, exile it with 3 egg counters instead") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Darigaaz Reincarnated")
                    .withCardInHand(2, "Eviscerate") // {3}{B} - destroy target creature
                    .withLandsOnBattlefield(2, "Swamp", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Eviscerate targeting Darigaaz
                val castResult = game.castSpell(2, "Eviscerate", game.findPermanent("Darigaaz Reincarnated")!!)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Darigaaz should not be on the battlefield
                withClue("Darigaaz should not be on the battlefield") {
                    game.isOnBattlefield("Darigaaz Reincarnated") shouldBe false
                }

                // Darigaaz should be in exile with 3 egg counters
                val exile = getExile(game, 1)
                val darigaazInExile = exile.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Darigaaz Reincarnated"
                }
                withClue("Darigaaz should be in exile") {
                    (darigaazInExile != null) shouldBe true
                }
                withClue("Darigaaz should have 3 egg counters") {
                    getEggCounters(game, darigaazInExile!!) shouldBe 3
                }
            }

            test("egg counters are removed one per upkeep, returns after 3rd upkeep") {
                // Start on P1's main so P1's upkeep comes after a full round
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Darigaaz Reincarnated")
                    .withCardInHand(1, "Eviscerate") // P1 destroys own Darigaaz for test setup
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // P1 kills own Darigaaz
                val castResult = game.castSpell(1, "Eviscerate", game.findPermanent("Darigaaz Reincarnated")!!)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find Darigaaz in exile
                val darigaazId = getExile(game, 1).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Darigaaz Reincarnated"
                }!!

                withClue("Initial egg counters should be 3") {
                    getEggCounters(game, darigaazId) shouldBe 3
                }

                // Pass to end of P1's turn, then through P2's turn, to P1's next precombat main
                // This passes through P1's upkeep (where the trigger fires)
                passFullRound(game)

                // After P1's first upkeep: 2 egg counters remaining
                withClue("After first upkeep, Darigaaz should have 2 egg counters") {
                    getEggCounters(game, darigaazId) shouldBe 2
                }
                withClue("Darigaaz should still be in exile") {
                    game.isOnBattlefield("Darigaaz Reincarnated") shouldBe false
                }

                // Pass another full round (P1's turn + P2's turn)
                passFullRound(game)

                // After P1's second upkeep: 1 egg counter remaining
                withClue("After second upkeep, Darigaaz should have 1 egg counter") {
                    getEggCounters(game, darigaazId) shouldBe 1
                }
                withClue("Darigaaz should still be in exile") {
                    game.isOnBattlefield("Darigaaz Reincarnated") shouldBe false
                }

                // Pass another full round (P1's turn + P2's turn)
                passFullRound(game)

                // After P1's third upkeep: 0 egg counters, Darigaaz returns to battlefield
                withClue("After third upkeep, Darigaaz should be back on the battlefield") {
                    game.isOnBattlefield("Darigaaz Reincarnated") shouldBe true
                }
            }
        }
    }
}
