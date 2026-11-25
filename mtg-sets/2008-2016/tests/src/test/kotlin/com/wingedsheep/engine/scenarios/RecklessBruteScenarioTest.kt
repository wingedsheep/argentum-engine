package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reckless Brute (M13 #144) — {2}{R} 3/1 Creature — Ogre Warrior.
 *   Haste
 *   This creature attacks each combat if able.
 */
class RecklessBruteScenarioTest : ScenarioTestBase() {
    init {
        test("attacks the turn it arrives, and must attack") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Reckless Brute", summoningSickness = true)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("declaring no attackers is illegal: the hasty Brute must attack") {
                game.execute(DeclareAttackers(game.player1Id, emptyMap())).error shouldNotBe null
            }
            game.declareAttackers(mapOf("Reckless Brute" to 2)).error shouldBe null
        }
    }
}
