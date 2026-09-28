package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Harmony of Nature (P02 #128) — {2}{G} Sorcery.
 *
 * "Tap any number of untapped creatures you control. You gain 4 life for each creature tapped
 *  this way."
 */
class HarmonyOfNatureScenarioTest : ScenarioTestBase() {

    init {
        fun tapped(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Boolean =
            game.state.getEntity(id)?.get<TappedComponent>() != null

        context("Harmony of Nature") {

            test("tapping two creatures gains 8 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Harmony of Nature")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Harmony of Nature").error shouldBe null
                var guard = 0
                while (game.getPendingDecision() !is SelectCardsDecision && guard++ < 20) {
                    game.resolveStack()
                }
                val decision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision; got ${game.getPendingDecision()}")
                game.submitDecision(CardsSelectedResponse(decision.id, listOf(bear, giant)))
                game.resolveStack()

                withClue("Both creatures tapped") {
                    tapped(game, bear) shouldBe true
                    tapped(game, giant) shouldBe true
                }
                withClue("4 life per creature tapped") {
                    game.getLifeTotal(1) shouldBe 28
                }
            }

            test("tapping nothing gains no life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Harmony of Nature")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Harmony of Nature").error shouldBe null
                var guard = 0
                while (game.getPendingDecision() !is SelectCardsDecision && guard++ < 20) {
                    game.resolveStack()
                }
                val decision = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a SelectCardsDecision; got ${game.getPendingDecision()}")
                game.submitDecision(CardsSelectedResponse(decision.id, emptyList()))
                game.resolveStack()

                tapped(game, bear) shouldBe false
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
