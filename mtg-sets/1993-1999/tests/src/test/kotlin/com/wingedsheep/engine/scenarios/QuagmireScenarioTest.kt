package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Quagmire (LEG) — {2}{B} Enchantment: "Creatures with swampwalk can be blocked as though they
 * didn't have swampwalk."
 */
class QuagmireScenarioTest : ScenarioTestBase() {

    private fun swampwalkerAttacksInto(quagmireController: Int?, defenderLand: String = "Swamp"): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Marsh Goblins")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withLandsOnBattlefield(2, defenderLand, 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (quagmireController != null) builder.withCardOnBattlefield(quagmireController, "Quagmire")
        val game = builder.build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Marsh Goblins" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        context("Quagmire") {
            test("a swampwalker can be blocked by a player who controls a Swamp") {
                val game = swampwalkerAttacksInto(quagmireController = 2)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Marsh Goblins"))).error shouldBe null
            }

            test("it also works when the swampwalker's controller has the Quagmire") {
                val game = swampwalkerAttacksInto(quagmireController = 1)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Marsh Goblins"))).error shouldBe null
            }

            test("without Quagmire, swampwalk keeps the attacker unblockable") {
                val game = swampwalkerAttacksInto(quagmireController = null)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Marsh Goblins"))).error shouldNotBe null
            }
        }
    }
}
