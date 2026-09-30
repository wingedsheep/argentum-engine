package com.wingedsheep.engine.handlers.effects.mana

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
})
