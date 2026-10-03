package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Primeval Herald (J22 #42) — {3}{G} Creature — Elf Scout, 3/1, Trample.
 *
 *   Whenever this creature enters or attacks, you may search your library for a basic land card,
 *   put it onto the battlefield tapped, then shuffle.
 *
 * Both halves of "enters or attacks" fetch a tapped basic; only basic lands are offered.
 */
class PrimevalHeraldScenarioTest : ScenarioTestBase() {

    init {
        context("Primeval Herald") {

            test("entering fetches a basic land onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Primeval Herald")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Primeval Herald").error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                val search = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a library search; got ${game.getPendingDecision()}")
                withClue("only the basic land is offered, and at most one") {
                    search.options.size shouldBe 1
                    search.maxSelections shouldBe 1
                }
                game.selectCards(search.options).error shouldBe null
                game.resolveStack()

                val plains = game.findPermanent("Plains")
                withClue("Plains entered the battlefield tapped") {
                    (plains != null) shouldBe true
                    game.state.getEntity(plains!!)?.has<TappedComponent>() shouldBe true
                }
            }

            test("attacking fetches a basic land onto the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Primeval Herald", summoningSickness = false)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Primeval Herald" to 2)).error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                val search = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a library search; got ${game.getPendingDecision()}")
                game.selectCards(search.options).error shouldBe null
                game.resolveStack()

                val plains = game.findPermanent("Plains")
                withClue("the attack trigger put Plains onto the battlefield tapped") {
                    (plains != null) shouldBe true
                    game.state.getEntity(plains!!)?.has<TappedComponent>() shouldBe true
                    game.librarySize(1) shouldBe 0
                }
            }
        }
    }
}
