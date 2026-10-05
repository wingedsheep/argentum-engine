package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Eidolon of Rhetoric (JOU #10) — {2}{W} Enchantment Creature — Spirit 1/4.
 *
 *   Each player can't cast more than one spell each turn.
 *
 * Covers the 2014-04-26 ruling (the Eidolon itself counts as the turn's one spell) and the
 * global reach of the restriction (the opponent is capped too).
 */
class EidolonOfRhetoricScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.canCast(playerNumber: Int, cardName: String): Boolean {
        val cardId = findCardsInHand(playerNumber, cardName).first()
        return getLegalActions(playerNumber).any {
            val a = it.action
            a is CastSpell && a.cardId == cardId
        }
    }

    init {
        context("Eidolon of Rhetoric") {

            test("casting the Eidolon uses up its controller's one spell for the turn") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Eidolon of Rhetoric")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Eidolon of Rhetoric").outcome shouldBe Outcome.Done
                game.resolveStack()

                withClue("Eidolon of Rhetoric should be on the battlefield") {
                    game.isOnBattlefield("Eidolon of Rhetoric") shouldBe true
                }
                withClue("The Eidolon was this turn's spell, so Shock can't be cast") {
                    game.canCast(1, "Shock") shouldBe false
                }
            }

            test("the opponent is capped at one spell per turn as well") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Eidolon of Rhetoric")
                    .withCardsInHand(2, "Shock", 2)
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("The first spell of the turn is castable") {
                    game.canCast(2, "Shock") shouldBe true
                }
                game.castSpellTargetingPlayer(2, "Shock", 1).outcome shouldBe Outcome.Done
                game.resolveStack()

                withClue("The second spell of the turn is blocked") {
                    game.canCast(2, "Shock") shouldBe false
                }
            }
        }
    }
}
