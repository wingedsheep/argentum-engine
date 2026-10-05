package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sage of the Falls (ELD #63) — {4}{U} Creature — Merfolk Wizard, 2/5.
 *
 * Whenever this creature or another non-Human creature you control enters, you may draw a card.
 * If you do, discard a card.
 */
class SageOfTheFallsScenarioTest : ScenarioTestBase() {

    init {
        context("Sage of the Falls") {

            test("its own entry offers a loot; accepting draws then discards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sage of the Falls")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Sage of the Falls").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                withClue("drew the Forest") { game.librarySize(1) shouldBe 0 }
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("discarded Grizzly Bears, kept the Forest") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.findCardsInHand(1, "Forest").size shouldBe 1
                }
            }

            test("another non-Human creature entering triggers; declining draws and discards nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sage of the Falls")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("declined the may — no draw, no discard") {
                    game.librarySize(1) shouldBe 1
                    game.graveyardSize(1) shouldBe 0
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("a Human creature entering does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sage of the Falls")
                    .withCardInHand(1, "Elite Vanguard")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Elite Vanguard").error shouldBe null
                game.resolveStack()

                withClue("no trigger for a Human") {
                    game.hasPendingDecision() shouldBe false
                    game.librarySize(1) shouldBe 1
                    game.isOnBattlefield("Elite Vanguard") shouldBe true
                }
            }
        }
    }
}
