package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MaChaoWesternWarriorScenarioTest : ScenarioTestBase() {
    init {
        context("Ma Chao, Western Warrior") {
            test("attacking alone makes it unblockable this combat") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Ma Chao, Western Warrior")
                    .withCardOnBattlefield(2, "Shu Cavalry")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ma Chao, Western Warrior" to 2)).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val r = game.declareBlockers(mapOf("Shu Cavalry" to listOf("Ma Chao, Western Warrior")))
                withClue("horsemanship blocker still can't block an unblockable attacker") { r.error shouldNotBe null }
            }

            test("attacking with another creature leaves horsemanship blockers able to block") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Ma Chao, Western Warrior")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Shu Cavalry")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ma Chao, Western Warrior" to 2, "Hill Giant" to 2)).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Shu Cavalry" to listOf("Ma Chao, Western Warrior"))).error shouldBe null
            }
        }
    }
}
