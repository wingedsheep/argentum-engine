package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Blood Operative (GRN #63) — {1}{B}{B} Creature — Vampire Assassin 3/1.
 *
 *   Whenever you surveil, if this card is in your graveyard, you may pay 3 life. If you do,
 *   return this card to your hand.
 *
 * The recursion trigger works from the graveyard, including when the surveil itself put the card
 * there.
 */
class BloodOperativeScenarioTest : ScenarioTestBase() {

    init {
        val surveilOne = card("Test Operative Surveil") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            oracleText = "Surveil 1."
            spell { effect = Patterns.Library.surveil(1) }
        }
        cardRegistry.register(listOf(surveilOne))

        /** Cast the surveil spell; [toGraveyard] puts the looked-at card into the graveyard. */
        fun ScenarioTestBase.TestGame.surveil(toGraveyard: Boolean) {
            castSpell(1, "Test Operative Surveil").error shouldBe null
            resolveStack()
            val decision = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            if (toGraveyard) selectCards(decision.options).error shouldBe null else skipSelection()
            if (getPendingDecision() is ReorderLibraryDecision) keepLibraryOrder()
            resolveStack()
        }

        test("surveilling with Blood Operative in the graveyard lets you pay 3 life to return it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Blood Operative")
                .withCardInHand(1, "Test Operative Surveil")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.surveil(toGraveyard = false)
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Blood Operative") shouldBe true
            game.isInGraveyard(1, "Blood Operative") shouldBe false
            game.getLifeTotal(1) shouldBe 17
        }

        test("a Blood Operative the surveil itself puts into the graveyard triggers") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Test Operative Surveil")
                .withCardInLibrary(1, "Blood Operative")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.surveil(toGraveyard = true)
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Blood Operative") shouldBe true
            game.getLifeTotal(1) shouldBe 17
        }

        test("declining to pay leaves it in the graveyard and keeps your life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Blood Operative")
                .withCardInHand(1, "Test Operative Surveil")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.surveil(toGraveyard = false)
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Blood Operative") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
