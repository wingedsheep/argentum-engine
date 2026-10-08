package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gnawing Vermin (BRO #101) — {B} Creature — Rat, 1/1.
 *
 * "When this creature enters, target player mills two cards.
 * When this creature dies, target creature you don't control gets -1/-1 until end of turn."
 */
class GnawingVerminScenarioTest : ScenarioTestBase() {

    init {
        context("Gnawing Vermin") {

            test("entering makes the target player mill two cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gnawing Vermin")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gnawing Vermin").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(game.player2Id))
                }
                game.resolveStack()

                withClue("Player2 milled exactly two cards") {
                    game.graveyardSize(2) shouldBe 2
                    game.librarySize(2) shouldBe 1
                }
            }

            test("dying gives target creature you don't control -1/-1 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gnawing Vermin")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vermin = game.findPermanent("Gnawing Vermin")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Lightning Bolt", vermin).error shouldBe null
                game.resolveStack()
                val decision = game.state.pendingDecision as ChooseTargetsDecision
                decision.legalTargets.values.flatten().toSet() shouldBe setOf(bears)
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Gnawing Vermin died") {
                    game.isInGraveyard(1, "Gnawing Vermin") shouldBe true
                }
                withClue("the opposing Grizzly Bears is now 1/1") {
                    game.state.projectedState.getPower(bears) shouldBe 1
                    game.state.projectedState.getToughness(bears) shouldBe 1
                }
            }
        }
    }
}
