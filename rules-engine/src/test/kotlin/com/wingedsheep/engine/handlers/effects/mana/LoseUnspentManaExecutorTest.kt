package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.core.ManaPoolChangedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.RetainUnspentManaComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.LoseUnspentManaEffect
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LoseUnspentManaExecutorTest : FunSpec({
    val player = EntityId("player")
    val context = EffectContext(sourceId = null, controllerId = player)
    val executor = LoseUnspentManaExecutor(CardRegistry())

    test("clears colored colorless restricted combat mana and provenance without mutating input") {
        val pool = ManaPoolComponent(white = 1, blue = 1, black = 1, red = 1, green = 1, colorless = 1,
            manaBySource = mapOf(EntityId("source") to 6))
            .addRestricted(Color.BLUE, 1, ManaRestriction.CreatureSpellsOnly)
            .addRestricted(Color.RED, 2, ManaRestriction.AnySpend, expiry = ManaExpiry.END_OF_COMBAT)
        val state = GameState(entities = mapOf(player to ComponentContainer().with(pool)
            .with(RetainUnspentManaComponent(colors = setOf(Color.RED)))))
        val result = executor.execute(state, LoseUnspentManaEffect(), context)
        result.newState.getEntity(player)!!.get<ManaPoolComponent>() shouldBe ManaPoolComponent()
        result.events shouldBe listOf(ManaPoolChangedEvent(player))
        state.getEntity(player)!!.get<ManaPoolComponent>() shouldBe pool
        result.newState.getEntity(player)!!.get<RetainUnspentManaComponent>() shouldBe
            state.getEntity(player)!!.get<RetainUnspentManaComponent>()
    }

    test("empty pool is a no-op without an event") {
        val state = GameState(entities = mapOf(player to ComponentContainer().with(ManaPoolComponent())))
        val result = executor.execute(state, LoseUnspentManaEffect(), context)
        result.newState shouldBe state
        result.events shouldBe emptyList()
    }

    test("transferTo adds exactly the lost mana to the recipient, keeping every property") {
        val opponent = EntityId("opponent")
        val drained = ManaPoolComponent(black = 2, colorless = 1,
            manaBySource = mapOf(EntityId("swamp") to 2), snowMana = mapOf(Color.BLACK to 1))
            .addRestricted(Color.GREEN, 1, ManaRestriction.CreatureSpellsOnly)
            .addRestricted(Color.RED, 1, ManaRestriction.AnySpend, expiry = ManaExpiry.END_OF_COMBAT)
            .let { it.copy(restrictedMana = it.restrictedMana.map { e -> e.copy(obligationIds = setOf("act-1")) }) }
        val state = GameState(entities = mapOf(
            opponent to ComponentContainer().with(drained),
            player to ComponentContainer().with(ManaPoolComponent(white = 1))
        ))
        val effect = LoseUnspentManaEffect(target = EffectTarget.ContextTarget(0), transferTo = EffectTarget.Controller)
        val result = executor.execute(state, effect,
            context.copy(targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(opponent))))

        result.newState.getEntity(opponent)!!.get<ManaPoolComponent>() shouldBe ManaPoolComponent()
        val gained = result.newState.getEntity(player)!!.get<ManaPoolComponent>()!!
        gained.white shouldBe 1
        gained.black shouldBe 2
        gained.colorless shouldBe 1
        gained.snowMana shouldBe mapOf(Color.BLACK to 1)
        gained.manaBySource shouldBe mapOf(EntityId("swamp") to 2)
        gained.restrictedMana.map { Triple(it.color, it.restriction, it.expiry) } shouldBe listOf(
            Triple(Color.GREEN, ManaRestriction.CreatureSpellsOnly, ManaExpiry.END_OF_TURN),
            Triple(Color.RED, ManaRestriction.AnySpend, ManaExpiry.END_OF_COMBAT)
        )
        gained.restrictedMana.all { it.obligationIds.isEmpty() } shouldBe true
        result.events shouldBe listOf(
            ManaPoolChangedEvent(opponent),
            ManaAddedEvent(player, null, null, black = 2, green = 1, red = 1, colorless = 1),
            ManaPoolChangedEvent(player)
        )
    }

    test("transferTo moves nothing when the target had no mana") {
        val opponent = EntityId("opponent")
        val state = GameState(entities = mapOf(
            opponent to ComponentContainer().with(ManaPoolComponent()),
            player to ComponentContainer().with(ManaPoolComponent(white = 1))
        ))
        val effect = LoseUnspentManaEffect(target = EffectTarget.ContextTarget(0), transferTo = EffectTarget.Controller)
        val result = executor.execute(state, effect,
            context.copy(targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(opponent))))
        result.newState shouldBe state
        result.events shouldBe emptyList()
    }
})
