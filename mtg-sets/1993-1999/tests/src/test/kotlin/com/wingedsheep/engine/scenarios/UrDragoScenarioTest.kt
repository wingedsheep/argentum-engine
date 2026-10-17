package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ur-Drago (LEG) — {3}{U}{U}{B}{B} Legendary Creature — Elemental 4/4: "First strike / Creatures
 * with swampwalk can be blocked as though they didn't have swampwalk."
 */
class UrDragoScenarioTest : ScenarioTestBase() {

    init {
        context("Ur-Drago") {
            test("blocks a swampwalker despite its controller's Swamp and kills it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Marsh Goblins")
                    .withCardOnBattlefield(2, "Ur-Drago")
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Marsh Goblins" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Ur-Drago" to listOf("Marsh Goblins"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.isInGraveyard(1, "Marsh Goblins") shouldBe true
                game.isOnBattlefield("Ur-Drago") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
