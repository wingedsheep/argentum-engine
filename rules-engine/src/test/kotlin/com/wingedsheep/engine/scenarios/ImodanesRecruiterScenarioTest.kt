package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Imodane's Recruiter // Train Troops. */
class ImodanesRecruiterScenarioTest : ScenarioTestBase() {

    init {
        context("Imodane's Recruiter // Train Troops") {
            test("the enters trigger gives creatures you control +1/+0 and haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Imodane's Recruiter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castSpell(1, "Imodane's Recruiter").error shouldBe null
                game.resolveStack()

                val recruiter = game.findPermanent("Imodane's Recruiter")!!

                withClue("the Bears got +1/+0 and haste") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 2
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                }
                withClue("the Recruiter is on the battlefield when its own trigger resolves") {
                    game.state.projectedState.getPower(recruiter) shouldBe 3
                    game.state.projectedState.hasKeyword(recruiter, Keyword.HASTE) shouldBe true
                }
                withClue("the opponent's creature is untouched") {
                    game.state.projectedState.getPower(wurm) shouldBe 6
                    game.state.projectedState.hasKeyword(wurm, Keyword.HASTE) shouldBe false
                }
            }

            test("Train Troops makes two 2/2 vigilant Knights and exiles the card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Imodane's Recruiter")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Imodane's Recruiter"
                }

                // faceIndex = 0 casts the Adventure half, Train Troops.
                game.execute(
                    CastSpell(playerId = game.player1Id, cardId = cardId, faceIndex = 0)
                ).error shouldBe null
                game.resolveStack()

                val knights = game.findAllPermanents("Knight Token")
                withClue("two Knight tokens") { knights.size shouldBe 2 }
                knights.forEach {
                    game.state.projectedState.getPower(it) shouldBe 2
                    game.state.projectedState.getToughness(it) shouldBe 2
                    game.state.projectedState.hasKeyword(it, Keyword.VIGILANCE) shouldBe true
                }
                withClue("the Adventure exiled itself so the creature can be cast later") {
                    game.isInExile(1, "Imodane's Recruiter") shouldBe true
                }
            }
        }
    }
}
