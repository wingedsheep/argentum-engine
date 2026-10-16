package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [Player.ControllerOfGrantingSource] — the controller of the permanent that granted the resolving
 * ability, as opposed to the bearer's controller ([Player.You]) who activated it.
 */
class GrantingSourceControllerTest : FunSpec({
    val bearer = EntityId.generate()
    val aura = EntityId.generate()
    val auraController = EntityId.generate()
    val bearerController = EntityId.generate()
    val context = EffectContext(sourceId = bearer, controllerId = bearerController, granterId = aura)

    fun resolve(state: GameState, ctx: EffectContext = context) =
        TargetResolutionUtils.resolvePlayerRef(Player.ControllerOfGrantingSource, ctx, state)

    test("names the granter's controller, not the activating bearer's controller") {
        val state = GameState().withEntity(aura, ComponentContainer().with(ControllerComponent(auraController)))
        resolve(state) shouldBe auraController
        TargetResolutionUtils.resolvePlayerRef(Player.You, context, state) shouldBe bearerController
    }

    test("a granter that has left the battlefield answers with its last-known controller") {
        val state = GameState().withEntity(
            aura,
            ComponentContainer().with(
                LastKnownPermanentComponent(EntitySnapshot(entityId = aura, controllerId = auraController))
            )
        )
        resolve(state) shouldBe auraController
    }

    test("a granter that ceased to exist answers from this resolution's departed snapshot") {
        val departed = EntitySnapshot(entityId = aura, controllerId = auraController)
        resolve(GameState(), context.copy(departedPermanents = listOf(departed))) shouldBe auraController
    }

    test("is null outside a granted ability") {
        resolve(GameState(), context.copy(granterId = null)) shouldBe null
    }
})
