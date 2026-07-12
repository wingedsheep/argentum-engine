package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Undead Butler (VOW #133) — {1}{B} Creature — Zombie, 1/2.
 *
 *   When this creature enters, mill three cards.
 *   When this creature dies, you may exile it. When you do, return target creature card from
 *   your graveyard to your hand.
 *
 * Exercises the ETB mill-three and the dies-trigger optional exile-then-return: exiling Undead
 * Butler from the graveyard and returning a targeted creature card to hand; declining the exile
 * leaves everything untouched.
 */
class UndeadButlerScenarioTest : ScenarioTestBase() {

    init {
        context("Undead Butler") {

            test("entering the battlefield mills three cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Undead Butler")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Undead Butler").error shouldBe null
                game.resolveStack()

                withClue("Undead Butler resolved onto the battlefield") {
                    game.isOnBattlefield("Undead Butler") shouldBe true
                }
                withClue("three cards were milled to the graveyard") {
                    game.librarySize(1) shouldBe 0
                    game.graveyardSize(1) shouldBe 3
                }
            }

            test("dies, exiling it returns a targeted creature card from the graveyard") {
                // Undead Butler is a black 1/2 — Doom Blade can't target black creatures, so use
                // Lightning Bolt (3 damage is lethal).
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Undead Butler", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val butler = game.findPermanent("Undead Butler")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.castSpell(1, "Lightning Bolt", targetId = butler).error shouldBe null
                game.resolveStack()

                withClue("Undead Butler died to the graveyard") {
                    game.isInGraveyard(1, "Undead Butler") shouldBe true
                }

                // "You may exile it." — accept.
                if (game.hasPendingDecision()) game.answerYesNo(true)
                // The reflexive "When you do" trigger now wants a target creature card in the graveyard.
                if (game.hasPendingDecision()) game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Undead Butler was exiled instead of staying in the graveyard") {
                    game.isInGraveyard(1, "Undead Butler") shouldBe false
                    game.isInExile(1, "Undead Butler") shouldBe true
                }
                withClue("Grizzly Bears was returned to hand") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("declining the exile leaves Undead Butler in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Undead Butler", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val butler = game.findPermanent("Undead Butler")!!
                game.castSpell(1, "Lightning Bolt", targetId = butler).error shouldBe null
                game.resolveStack()

                if (game.hasPendingDecision()) game.answerYesNo(false)
                game.resolveStack()

                withClue("Undead Butler stays in the graveyard when declining the exile") {
                    game.isInGraveyard(1, "Undead Butler") shouldBe true
                }
                withClue("Grizzly Bears stays in the graveyard too") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
