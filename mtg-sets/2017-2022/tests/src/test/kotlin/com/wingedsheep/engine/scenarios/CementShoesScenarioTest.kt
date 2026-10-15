package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cement Shoes (SNC #235) — {1} Artifact — Equipment
 *
 *   Equipped creature gets +3/+3 and has "At the beginning of your end step, tap this creature."
 *   Equipped creature doesn't untap during its controller's untap step.
 *   Equip {2}
 *
 * The granted trigger fires on the equipped creature's controller's end step only, and the
 * doesn't-untap clause keeps it tapped through that player's next untap step.
 */
class CementShoesScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, name: String): Boolean =
        game.state.getEntity(game.findPermanent(name)!!)?.has<TappedComponent>() == true

    init {
        context("Cement Shoes") {

            test("equipped creature is +3/+3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Cement Shoes", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 5
                game.state.projectedState.getToughness(bears) shouldBe 5
            }

            test("taps the creature at its controller's end step and it stays tapped through the next untap") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Cement Shoes", "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                isTapped(game, "Grizzly Bears") shouldBe false

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("the granted end-step trigger tapped it") {
                    isTapped(game, "Grizzly Bears") shouldBe true
                }

                // Opponent's turn: their end step does not matter; then back to our upkeep.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("it doesn't untap during its controller's untap step") {
                    isTapped(game, "Grizzly Bears") shouldBe true
                }
            }

            test("does not trigger on an opponent's end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Cement Shoes", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                withClue("\"your end step\" is the equipped creature's controller's") {
                    isTapped(game, "Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
