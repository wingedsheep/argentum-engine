package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Zulaport Duelist (ZNR #88) — {U} Creature — Human Rogue, 1/1.
 *
 *   Flash
 *   When this creature enters, up to one target creature gets -2/-0 until end of turn. Its
 *   controller mills two cards.
 *
 * Covers that the mill lands on the *target's* controller, and that declining the optional
 * target mills nobody (the mill's own player fallback would otherwise mill the Duelist's controller).
 */
class ZulaportDuelistScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Zulaport Duelist")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Zulaport Duelist ETB") {

            test("targeted creature gets -2/-0 and its controller mills two") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Zulaport Duelist").error shouldBe null
                game.resolveStack()

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears shrunk to 0/2") {
                    game.state.projectedState.getPower(bears) shouldBe 0
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
                withClue("target's controller milled two") { game.graveyardSize(2) shouldBe 2 }
                withClue("Duelist's controller milled nothing") { game.graveyardSize(1) shouldBe 0 }
            }

            test("targeting your own creature mills you") {
                val game = board()
                val duelistSelfTarget = { game.findPermanent("Zulaport Duelist")!! }

                game.castSpell(1, "Zulaport Duelist").error shouldBe null
                game.resolveStack()

                game.selectTargets(listOf(duelistSelfTarget())).error shouldBe null
                game.resolveStack()

                withClue("Duelist's controller milled two") { game.graveyardSize(1) shouldBe 2 }
                withClue("opponent milled nothing") { game.graveyardSize(2) shouldBe 0 }
            }

            test("declining the target mills nobody") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Zulaport Duelist").error shouldBe null
                game.resolveStack()

                game.skipTargets().error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears unchanged") { game.state.projectedState.getPower(bears) shouldBe 2 }
                withClue("no one milled") {
                    game.graveyardSize(1) shouldBe 0
                    game.graveyardSize(2) shouldBe 0
                }
            }
        }
    }
}
