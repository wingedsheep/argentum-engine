package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Salvage (Portal Second Age #145).
 *
 * {G} Sorcery — "Put target card from your graveyard on top of your library."
 */
class SalvageScenarioTest : ScenarioTestBase() {

    init {
        context("Salvage") {
            test("puts the targeted graveyard card on top of your library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Salvage")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.state.getGraveyard(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
                val cardId = game.state.getHand(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Salvage" }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD))
                    )
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears left the graveyard") {
                    (bears in game.state.getGraveyard(game.player1Id)) shouldBe false
                }
                withClue("Grizzly Bears is on top of the library") {
                    game.state.getLibrary(game.player1Id).first() shouldBe bears
                }
            }

            test("cannot target a card in an opponent's graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Salvage")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirs = game.state.getGraveyard(game.player2Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
                val cardId = game.state.getHand(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Salvage" }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Card(theirs, game.player2Id, Zone.GRAVEYARD))
                    )
                )
                (cast.error != null) shouldBe true
            }
        }
    }
}
