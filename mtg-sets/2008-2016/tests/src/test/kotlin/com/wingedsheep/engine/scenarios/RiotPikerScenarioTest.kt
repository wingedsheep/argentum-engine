package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Riot Piker (DGM #37) — {1}{R} 2/1 Creature — Goblin Berserker.
 *
 *   First strike
 *   This creature attacks each combat if able.
 */
class RiotPikerScenarioTest : ScenarioTestBase() {
    init {
        test("has first strike and must attack each combat if able") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Riot Piker")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val piker = game.findPermanent("Riot Piker")!!

            withClue("first strike") {
                game.state.projectedState.hasKeyword(piker, Keyword.FIRST_STRIKE) shouldBe true
            }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val noAttack = game.execute(DeclareAttackers(playerId = game.player1Id, attackers = emptyMap()))
            withClue("declaring no attackers is illegal") { noAttack.error shouldNotBe null }
            game.declareAttackers(mapOf("Riot Piker" to 2)).error shouldBe null
        }
    }
}
