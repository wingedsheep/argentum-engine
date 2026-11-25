package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Harbor Bandit (M13 #95) — {2}{B} 2/2 Creature — Human Rogue.
 *   This creature gets +1/+1 as long as you control an Island.
 *   {1}{U}: This creature can't be blocked this turn.
 */
class HarborBanditScenarioTest : ScenarioTestBase() {
    init {
        val evadeId = cardRegistry.getCard("Harbor Bandit")!!.script.activatedAbilities.single().id

        test("is 2/2 without an Island") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Harbor Bandit")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bandit = game.findPermanent("Harbor Bandit")!!
            game.state.projectedState.getPower(bandit) shouldBe 2
            game.state.projectedState.getToughness(bandit) shouldBe 2
        }

        test("an Island makes it 3/3, and {1}{U} makes it unblockable this turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Harbor Bandit")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bandit = game.findPermanent("Harbor Bandit")!!
            game.state.projectedState.getPower(bandit) shouldBe 3
            game.state.projectedState.getToughness(bandit) shouldBe 3

            val result = game.execute(ActivateAbility(game.player1Id, bandit, evadeId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Harbor Bandit" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Grizzly Bears can't block it") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Harbor Bandit"))).error shouldNotBe null
            }
        }
    }
}
