package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Seedborn Muse.
 *
 * Seedborn Muse: {3}{G}{G}
 * Creature — Spirit 2/4
 * Untap all permanents you control during each other player's untap step.
 */
class SeedbornMuseScenarioTest : ScenarioTestBase() {

    init {
        context("Seedborn Muse untap during opponent's untap step") {

            test("untaps controller's tapped permanents during opponent's untap step") {
                // Player 1 controls Seedborn Muse and a tapped Forest
                // Start at Player 1's end step, so advancing passes through Player 2's untap
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Seedborn Muse")
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardOnBattlefield(2, "Mountain", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                // Verify initial state: Player 1's Forest is tapped
                val forestId = game.findPermanent("Forest")!!
                game.state.getEntity(forestId)!!.has<TappedComponent>() shouldBe true

                // Advance through turn transition into Player 2's upkeep
                // This will execute Player 2's untap step, which should also untap P1's permanents
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Player 1's Forest should now be untapped (Seedborn Muse effect)
                game.state.getEntity(forestId)!!.has<TappedComponent>() shouldBe false

                // Player 2's Mountain should also be untapped (it's their untap step)
                val mountainId = game.findPermanent("Mountain")!!
                game.state.getEntity(mountainId)!!.has<TappedComponent>() shouldBe false
            }

            test("does not untap controller's permanents during their own untap step (normal untap only)") {
                // Player 1 controls Seedborn Muse and a tapped Forest
                // Start at Player 2's end step, so advancing passes through Player 1's untap
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Seedborn Muse")
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val forestId = game.findPermanent("Forest")!!
                game.state.getEntity(forestId)!!.has<TappedComponent>() shouldBe true

                // Advance through Player 1's untap step
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Forest untaps normally during Player 1's own untap step
                game.state.getEntity(forestId)!!.has<TappedComponent>() shouldBe false
            }

            test("does not untap permanents with DOESNT_UNTAP during opponent's untap step") {
                // Use Goblin Sharpshooter which has DOESNT_UNTAP
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Seedborn Muse")
                    .withCardOnBattlefield(1, "Goblin Sharpshooter", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val sharpshooterId = game.findPermanent("Goblin Sharpshooter")!!
                game.state.getEntity(sharpshooterId)!!.has<TappedComponent>() shouldBe true

                // Advance through Player 2's untap step
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Goblin Sharpshooter should stay tapped due to DOESNT_UNTAP
                game.state.getEntity(sharpshooterId)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
