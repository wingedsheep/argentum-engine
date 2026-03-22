package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.effects.CreateFoodTokensEffect
import kotlin.reflect.KClass

/**
 * Executor for CreateFoodTokensEffect.
 * Creates Food artifact tokens ("Artifact — Food").
 */
class CreateFoodTokenExecutor : EffectExecutor<CreateFoodTokensEffect> {

    override val effectType: KClass<CreateFoodTokensEffect> = CreateFoodTokensEffect::class

    override fun execute(
        state: GameState,
        effect: CreateFoodTokensEffect,
        context: EffectContext
    ): ExecutionResult {
        val controller = effect.controller
        val tokenControllerId = if (controller != null) {
            TargetResolutionUtils.resolvePlayerTarget(controller, context, state)
                ?: context.controllerId
        } else {
            context.controllerId
        }

        var newState = state
        val createdTokenIds = mutableListOf<EntityId>()

        repeat(effect.count) {
            val tokenId = EntityId.generate()
            createdTokenIds.add(tokenId)

            val tokenComponent = CardComponent(
                cardDefinitionId = "token:Food",
                name = "Food",
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Artifact - Food"),
                ownerId = tokenControllerId
            )

            val container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(tokenControllerId)
            )

            newState = newState.withEntity(tokenId, container)

            val battlefieldZone = ZoneKey(tokenControllerId, Zone.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        val events = createdTokenIds.map { tokenId ->
            ZoneChangeEvent(
                entityId = tokenId,
                entityName = "Food",
                fromZone = null,
                toZone = Zone.BATTLEFIELD,
                ownerId = tokenControllerId
            )
        }

        return ExecutionResult.success(newState, events)
    }
}
