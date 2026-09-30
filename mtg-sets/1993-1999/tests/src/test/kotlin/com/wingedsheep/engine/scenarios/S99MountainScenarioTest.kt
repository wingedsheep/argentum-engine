package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.s99.Starter1999Set
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Mountain166
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Mountain167
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Mountain168
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Mountain169
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class S99MountainScenarioTest : ScenarioTestBase() {
    init {
        test("S99 discovers every Mountain art with the set identity") {
            val mountains = Starter1999Set.basicLands.filter { it.name == "Mountain" }
            mountains.map { it.metadata.collectorNumber }.sortedBy { it?.toInt() } shouldBe
                listOf("166", "167", "168", "169")
            mountains.map { it.setCode }.toSet() shouldBe setOf("S99")
        }

        listOf(
            Starter1999Mountain166,
            Starter1999Mountain167,
            Starter1999Mountain168,
            Starter1999Mountain169
        ).forEach { mountain ->
            test("S99 Mountain ${mountain.metadata.collectorNumber} taps for red immediately and cannot tap twice") {
                val printing = mountain.copy(setCode = "S99")
                cardRegistry.register(printing)
                val game = scenario()
                    .withPlayers("Mountain player", "Opponent")
                    .withCardOnBattlefield(1, "Mountain#S99-${mountain.metadata.collectorNumber}")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val permanent = game.findPermanent("Mountain")!!
                val activation = ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = permanent,
                    abilityId = mountain.script.activatedAbilities.single().id
                )

                game.execute(activation).error shouldBe null

                game.state.getEntity(permanent)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.getAmount(Color.RED) shouldBe 1
                game.state.stack.isEmpty() shouldBe true
                game.execute(activation).error shouldNotBe null
                game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.getAmount(Color.RED) shouldBe 1
            }
        }
    }
}
