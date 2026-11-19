package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Pillar of War: "Defender. As long as this creature is enchanted, it can attack as though it
 * didn't have defender."
 */
class PillarOfWarScenarioTest : ScenarioTestBase() {

    init {
        test("can't attack while unenchanted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Pillar of War", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Pillar of War" to 2)).error.shouldNotBeNull()
        }

        test("can attack while enchanted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Pillar of War", summoningSickness = false)
                .withCardAttachedTo(1, "Claim of Erebos", "Pillar of War")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Pillar of War" to 2)).error shouldBe null
        }
    }
}
