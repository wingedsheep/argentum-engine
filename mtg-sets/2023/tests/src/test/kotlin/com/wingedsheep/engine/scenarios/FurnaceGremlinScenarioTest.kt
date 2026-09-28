package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.FurnaceGremlin
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Furnace Gremlin — "{1}{R}: +1/+0 until end of turn. When this creature dies, incubate X, where X is its power."
 */
class FurnaceGremlinScenarioTest : ScenarioTestBase() {

    private val pumpAbility = FurnaceGremlin.activatedAbilities.single().id

    private fun TestGame.incubatorCounts(): List<Int> =
        state.getBattlefield(player1Id)
            .filter { state.getEntity(it)?.get<CardComponent>()?.name == "Incubator" }
            .map { state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0 }

    init {
        context("Furnace Gremlin") {
            test("dying at base power incubates 1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Furnace Gremlin")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gremlin = game.findPermanent("Furnace Gremlin")!!
                game.castSpell(2, "Lightning Bolt", gremlin).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Furnace Gremlin") shouldBe true
                game.incubatorCounts() shouldBe listOf(1)
            }

            test("pumped power is used for incubate") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Furnace Gremlin", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gremlin = game.findPermanent("Furnace Gremlin")!!
                game.execute(ActivateAbility(game.player1Id, gremlin, pumpAbility)).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Lightning Bolt", gremlin).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Furnace Gremlin") shouldBe true
                game.incubatorCounts() shouldBe listOf(2)
            }
        }
    }
}
