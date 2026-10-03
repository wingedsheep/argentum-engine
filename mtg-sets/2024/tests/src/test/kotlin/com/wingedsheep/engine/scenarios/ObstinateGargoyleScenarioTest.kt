package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Obstinate Gargoyle (MH3 #195): "This creature has flying as long as it's modified. Persist."
 */
class ObstinateGargoyleScenarioTest : ScenarioTestBase() {

    init {
        context("Obstinate Gargoyle") {

            test("has no flying while unmodified, flying once it has a counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Obstinate Gargoyle")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val gargoyle = game.findPermanent("Obstinate Gargoyle")!!

                game.state.projectedState.hasKeyword(gargoyle, Keyword.FLYING) shouldBe false

                game.state = game.state.updateEntity(gargoyle) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
                game.state.projectedState.hasKeyword(gargoyle, Keyword.FLYING) shouldBe true
            }

            test("persist returns it with a -1/-1 counter, so it comes back flying; dies for good the second time") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Obstinate Gargoyle")
                    .withCardsInHand(1, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val first = game.findPermanent("Obstinate Gargoyle")!!

                game.castSpell(1, "Lightning Bolt", first).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Obstinate Gargoyle")
                returned shouldNotBe null
                game.state.getEntity(returned!!)!!.get<CountersComponent>()!!
                    .getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
                val projected = game.state.projectedState
                projected.getPower(returned) shouldBe 1
                projected.getToughness(returned) shouldBe 1
                projected.hasKeyword(returned, Keyword.FLYING) shouldBe true

                game.castSpell(1, "Lightning Bolt", returned).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Obstinate Gargoyle") shouldBe false
                game.isInGraveyard(1, "Obstinate Gargoyle") shouldBe true
            }
        }
    }
}
