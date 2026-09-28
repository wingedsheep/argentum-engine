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
 * Renewing Touch (P02): {G} Sorcery — "Shuffle any number of target creature cards from your
 * graveyard into your library."
 */
class RenewingTouchScenarioTest : ScenarioTestBase() {

    init {
        context("Renewing Touch") {

            test("shuffles the chosen creature cards from your graveyard into your library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Renewing Touch")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Giant Growth")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun gy(name: String) = game.state.getGraveyard(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

                val bears = gy("Grizzly Bears")
                val giant = gy("Hill Giant")
                val growth = gy("Giant Growth")
                val cardId = game.state.getHand(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Renewing Touch" }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD),
                            ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD)
                        )
                    )
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val graveyard = game.state.getGraveyard(game.player1Id)
                val library = game.state.getLibrary(game.player1Id)
                withClue("both creature cards left the graveyard") {
                    (bears in graveyard) shouldBe false
                    (giant in graveyard) shouldBe false
                }
                withClue("both creature cards are now in the library") {
                    (bears in library) shouldBe true
                    (giant in library) shouldBe true
                }
                withClue("the nontargeted noncreature card stays in the graveyard") {
                    (growth in graveyard) shouldBe true
                }
            }

            test("a creature card in the opponent's graveyard is not a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Renewing Touch")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theirs = game.state.getGraveyard(game.player2Id).first()
                val cardId = game.state.getHand(game.player1Id)
                    .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Renewing Touch" }

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
