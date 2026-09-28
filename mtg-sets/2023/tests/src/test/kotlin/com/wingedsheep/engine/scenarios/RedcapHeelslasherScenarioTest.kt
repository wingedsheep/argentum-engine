package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/** Redcap Heelslasher — backup 1: counters on the target; another creature also gains the ability until end of turn. */
class RedcapHeelslasherScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun cast(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Redcap Heelslasher")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Redcap Heelslasher").error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Redcap Heelslasher") {
            test("backup on another creature: counters and the ability") {
                val game = cast()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                plusOnes(game, bears) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
            }

            test("backup on itself: counters only") {
                val game = cast()
                val self = game.findPermanent("Redcap Heelslasher")!!
                game.selectTargets(listOf(self)).error shouldBe null
                game.resolveStack()
                plusOnes(game, self) shouldBe 1
                game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.FIRST_STRIKE) shouldBe false
            }
        }
    }
}
