package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rubblebelt Rioters (WAR #215) — {1}{R}{G} Creature — Human Berserker 0/4, haste.
 *
 *   Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the greatest
 *   power among creatures you control.
 */
class RubblebeltRiotersScenarioTest : ScenarioTestBase() {

    private fun attackWith(withGiant: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Rubblebelt Rioters", summoningSickness = true)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withGiant) builder.withCardOnBattlefield(1, "Hill Giant")
        val game = builder.build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Rubblebelt Rioters" to 2)).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("haste lets it attack the turn it arrives, pumped by the greatest power you control") {
            val game = attackWith(withGiant = true)
            val rioters = game.findPermanent("Rubblebelt Rioters")!!

            game.state.projectedState.hasKeyword(rioters, Keyword.HASTE) shouldBe true
            game.state.projectedState.getPower(rioters) shouldBe 3
            game.state.projectedState.getToughness(rioters) shouldBe 4
        }

        test("alone, X is its own power of 0") {
            val game = attackWith(withGiant = false)
            val rioters = game.findPermanent("Rubblebelt Rioters")!!

            game.state.projectedState.getPower(rioters) shouldBe 0
        }
    }
}
