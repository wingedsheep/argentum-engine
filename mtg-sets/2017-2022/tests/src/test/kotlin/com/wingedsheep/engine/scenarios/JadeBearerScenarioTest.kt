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
 * Jade Bearer (RIX #134) — "When this creature enters, put a +1/+1 counter on another target Merfolk you control."
 */
class JadeBearerScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("entering puts a +1/+1 counter on another Merfolk you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Jade Bearer")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(1, "Merfolk Looter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ally = game.findPermanent("Merfolk Looter")!!
            game.castSpell(1, "Jade Bearer").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as ChooseTargetsDecision
            decision.legalTargets.values.flatten().toSet() shouldBe setOf(ally)
            game.selectTargets(listOf(ally)).error shouldBe null
            game.resolveStack()

            game.plusOnes(ally) shouldBe 1
            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
            game.plusOnes(game.findPermanent("Jade Bearer")!!) shouldBe 0
        }
    }
}
