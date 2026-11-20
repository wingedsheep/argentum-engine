package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Woodlot Crawler (DGM #118) — {U}{B} 2/1 Creature — Insect.
 *
 *   Forestwalk
 *   Protection from green
 */
class WoodlotCrawlerScenarioTest : ScenarioTestBase() {

    private fun attack(defenderLand: String, blocker: String): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Woodlot Crawler")
            .withLandsOnBattlefield(2, defenderLand, 1)
            .withCardOnBattlefield(2, blocker)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Woodlot Crawler" to 2)).error shouldBe null
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        test("forestwalk: unblockable while the defender controls a Forest") {
            val game = attack("Forest", "Hill Giant")
            withClue("Hill Giant can't block it") {
                game.declareBlockers(mapOf("Hill Giant" to listOf("Woodlot Crawler"))).error shouldNotBe null
            }
        }

        test("without a Forest a nongreen creature can block it") {
            val game = attack("Mountain", "Hill Giant")
            game.declareBlockers(mapOf("Hill Giant" to listOf("Woodlot Crawler"))).error shouldBe null
        }

        test("protection from green: a green creature can't block it") {
            val game = attack("Mountain", "Grizzly Bears")
            withClue("Grizzly Bears can't block a creature with protection from green") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Woodlot Crawler"))).error shouldNotBe null
            }
        }
    }
}
