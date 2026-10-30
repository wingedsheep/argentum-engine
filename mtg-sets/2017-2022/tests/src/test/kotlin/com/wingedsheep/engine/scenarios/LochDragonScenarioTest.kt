package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Loch Dragon (ELD #211) — {U/R}{U/R}{U/R}{U/R} Creature — Dragon 3/2.
 *
 * "Flying. Whenever this creature enters or attacks, you may discard a card. If you do, draw a card."
 */
class LochDragonScenarioTest : ScenarioTestBase() {

    init {
        context("Loch Dragon") {

            test("on entering, discarding a card draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Loch Dragon")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Mountain")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Loch Dragon").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears")).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
                game.handSize(1) shouldBe 2
            }

            test("on attacking, declining the discard draws nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loch Dragon")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Loch Dragon" to 2)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe false
            }
        }
    }
}
