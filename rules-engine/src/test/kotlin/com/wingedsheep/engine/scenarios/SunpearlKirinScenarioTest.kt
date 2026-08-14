package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Sunpearl Kirin. */
class SunpearlKirinScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Sunpearl Kirin") {

            test("returns a chosen nontoken permanent to hand and draws no card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sunpearl Kirin")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Sunpearl Kirin")
                withClue("Casting Sunpearl Kirin should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack() // Kirin enters → ETB trigger on stack, asks for a target.

                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("The chosen Grizzly Bears returns to its owner's hand") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("Grizzly Bears left the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                // Cast Kirin (-1 hand), bounced Bears (+1 hand) → net unchanged; no extra draw (nontoken).
                withClue("Returning a nontoken permanent draws no card") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("The library card was not drawn") {
                    game.findCardsInLibrary(1, "Mountain").size shouldBe 1
                }
            }

            test("returning a token draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sunpearl Kirin")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false, isToken = true)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Sunpearl Kirin")
                withClue("Casting Sunpearl Kirin should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val tokenBears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(tokenBears))
                game.resolveStack()

                withClue("The token ceases to exist when it leaves the battlefield (not in hand)") {
                    game.findCardsInHand(1, "Grizzly Bears").size shouldBe 0
                }
                withClue("The token left the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                // Cast Kirin (-1 hand), token doesn't go to hand, but the token clause draws (+1).
                withClue("Returning a token draws a card, so hand size is unchanged after casting Kirin") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("The library card was drawn by the token clause") {
                    game.findCardsInLibrary(1, "Mountain").size shouldBe 0
                }
            }
        }
    }
}
