package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Myr Custodian — {3} Artifact Creature — Myr 2/3
 * "When this creature enters, scry 2. Then each opponent may scry 1."
 *
 * The controller scries 2 first; then the opponent (not the controller) is asked whether to scry,
 * and the scry looks at the opponent's own library.
 */
class MyrCustodianScenarioTest : ScenarioTestBase() {

    private fun setup(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Myr Custodian")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Mountain")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Resolve Custodian and its trigger up to the controller's scry-2 prompt; keep both on top. */
    private fun TestGame.castAndControllerKeepsScry() {
        castSpell(1, "Myr Custodian").error shouldBe null
        resolveStack()
        val scry = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        scry.playerId shouldBe player1Id
        scry.options.size shouldBe 2
        scry.options.forEach { state.getLibrary(player1Id).contains(it) shouldBe true }
        selectCards(emptyList()).error shouldBe null
        if (getPendingDecision() is com.wingedsheep.engine.core.ReorderLibraryDecision) {
            keepLibraryOrder().error shouldBe null
        }
    }

    init {
        test("controller scries 2, then the opponent may scry 1 in their own library") {
            val game = setup()
            game.castAndControllerKeepsScry()

            val ask = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            ask.playerId shouldBe game.player2Id
            game.answerYesNo(true).error shouldBe null

            val oppScry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            oppScry.playerId shouldBe game.player2Id
            oppScry.options.size shouldBe 1
            val top = oppScry.options.single()
            val libBefore = game.state.getLibrary(game.player2Id)
            val indexBefore = libBefore.indexOf(top)
            indexBefore shouldNotBe -1

            game.selectCards(listOf(top)).error shouldBe null
            val libAfter = game.state.getLibrary(game.player2Id)
            libAfter.size shouldBe 3
            libAfter.indexOf(top) shouldBe (if (indexBefore == 0) 2 else 0)
            game.getPendingDecision() shouldBe null
            game.findPermanent("Myr Custodian") shouldNotBe null
        }

        test("opponent declining leaves their library untouched") {
            val game = setup()
            val oppLibrary = game.state.getLibrary(game.player2Id)
            game.castAndControllerKeepsScry()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player2Id
            game.answerYesNo(false).error shouldBe null

            game.getPendingDecision() shouldBe null
            game.state.getLibrary(game.player2Id) shouldBe oppLibrary
        }
    }
}
