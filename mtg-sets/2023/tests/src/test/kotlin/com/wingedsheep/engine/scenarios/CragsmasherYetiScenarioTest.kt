package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/** Cragsmasher Yeti — backup 2: counters on the target; another creature also gains the ability until end of turn. */
class CragsmasherYetiScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun cast(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Cragsmasher Yeti")
            .withLandsOnBattlefield(1, "Mountain", 6)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Cragsmasher Yeti").error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Cragsmasher Yeti") {
            test("backup on another creature: counters and the ability") {
                val game = cast()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                plusOnes(game, bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }

            test("backup on itself: counters only") {
                val game = cast()
                val self = game.findPermanent("Cragsmasher Yeti")!!
                game.selectTargets(listOf(self)).error shouldBe null
                game.resolveStack()
                plusOnes(game, self) shouldBe 2
                game.state.projectedState.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
