package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rubblebelt Recluse (RNA #111) — {4}{R} 6/5 Creature — Ogre Berserker.
 *
 *   This creature attacks each combat if able.
 */
class RubblebeltRecluseScenarioTest : ScenarioTestBase() {

    init {
        context("Rubblebelt Recluse") {
            test("must attack when able") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rubblebelt Recluse")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(emptyMap()).error shouldNotBe null
                game.declareAttackers(mapOf("Rubblebelt Recluse" to 2)).error shouldBe null
            }
        }
    }
}
