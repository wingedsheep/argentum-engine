package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Spectral Denial. */
class SpectralDenialScenarioTest : ScenarioTestBase() {

    init {
        context("Spectral Denial") {
            test("counters a spell whose tapped-out controller cannot pay {X}") {
                // Player 2 taps out casting Grizzly Bears; Player 1 responds with Spectral Denial
                // for X=2, which Player 2 cannot pay, so the spell is countered.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Spectral Denial")
                    .withLandsOnBattlefield(1, "Island", 3) // {X}{U} with X=2 → {2}{U}
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2) // taps out
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gbCast = game.castSpell(2, "Grizzly Bears")
                withClue("Grizzly Bears cast should succeed: ${gbCast.error}") { gbCast.error shouldBe null }
                game.passPriority()

                val targetSpell = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Spectral Denial").first(),
                        listOf(ChosenTarget.Spell(targetSpell)),
                        xValue = 2,
                    )
                )
                withClue("Spectral Denial cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }

                withClue("Grizzly Bears should be countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("its discount keeps paying into X once the printed generic is gone (CR 601.2f)") {
                // Two power-4 creatures take {2} off a {X}{U} spell with no printed generic: with a
                // single Island Player 1 can still cast it for X = 2, and the offer advertises X = 2.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Spectral Denial")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardOnBattlefield(1, "Hill Giant") // power 3 — no discount
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val offer = game.getLegalActions(1).first {
                    it.actionType == "CastSpell" && it.description.contains("Spectral Denial")
                }
                withClue("one Island plus the {2} discount affords X = 2") {
                    offer.maxAffordableX shouldBe 2
                }

                val targetSpell = game.state.stack.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Spectral Denial").first(),
                        listOf(ChosenTarget.Spell(targetSpell)),
                        xValue = 2,
                    )
                )
                withClue("Spectral Denial for X = 2 should cost just {U}: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.skipSelection()
                    game.resolveStack()
                }
                withClue("Grizzly Bears should be countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
