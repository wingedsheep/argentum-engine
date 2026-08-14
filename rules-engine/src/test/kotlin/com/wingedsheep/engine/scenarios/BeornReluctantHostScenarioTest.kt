package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Beorn, Reluctant Host // Till and Tend (HOB #118) — {4}{G} 5/5 Trample // {1}{G} Sorcery — Adventure
 *
 * Till and Tend: You may play an additional land this turn.
 *
 * Proves the Adventure half actually grants the extra land drop and then exiles on resolution, so
 * the creature stays castable from exile later (CR 715).
 */
class BeornReluctantHostScenarioTest : ScenarioTestBase() {

    init {
        context("Beorn, Reluctant Host") {

            test("Till and Tend grants a second land drop and exiles the card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Beorn, Reluctant Host")
                    .withCardsInHand(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Beorn, Reluctant Host"
                }

                // faceIndex = 0 is the Adventure half.
                val cast = game.execute(
                    CastSpell(playerId = game.player1Id, cardId = cardId, faceIndex = 0)
                )
                withClue("Casting Till and Tend should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The Adventure exiles on resolution, ready to be cast as Beorn later") {
                    game.isInExile(1, "Beorn, Reluctant Host") shouldBe true
                }

                val handForests = game.findCardsInHand(1, "Forest")
                withClue("The normal land drop") {
                    game.execute(PlayLand(game.player1Id, handForests[0])).error shouldBe null
                }
                withClue("And the additional one Till and Tend granted") {
                    game.execute(PlayLand(game.player1Id, handForests[1])).error shouldBe null
                }
                withClue("Two Forests started in play, two more were played") {
                    game.findAllPermanents("Forest").size shouldBe 4
                }
            }
        }
    }
}
