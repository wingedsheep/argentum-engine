package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Hullbreaker Horror (VOW #63) — {5}{U}{U} Creature — Kraken Horror, 7/8.
 *
 *   Flash
 *   This spell can't be countered.
 *   Whenever you cast a spell, choose up to one —
 *   • Return target spell you don't control to its owner's hand.
 *   • Return target nonland permanent to its owner's hand.
 *
 * Exercises the printed Flash + can't-be-countered flags on the card definition, and the modal
 * "choose up to one" cast trigger: casting a further spell offers the two modes plus a decline
 * option, and choosing "return target nonland permanent" bounces a permanent to its owner's hand.
 */
class HullbreakerHorrorScenarioTest : ScenarioTestBase() {

    init {
        context("Hullbreaker Horror") {

            test("has Flash and can't be countered") {
                val card = cardRegistry.getCard("Hullbreaker Horror")!!
                withClue("Flash is a printed keyword") {
                    card.keywords.contains(Keyword.FLASH) shouldBe true
                }
                withClue("the spell can't be countered") {
                    card.script.cantBeCountered shouldBe true
                }
            }

            test("casting a further spell offers the modal choice; bouncing a nonland permanent returns it to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hullbreaker Horror", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Grizzly Bears") // target for the bounce mode
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null

                // The cast trigger resolves before the Bolt itself, offering the modal choice.
                game.resolveStack()
                val modeDecision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("Expected a ChooseOptionDecision; got ${game.getPendingDecision()}")
                withClue("both bounce modes plus a decline option are offered") {
                    modeDecision.options.size shouldBe 3
                }
                val bounceModeIndex = modeDecision.options.indexOfFirst { it.contains("nonland permanent") }
                withClue("the 'return target nonland permanent' mode is offered") {
                    (bounceModeIndex >= 0) shouldBe true
                }
                game.submitDecision(OptionChosenResponse(modeDecision.id, bounceModeIndex))

                withClue("the mode asks for a target nonland permanent") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Grizzly Bears was returned to its owner's hand") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
