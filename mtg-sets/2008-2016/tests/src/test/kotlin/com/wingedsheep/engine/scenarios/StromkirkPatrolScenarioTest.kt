package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Stromkirk Patrol (ISD #118) — {4}{B} 4/3 Vampire Soldier.
 *
 *   Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 */
class StromkirkPatrolScenarioTest : ScenarioTestBase() {

    init {
        test("dealing combat damage to a player puts a +1/+1 counter on it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Stromkirk Patrol")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val patrol = game.findPermanent("Stromkirk Patrol")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Stromkirk Patrol" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers()
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 16
            game.state.getEntity(patrol)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.projectedState.getPower(patrol) shouldBe 5
            game.state.projectedState.getToughness(patrol) shouldBe 4
        }
    }
}
