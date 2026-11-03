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
 * Sanguine Glorifier (RIX #20) — "When this creature enters, put a +1/+1 counter on another target Vampire you control."
 */
class SanguineGlorifierScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("entering puts a +1/+1 counter on another Vampire you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sanguine Glorifier")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Vampire Interloper")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ally = game.findPermanent("Vampire Interloper")!!
            game.castSpell(1, "Sanguine Glorifier").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as ChooseTargetsDecision
            decision.legalTargets.values.flatten().toSet() shouldBe setOf(ally)
            game.selectTargets(listOf(ally)).error shouldBe null
            game.resolveStack()

            game.plusOnes(ally) shouldBe 1
            game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
            game.plusOnes(game.findPermanent("Sanguine Glorifier")!!) shouldBe 0
        }
    }
}
