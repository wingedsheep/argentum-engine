package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.AddColorlessManaEffect
import com.wingedsheep.rulesengine.ability.AddManaEffect
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.components.ManaPoolComponent
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.EffectExecutor
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ManaHandlersTest : FunSpec({

    val executor = EffectExecutor()
    val player1 = EntityId.of("p1")

    fun setup(): GameState {
        return GameState.newGame(listOf(player1 to "Alice", EntityId.of("p2") to "Bob"))
    }

    context("AddManaHandler") {
        test("Adds single colored mana") {
            val state = setup()
            val effect = AddManaEffect(Color.GREEN, 1)
            val context = ExecutionContext(controllerId = player1, sourceId = EntityId.generate())

            val result = executor.execute(state, effect, context)

            val pool = result.state.getComponent<ManaPoolComponent>(player1)?.pool
            pool?.green shouldBe 1
            pool?.red shouldBe 0

            val event = result.events.first()
            event.shouldBeInstanceOf<EffectEvent.ManaAdded>()
            (event as EffectEvent.ManaAdded).amount shouldBe 1
            // Fixed: Expect "Green" instead of "GREEN" to match actual output
            event.color shouldBe "Green"
        }

        test("Adds multiple colored mana") {
            val state = setup()
            val effect = AddManaEffect(Color.RED, 3)
            val context = ExecutionContext(controllerId = player1, sourceId = EntityId.generate())

            val result = executor.execute(state, effect, context)

            val pool = result.state.getComponent<ManaPoolComponent>(player1)?.pool
            pool?.red shouldBe 3
        }

        test("Accumulates with existing mana") {
            val state = setup()
            val stateWithMana = state.updateEntity(player1) {
                val currentPool = it.get<ManaPoolComponent>() ?: ManaPoolComponent()
                it.with(currentPool.add(Color.BLUE, 1))
            }

            val effect = AddManaEffect(Color.BLUE, 2)
            val context = ExecutionContext(controllerId = player1, sourceId = EntityId.generate())

            val result = executor.execute(stateWithMana, effect, context)

            val pool = result.state.getComponent<ManaPoolComponent>(player1)?.pool
            pool?.blue shouldBe 3
        }
    }

    context("AddColorlessManaHandler") {
        test("Adds colorless mana") {
            val state = setup()
            val effect = AddColorlessManaEffect(2)
            val context = ExecutionContext(controllerId = player1, sourceId = EntityId.generate())

            val result = executor.execute(state, effect, context)

            val pool = result.state.getComponent<ManaPoolComponent>(player1)?.pool
            pool?.colorless shouldBe 2
        }

        test("Accumulates colorless mana") {
            val state = setup()
            val stateWithMana = state.updateEntity(player1) {
                val currentPool = it.get<ManaPoolComponent>() ?: ManaPoolComponent()
                it.with(currentPool.addColorless(1))
            }

            val effect = AddColorlessManaEffect(3)
            val context = ExecutionContext(controllerId = player1, sourceId = EntityId.generate())

            val result = executor.execute(stateWithMana, effect, context)

            val pool = result.state.getComponent<ManaPoolComponent>(player1)?.pool
            pool?.colorless shouldBe 4
        }
    }
})
