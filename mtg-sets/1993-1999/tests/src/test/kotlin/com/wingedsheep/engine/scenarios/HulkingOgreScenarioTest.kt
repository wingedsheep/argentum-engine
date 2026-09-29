package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HulkingOgreScenarioTest : ScenarioTestBase() {
    init {
        test("Hulking Ogre cannot block while another creature can") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Hulking Ogre")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Hulking Ogre" to listOf("Grizzly Bears"))).error shouldNotBe null
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
        }

        test("Hulking Ogre can attack") {
            val game = scenario()
                .withPlayers("Attacker", "Defender")
                .withCardOnBattlefield(1, "Hulking Ogre", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Hulking Ogre" to 2)).error shouldBe null
        }
    }
}
