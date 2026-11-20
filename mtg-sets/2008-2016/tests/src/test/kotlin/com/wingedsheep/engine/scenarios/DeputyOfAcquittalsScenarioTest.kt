package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Deputy of Acquittals (DGM #65) — {W}{U} 2/2 Creature — Human Wizard.
 *
 *   Flash
 *   When this creature enters, you may return another target creature you control to its owner's hand.
 */
class DeputyOfAcquittalsScenarioTest : ScenarioTestBase() {

    private fun castDeputyAtFlashSpeed(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(1, "Deputy of Acquittals")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Island", 1)
            .withActivePlayer(2)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val cast = game.castSpell(1, "Deputy of Acquittals")
        withClue("flash: castable on the opponent's turn: ${cast.error}") { cast.error shouldBe null }
        game.resolveStack()

        val bears = game.findPermanent("Grizzly Bears")!!
        withClue("only another creature you control is a legal target") {
            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0] shouldBe listOf(bears)
        }
        game.selectTargets(listOf(bears)).error shouldBe null
        game.resolveStack()
        game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        return game
    }

    init {
        test("accepting returns the targeted creature to its owner's hand") {
            val game = castDeputyAtFlashSpeed()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Deputy of Acquittals") shouldBe true
        }

        test("declining leaves the creature on the battlefield") {
            val game = castDeputyAtFlashSpeed()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isInHand(1, "Grizzly Bears") shouldBe false
        }
    }
}
