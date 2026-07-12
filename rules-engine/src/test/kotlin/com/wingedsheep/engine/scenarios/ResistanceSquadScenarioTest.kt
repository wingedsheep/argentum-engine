package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Resistance Squad (VOW #32) — {2}{W} Creature — Human Soldier, 3/2.
 *
 *   When this creature enters, if you control another Human, draw a card.
 *
 * Exercises the ETB intervening-if gated on `Conditions.YouControl(Human, excludeSelf = true)`:
 * the draw fires only when another Human is already on the battlefield.
 */
class ResistanceSquadScenarioTest : ScenarioTestBase() {

    init {
        context("Resistance Squad ETB draw") {

            test("draws a card when you already control another Human") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Resistance Squad")
                    .withCardOnBattlefield(1, "Traveling Minister") // Human Cleric
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpell(1, "Resistance Squad").error shouldBe null
                game.resolveStack()

                withClue("Resistance Squad enters the battlefield") {
                    game.isOnBattlefield("Resistance Squad") shouldBe true
                }
                // Hand: -1 (Resistance Squad leaves hand) + 1 (drawn card) = net 0.
                withClue("Controlling another Human triggers the draw") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("does not draw a card when you control no other Human") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Resistance Squad")
                    .withCardOnBattlefield(1, "Grizzly Bears") // not a Human
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpell(1, "Resistance Squad").error shouldBe null
                game.resolveStack()

                withClue("Resistance Squad enters the battlefield") {
                    game.isOnBattlefield("Resistance Squad") shouldBe true
                }
                // Hand: -1 (Resistance Squad leaves hand), no draw.
                withClue("No other Human means no draw") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }
    }
}
