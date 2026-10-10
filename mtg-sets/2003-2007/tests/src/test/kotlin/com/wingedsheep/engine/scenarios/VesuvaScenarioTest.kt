package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Vesuva (TSP #281) — Land
 * "You may have this land enter tapped as a copy of any land on the battlefield."
 *
 * First card using a *battlefield*-sourced [com.wingedsheep.sdk.scripting.EntersAsCopy] with the
 * `tappedIfCopied` rider on the land-play path. Covers: copying an opponent's tapped land (Vesuva
 * enters tapped because it copied, not because the original was tapped), and declining.
 */
class VesuvaScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.idOnBattlefield(playerId: com.wingedsheep.sdk.model.EntityId, name: String) =
        state.getBattlefield(playerId).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    init {
        context("Vesuva — enters as a copy of a land on the battlefield") {

            test("copying an opponent's land makes Vesuva that land, tapped, under your control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vesuva")
                    .withCardOnBattlefield(2, "Forest", tapped = false)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vesuva = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Vesuva"
                }
                val played = game.execute(PlayLand(game.player1Id, vesuva))
                withClue("Playing Vesuva should succeed: ${played.error}") { played.error shouldBe null }
                withClue("An EntersAsCopy selection should be pending") { game.hasPendingDecision().shouldBeTrue() }

                val forest = game.idOnBattlefield(game.player2Id, "Forest")
                val resolved = game.selectCards(listOf(forest))
                withClue("Resolving the copy choice should succeed: ${resolved.error}") { resolved.error shouldBe null }

                val copy = game.idOnBattlefield(game.player1Id, "Forest")
                withClue("Vesuva is now a Forest") {
                    game.state.projectedState.getSubtypes(copy).any { it.equals("Forest", ignoreCase = true) }.shouldBeTrue()
                }
                withClue("Enters tapped because it entered as a copy") {
                    game.state.getEntity(copy)?.has<TappedComponent>()?.shouldBeTrue()
                }
                withClue("The copied Forest itself is untouched") {
                    game.state.getEntity(forest)?.has<TappedComponent>()?.shouldBeFalse()
                }
            }

            test("declining the copy enters untapped as Vesuva") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vesuva")
                    .withCardOnBattlefield(1, "Island")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vesuva = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Vesuva"
                }
                game.execute(PlayLand(game.player1Id, vesuva)).error shouldBe null
                game.hasPendingDecision().shouldBeTrue()

                val resolved = game.skipSelection()
                withClue("Declining should succeed: ${resolved.error}") { resolved.error shouldBe null }

                val land = game.idOnBattlefield(game.player1Id, "Vesuva")
                withClue("Enters untapped as its printed self") {
                    game.state.getEntity(land)?.has<TappedComponent>()?.shouldBeFalse()
                }
            }
        }
    }
}
