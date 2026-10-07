package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.MonstrousComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class DomesticatedHydraScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Domesticated Hydra")!!.activatedAbilities.first().id

    private fun TestGame.activate(x: Int) = execute(
        ActivateAbility(player1Id, findPermanent("Domesticated Hydra")!!, abilityId, xValue = x)
    )

    private fun TestGame.counters() = state.getEntity(findPermanent("Domesticated Hydra")!!)
        ?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("chosen X adds that many counters and grants trample only on resolution") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Domesticated Hydra")
                .withLandsOnBattlefield(1, "Forest", 12)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val hydra = game.findPermanent("Domesticated Hydra")!!

            game.state.projectedState.hasKeyword(hydra, Keyword.TRAMPLE) shouldBe false
            game.activate(4).error shouldBe null
            game.counters() shouldBe 0
            game.state.projectedState.hasKeyword(hydra, Keyword.TRAMPLE) shouldBe false
            game.resolveStack()

            game.counters() shouldBe 4
            game.state.getEntity(hydra)?.has<MonstrousComponent>() shouldBe true
            game.state.projectedState.getPower(hydra) shouldBe 7
            game.state.projectedState.getToughness(hydra) shouldBe 7
            game.state.projectedState.hasKeyword(hydra, Keyword.TRAMPLE) shouldBe true

            game.activate(2).error shouldBe null
            game.resolveStack()
            game.counters() shouldBe 4
        }

        test("X zero still makes the Hydra monstrous and grants trample without counters") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Domesticated Hydra")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val hydra = game.findPermanent("Domesticated Hydra")!!

            game.activate(0).error shouldBe null
            game.resolveStack()

            game.counters() shouldBe 0
            game.state.getEntity(hydra)?.has<MonstrousComponent>() shouldBe true
            game.state.projectedState.getPower(hydra) shouldBe 3
            game.state.projectedState.getToughness(hydra) shouldBe 3
            game.state.projectedState.hasKeyword(hydra, Keyword.TRAMPLE) shouldBe true
        }
    }
}
