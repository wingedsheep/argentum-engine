package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Stoneforge Acolyte (OGW) — {W} 1/2 Kor Artificer Ally.
 * "Cohort — {T}, Tap an untapped Ally you control: Look at the top four cards of your library.
 * You may reveal an Equipment card from among them and put it into your hand. Put the rest on
 * the bottom of your library in any order."
 */
class StoneforgeAcolyteScenarioTest : ScenarioTestBase() {
    init {
        val cohortId = cardRegistry.getCard("Stoneforge Acolyte")!!.script.activatedAbilities.single().id

        fun board(vararg library: String): TestGame {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Stoneforge Acolyte")
                .withCardOnBattlefield(1, "Kor Sky Climber")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            library.forEach { builder.withCardInLibrary(1, it) }
            return builder.build()
        }

        fun TestGame.activateCohort() {
            val acolyte = findPermanent("Stoneforge Acolyte")!!
            val ally = findPermanent("Kor Sky Climber")!!
            val result = execute(
                ActivateAbility(
                    player1Id, acolyte, cohortId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(ally)),
                )
            )
            withClue("cohort should be payable with another Ally: ${result.error}") { result.error shouldBe null }
            resolveStack()
        }

        fun TestGame.finishOrdering() {
            if (getPendingDecision() is ReorderLibraryDecision) keepLibraryOrder()
            resolveStack()
        }

        test("puts an Equipment from the top four into hand and bottoms the rest") {
            val game = board("Island", "Bone Saw", "Forest", "Grizzly Bears", "Mountain")
            val saw = game.findCardsInLibrary(1, "Bone Saw").single()
            val mountain = game.findCardsInLibrary(1, "Mountain").single()

            game.activateCohort()
            (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            game.selectCards(listOf(saw)).error shouldBe null
            game.finishOrdering()

            game.isInHand(1, "Bone Saw") shouldBe true
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 4
            withClue("the fifth card, never looked at, is now on top") {
                game.state.getLibrary(game.player1Id).first() shouldBe mountain
            }
        }

        test("a non-Equipment card can't be taken") {
            val game = board("Island", "Grizzly Bears", "Forest", "Plains", "Mountain")
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

            game.activateCohort()
            if (game.getPendingDecision() is SelectCardsDecision) {
                game.selectCards(listOf(bears))
                if (game.getPendingDecision() is SelectCardsDecision) game.skipSelection()
            }
            game.finishOrdering()

            game.handSize(1) shouldBe 0
            game.librarySize(1) shouldBe 5
            game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Mountain").single()
        }
    }
}
