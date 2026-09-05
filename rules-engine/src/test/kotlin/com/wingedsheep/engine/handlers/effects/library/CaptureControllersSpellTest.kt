package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CaptureControllersEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CaptureControllersSpellTest : FunSpec({
    test("capture a stack spell's controller instead of its owner or stale card controller") {
        val owner = EntityId.generate()
        val caster = EntityId.generate()
        val spell = EntityId.generate()
        val state = GameState().withEntity(spell, ComponentContainer()
            .with(OwnerComponent(owner))
            .with(ControllerComponent(owner))
            .with(SpellOnStackComponent(caster)))
            .copy(stack = listOf(spell))
        val context = EffectContext(sourceId = null, controllerId = owner,
            pipeline = PipelineState(storedCollections = mapOf("cards" to listOf(spell))))
        val result = CaptureControllersExecutor().execute(state, CaptureControllersEffect("cards", "controllers"), context)
        result.updatedCollections shouldBe mapOf("controllers" to listOf(caster))
        result.state shouldBe state
        context.pipeline.storedCollections shouldBe mapOf("cards" to listOf(spell))
    }
})
