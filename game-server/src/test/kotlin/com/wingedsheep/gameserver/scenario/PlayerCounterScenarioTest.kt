package com.wingedsheep.gameserver.scenario

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlayerCounterScenarioTest : FunSpec({
    val service = ScenarioBuilderService(CardRegistry())
    test("scenario counters are per-player staged totals") {
        val result = service.buildScenario(ScenarioRequest(
            player1 = PlayerConfig(counters = mapOf("RAD" to 4)),
            player2 = PlayerConfig(counters = mapOf("RAD" to 2, "ENERGY" to 3))
        ))
        result.state.getEntity(result.player1Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) shouldBe 4
        result.state.getEntity(result.player2Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) shouldBe 2
        result.state.getEntity(result.player2Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) shouldBe 3
        result.state.stack.size shouldBe 0
    }
    test("spectators receive public counters in the board and legacy seat fields") {
        val registry = CardRegistry()
        val result = ScenarioBuilderService(registry).buildScenario(ScenarioRequest(
            player1 = PlayerConfig(counters = mapOf("RAD" to 4)),
            player2 = PlayerConfig(counters = mapOf("RAD" to 2))
        ))
        val view = com.wingedsheep.gameserver.session.SpectatorStateBuilder(
            registry, com.wingedsheep.engine.view.ClientStateTransformer(
                registry, predicateEvaluator = com.wingedsheep.engine.handlers.PredicateEvaluator(registry)
            )
        ).buildState(result.state, listOf(
            com.wingedsheep.gameserver.session.SpectatorSeat(result.player1Id, "One"),
            com.wingedsheep.gameserver.session.SpectatorSeat(result.player2Id, "Two")
        ), emptyList(), "radiation-spec")
        view.player1.radCounters shouldBe 4
        view.player2.radCounters shouldBe 2
        view.gameState?.players?.single { it.playerId == result.player1Id }?.radCounters shouldBe 4
    }
    test("negative player totals are rejected by public validation") {
        service.validate(ScenarioRequest(player1 = PlayerConfig(counters = mapOf("RAD" to -1))), true)
            .any { "nonnegative" in it } shouldBe true
    }
    test("unknown counter kinds are rejected at the scenario API boundary") {
        service.validate(ScenarioRequest(player1 = PlayerConfig(counters = mapOf("NOT_A_COUNTER" to 1))), true)
            .any { "Unknown player counter" in it } shouldBe true
    }
})
