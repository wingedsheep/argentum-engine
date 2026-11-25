package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Goblin Battle Jester (M13 #135) — {3}{R} 2/2 Creature — Goblin.
 *   Whenever you cast a red spell, target creature can't block this turn.
 */
class GoblinBattleJesterScenarioTest : ScenarioTestBase() {
    init {
        test("casting a red spell stops the targeted creature from blocking this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Goblin Battle Jester")
                .withCardInHand(1, "Goblin Arsonist")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val result = game.castSpell(1, "Goblin Arsonist")
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            withClue("the Jester's trigger asks for a target") { game.hasPendingDecision() shouldBe true }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Goblin Battle Jester" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Grizzly Bears can't block this turn") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Goblin Battle Jester"))).error shouldNotBe null
            }
        }
    }
}
