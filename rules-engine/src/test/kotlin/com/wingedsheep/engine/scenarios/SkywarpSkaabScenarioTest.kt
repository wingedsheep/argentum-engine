package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Skywarp Skaab (VOW #78) — {3}{U}{U} Creature — Zombie Drake, 2/5, Flying.
 *
 *   When this creature enters, you may exile two creature cards from your graveyard. If you do,
 *   draw a card.
 *
 * Exercises the "exile exactly two" pipeline: accepting with two creature cards available exiles
 * both and draws a card; declining leaves the graveyard untouched and draws nothing.
 */
class SkywarpSkaabScenarioTest : ScenarioTestBase() {

    init {
        context("Skywarp Skaab ETB") {

            test("Skywarp Skaab is a 2/5 flyer") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skywarp Skaab", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val skaab = game.findPermanent("Skywarp Skaab")!!
                withClue("Skywarp Skaab is a 2/5") {
                    game.state.projectedState.getPower(skaab) shouldBe 2
                    game.state.projectedState.getToughness(skaab) shouldBe 5
                }
                withClue("Skywarp Skaab has flying") {
                    game.state.projectedState.hasKeyword(skaab, Keyword.FLYING) shouldBe true
                }
            }

            test("accepting the may exiles two creature cards and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Skywarp Skaab")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Skywarp Skaab").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the ETB offers a yes/no to exile two creature cards") {
                    decision.shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true)

                if (game.hasPendingDecision()) {
                    val graveyard = game.state.getGraveyard(game.player1Id)
                    game.selectCards(graveyard.take(2))
                }
                game.resolveStack()

                withClue("both creature cards were exiled from the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Hill Giant") shouldBe false
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(1, "Hill Giant") shouldBe true
                }
                withClue("a card was drawn") {
                    game.isInHand(1, "Plains") shouldBe true
                }
            }

            test("declining the may leaves the graveyard untouched and draws no card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Skywarp Skaab")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Skywarp Skaab").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("both creature cards remain in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                }
                withClue("no card was drawn") {
                    game.isInHand(1, "Plains") shouldBe false
                }
            }
        }
    }
}
