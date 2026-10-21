package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Arborea Pegasus (AFR #2) — {3}{W} Creature — Pegasus 2/3, flying.
 *
 *   When this creature enters, target creature gets +1/+1 and gains flying until end of turn.
 */
class ArboreaPegasusScenarioTest : ScenarioTestBase() {

    init {
        test("entering gives the target creature +1/+1 and flying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Arborea Pegasus")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Arborea Pegasus").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }
    }
}
