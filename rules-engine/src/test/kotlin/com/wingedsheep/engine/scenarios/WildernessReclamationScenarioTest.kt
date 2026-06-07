package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wilderness Reclamation (RNA #149).
 *
 * "{3}{G} Enchantment. At the beginning of your end step, untap all lands you control."
 *
 * Exercises the mtgish-tooling `AtTheBeginningOfAPlayersEndStep` (You scope) mapping ->
 * `Triggers.YourEndStep`. The trigger must:
 *   - untap the controller's tapped lands when their end step begins,
 *   - leave the OPPONENT's lands alone (the "you control" filter), and
 *   - NOT fire on the opponent's end step ("your" scope, not "each").
 */
class WildernessReclamationScenarioTest : ScenarioTestBase() {

    private fun tappedLands(game: TestGame, playerId: EntityId) =
        game.state.getBattlefield(playerId)
            .filter { game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest" }
            .map { it to (game.state.getEntity(it)?.has<TappedComponent>() == true) }

    init {
        context("Wilderness Reclamation end step") {

            test("untaps all of your lands at the beginning of your end step") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wilderness Reclamation")
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardOnBattlefield(2, "Forest", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                // Advance to player 1's end step so the "your end step" trigger fires, then drain it.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("All of player 1's lands should be untapped after Wilderness Reclamation resolves") {
                    tappedLands(game, game.player1Id).forEach { (_, tapped) -> tapped shouldBe false }
                }
                withClue("Player 2's land must stay tapped — Wilderness Reclamation only untaps lands YOU control") {
                    tappedLands(game, game.player2Id).single().second shouldBe true
                }
            }

            test("does not fire on the opponent's end step (your scope, not each)") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wilderness Reclamation")
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                // Player 2's end step — Wilderness Reclamation's controller is player 1, so it must not fire.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Player 1's land should remain tapped during the opponent's end step") {
                    tappedLands(game, game.player1Id).single().second shouldBe true
                }
            }
        }
    }
}
