package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Symmetry Matrix (BRO #252).
 *
 * {4} Artifact — "Whenever a creature you control with power equal to its toughness enters, you
 * may pay {1}. If you do, draw a card."
 *
 * The "power equal to its toughness" qualifier is a trigger-time `triggerRestriction` comparing the
 * entering creature's power with its toughness; these tests pin that a square creature triggers
 * and a lopsided one does not.
 */
class SymmetryMatrixScenarioTest : ScenarioTestBase() {

    private val squareBeast = card("Square Beast") {
        manaCost = "{0}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
    }

    private val lopsidedBeast = card("Lopsided Beast") {
        manaCost = "{0}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 3
    }

    init {
        cardRegistry.register(squareBeast)
        cardRegistry.register(lopsidedBeast)

        context("Symmetry Matrix") {

            test("a creature with power equal to its toughness entering offers pay {1} to draw") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Symmetry Matrix")
                    .withCardInHand(1, "Square Beast")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castSpell(1, "Square Beast").error shouldBe null
                game.resolveStack()

                withClue("Square Beast is on the battlefield") {
                    game.isOnBattlefield("Square Beast") shouldBe true
                }
                withClue("Symmetry Matrix offers the may-pay {1} decision") {
                    game.hasPendingDecision() shouldBe true
                }

                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Paying {1} draws a card (cast −1, draw +1)") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("a creature whose power differs from its toughness does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Symmetry Matrix")
                    .withCardInHand(1, "Lopsided Beast")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Lopsided Beast").error shouldBe null
                game.resolveStack()

                withClue("Lopsided Beast is on the battlefield") {
                    game.isOnBattlefield("Lopsided Beast") shouldBe true
                }
                withClue("A 1/3 must not trigger Symmetry Matrix") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
