package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Aeronaut Cavalry (BRO #1) — "When this creature enters, put a +1/+1 counter on another target
 * Soldier you control."
 */
class AeronautCavalryScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("entering puts a +1/+1 counter on another Soldier you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Aeronaut Cavalry")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Air Marshal")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val marshal = game.findPermanent("Air Marshal")!!
            game.castSpell(1, "Aeronaut Cavalry").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as ChooseTargetsDecision
            decision.legalTargets.values.flatten().toSet() shouldBe setOf(marshal)
            game.selectTargets(listOf(marshal)).error shouldBe null
            game.resolveStack()

            game.plusOnes(marshal) shouldBe 1
            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
            game.plusOnes(game.findPermanent("Aeronaut Cavalry")!!) shouldBe 0
        }
    }
}
