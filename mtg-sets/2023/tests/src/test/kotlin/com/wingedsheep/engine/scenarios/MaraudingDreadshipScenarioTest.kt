package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType

/** Marauding Dreadship — Vehicle, haste; incubates 2 on entering; Crew 2. */
class MaraudingDreadshipScenarioTest : ScenarioTestBase() {
    init {
        test("entering incubates 2, and Crew 2 animates it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Marauding Dreadship")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Marauding Dreadship").error shouldBe null
            game.resolveStack()

            val incubator = game.state.getBattlefield(game.player1Id).single {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Incubator"
            }
            game.state.getEntity(incubator)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

            val ship = game.findPermanent("Marauding Dreadship")!!
            game.state.projectedState.isCreature(ship) shouldBe false
            val giant = game.findPermanent("Hill Giant")!!
            game.execute(CrewVehicle(game.player1Id, ship, listOf(giant))).error shouldBe null
            game.resolveStack()
            game.state.projectedState.isCreature(ship) shouldBe true
            game.state.projectedState.getPower(ship) shouldBe 4
            game.state.projectedState.getToughness(ship) shouldBe 1
        }
    }
}
