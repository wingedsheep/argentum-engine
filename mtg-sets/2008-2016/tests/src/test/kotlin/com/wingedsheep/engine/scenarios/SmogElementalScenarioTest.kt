package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Smog Elemental (GTC) — Creatures with flying your opponents control get -1/-1. */
class SmogElementalScenarioTest : ScenarioTestBase() {
    init {
        test("shrinks only opponents' flying creatures, not its own or ground creatures") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Smog Elemental")
                .withCardOnBattlefield(1, "Wind Drake")
                .withCardOnBattlefield(2, "Wind Drake")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val projected = game.state.projectedState
            val smog = game.findPermanent("Smog Elemental")!!
            val drakes = game.findPermanents("Wind Drake")
            val myDrake = drakes.single { projected.getController(it) == game.player1Id }
            val theirDrake = drakes.single { projected.getController(it) == game.player2Id }
            val bears = game.findPermanent("Grizzly Bears")!!

            projected.getPower(theirDrake) shouldBe 1
            projected.getToughness(theirDrake) shouldBe 1
            projected.getPower(myDrake) shouldBe 2
            projected.getToughness(myDrake) shouldBe 2
            projected.getPower(bears) shouldBe 2
            projected.getPower(smog) shouldBe 3
        }

        test("an opponent's 0/1 flier dies") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Smog Elemental")
                .withCardOnBattlefield(2, "Birds of Paradise")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.checkStateBasedActions()

            game.isOnBattlefield("Birds of Paradise") shouldBe false
            game.isInGraveyard(2, "Birds of Paradise") shouldBe true
        }
    }
}
