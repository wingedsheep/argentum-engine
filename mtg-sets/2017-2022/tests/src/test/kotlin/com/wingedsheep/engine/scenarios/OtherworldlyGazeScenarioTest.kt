package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Otherworldly Gaze (MID #67) — "Surveil 3. Flashback {1}{U}"
 */
class OtherworldlyGazeScenarioTest : ScenarioTestBase() {

    private fun setup(fromGraveyard: Boolean) = scenario()
        .withPlayers("Player1", "Player2")
        .let { if (fromGraveyard) it.withCardInGraveyard(1, "Otherworldly Gaze") else it.withCardInHand(1, "Otherworldly Gaze") }
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Shock") // fourth card — out of reach
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Otherworldly Gaze") {

            test("surveils the top three cards") {
                val game = setup(fromGraveyard = false)
                game.castSpell(1, "Otherworldly Gaze").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("exactly the top three are looked at") {
                    decision.options.size shouldBe 3
                }
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()
                val giant = game.findCardsInLibrary(1, "Hill Giant").single()
                game.selectCards(listOf(bolt, giant)).error shouldBe null
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack()

                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                withClue("Grizzly Bears stayed on top") {
                    game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Grizzly Bears").single()
                }
            }

            test("flashback from the graveyard surveils and exiles the card") {
                val game = setup(fromGraveyard = true)
                game.castSpellFromGraveyard(1, "Otherworldly Gaze").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(game.findCardsInLibrary(1, "Lightning Bolt").single())).error shouldBe null
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack()

                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInExile(1, "Otherworldly Gaze") shouldBe true
            }
        }
    }
}
