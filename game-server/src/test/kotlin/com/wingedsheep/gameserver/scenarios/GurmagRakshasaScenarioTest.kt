package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Gurmag Rakshasa (TDM) — {4}{B}{B} Creature — Demon, 5/5.
 *
 * "Menace
 *  When this creature enters, target creature an opponent controls gets -2/-2 until end of
 *  turn and target creature you control gets +2/+2 until end of turn."
 *
 * Exercises the two-target ETB trigger: one target on an opponent's creature (-2/-2) and one on
 * a creature you control (+2/+2), each its own `Effects.ModifyStats` until end of turn.
 */
class GurmagRakshasaScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Gurmag Rakshasa enters-the-battlefield trigger") {

            test("shrinks an opponent's creature and pumps one of yours") {
                // Player 1 controls Grizzly Bears (2/2) to pump; Player 2 controls Hill Giant (3/3) to shrink.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gurmag Rakshasa")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ownBears = game.findPermanent("Grizzly Bears")!!
                val oppGiant = game.findPermanent("Hill Giant")!!

                val cast = game.castSpell(1, "Gurmag Rakshasa")
                withClue("Cast should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // ETB trigger goes on the stack and asks for both targets in one decision:
                // slot 0 = opponent's creature (-2/-2), slot 1 = your creature (+2/+2).
                val decisionId = game.getPendingDecision()?.id
                    ?: error("expected a target-selection decision for the ETB trigger")
                game.submitDecision(
                    com.wingedsheep.engine.core.TargetsResponse(
                        decisionId,
                        mapOf(0 to listOf(oppGiant), 1 to listOf(ownBears))
                    )
                )
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Hill Giant (3/3) gets -2/-2 → 1/1") {
                    projected.getPower(oppGiant) shouldBe 1
                    projected.getToughness(oppGiant) shouldBe 1
                }
                withClue("Grizzly Bears (2/2) gets +2/+2 → 4/4") {
                    projected.getPower(ownBears) shouldBe 4
                    projected.getToughness(ownBears) shouldBe 4
                }
                withClue("Gurmag Rakshasa itself resolves onto the battlefield") {
                    game.isOnBattlefield("Gurmag Rakshasa") shouldBe true
                }
            }
        }
    }
}
