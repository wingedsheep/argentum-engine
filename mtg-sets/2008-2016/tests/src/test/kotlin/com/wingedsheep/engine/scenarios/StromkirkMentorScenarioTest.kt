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
 * Stromkirk Mentor (SOI #137) — {3}{B} 4/2 Vampire Soldier.
 *
 *   When this creature enters, put a +1/+1 counter on another target Vampire you control.
 */
class StromkirkMentorScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("entering puts a +1/+1 counter on another Vampire you control — only yours are legal") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Stromkirk Mentor")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardOnBattlefield(1, "Vampire Interloper")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Bloodthrone Vampire")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ally = game.findPermanent("Vampire Interloper")!!
            game.castSpell(1, "Stromkirk Mentor").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision as ChooseTargetsDecision
            decision.legalTargets.values.flatten().toSet() shouldBe setOf(ally)
            game.selectTargets(listOf(ally)).error shouldBe null
            game.resolveStack()

            game.plusOnes(ally) shouldBe 1
            game.plusOnes(game.findPermanent("Stromkirk Mentor")!!) shouldBe 0
            game.plusOnes(game.findPermanent("Bloodthrone Vampire")!!) shouldBe 0
        }
    }
}
