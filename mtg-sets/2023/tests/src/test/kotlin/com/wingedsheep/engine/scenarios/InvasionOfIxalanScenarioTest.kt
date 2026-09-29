package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Ixalan // Belligerent Regisaur (MOM #191).
 *
 * Front: "When this Siege enters, look at the top five cards of your library. You may reveal a
 * permanent card from among them and put it into your hand. Put the rest on the bottom of your
 * library in a random order."
 * Back: Trample; "Whenever you cast a spell, this creature gains indestructible until end of turn."
 */
class InvasionOfIxalanScenarioTest : ScenarioTestBase() {

    private fun frontSetup() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Invasion of Ixalan")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Divination")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Shock")
        .withCardInLibrary(1, "Hill Giant") // sixth card — out of reach
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("front: only permanent cards among the top five can be taken; the rest go to the bottom") {
            val game = frontSetup()
            game.castSpell(1, "Invasion of Ixalan").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as SelectCardsDecision
            withClue("Grizzly Bears and Mountain are permanent cards; Hill Giant is sixth and unseen") {
                decision.options.size shouldBe 2
            }
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            game.selectCards(listOf(bears)).error shouldBe null
            while (game.hasPendingDecision()) game.keepLibraryOrder()
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Mountain") shouldBe false
            withClue("the other four went to the bottom, so Hill Giant is now on top") {
                game.state.getLibrary(game.player1Id).first() shouldBe
                    game.findCardsInLibrary(1, "Hill Giant").single()
            }
        }

        test("front: declining keeps every card in the library") {
            val game = frontSetup()
            val before = game.librarySize(1)
            game.castSpell(1, "Invasion of Ixalan").error shouldBe null
            game.resolveStack()
            game.skipSelection()
            while (game.hasPendingDecision()) game.keepLibraryOrder()
            game.resolveStack()

            game.librarySize(1) shouldBe before
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }

        test("back: casting a spell makes Belligerent Regisaur indestructible until end of turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Ixalan")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            repeat(2) {
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Ixalan")!!).error shouldBe null
                game.resolveStack()
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val regisaur = game.findPermanent("Belligerent Regisaur")!!
            game.state.projectedState.hasKeyword(regisaur, Keyword.TRAMPLE) shouldBe true
            game.state.projectedState.hasKeyword(regisaur, Keyword.INDESTRUCTIBLE) shouldBe false

            game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(regisaur, Keyword.INDESTRUCTIBLE) shouldBe true
        }
    }
}
