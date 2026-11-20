package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Akroan Hoplite (THS #185) — {R}{W} Creature — Human Soldier 1/2.
 *
 *   Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the number of
 *   attacking creatures you control.
 */
class AkroanHopliteScenarioTest : ScenarioTestBase() {

    private fun attackWith(attackers: List<String>): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Akroan Hoplite")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        val game = builder.build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(attackers.associateWith { 2 }).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("attacking alongside another creature, it gets +2/+0 (both attackers count, itself included)") {
            val game = attackWith(listOf("Akroan Hoplite", "Grizzly Bears"))
            val hoplite = game.findPermanent("Akroan Hoplite")!!

            game.state.projectedState.getPower(hoplite) shouldBe 3
            game.state.projectedState.getToughness(hoplite) shouldBe 2
        }

        test("attacking alone, it counts only itself and gets +1/+0; a non-attacker doesn't count") {
            val game = attackWith(listOf("Akroan Hoplite"))
            val hoplite = game.findPermanent("Akroan Hoplite")!!

            game.state.projectedState.getPower(hoplite) shouldBe 2
            game.state.projectedState.getToughness(hoplite) shouldBe 2
        }
    }
}
