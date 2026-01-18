package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.AddColorlessManaEffect
import com.wingedsheep.rulesengine.ability.AddManaEffect
import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.components.ManaPoolComponent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

/**
 * Handler for AddManaEffect.
 */
class AddManaHandler : BaseEffectHandler<AddManaEffect>() {
    override val effectClass: KClass<AddManaEffect> = AddManaEffect::class

    override fun execute(
        state: EcsGameState,
        effect: AddManaEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val container = state.getEntity(context.controllerId) ?: return noOp(state)
        val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()

        val newState = state.updateEntity(context.controllerId) { c ->
            c.with(manaPool.add(effect.color, effect.amount))
        }

        return result(newState, EcsEvent.ManaAdded(context.controllerId, effect.color.displayName, effect.amount))
    }
}

/**
 * Handler for AddColorlessManaEffect.
 */
class AddColorlessManaHandler : BaseEffectHandler<AddColorlessManaEffect>() {
    override val effectClass: KClass<AddColorlessManaEffect> = AddColorlessManaEffect::class

    override fun execute(
        state: EcsGameState,
        effect: AddColorlessManaEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val container = state.getEntity(context.controllerId) ?: return noOp(state)
        val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()

        val newState = state.updateEntity(context.controllerId) { c ->
            c.with(manaPool.addColorless(effect.amount))
        }

        return result(newState, EcsEvent.ManaAdded(context.controllerId, "Colorless", effect.amount))
    }
}

/**
 * Handler for CreateTokenEffect.
 */
class CreateTokenHandler : BaseEffectHandler<CreateTokenEffect>() {
    override val effectClass: KClass<CreateTokenEffect> = CreateTokenEffect::class

    override fun execute(
        state: EcsGameState,
        effect: CreateTokenEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Token creation requires CardDefinition creation - placeholder for now
        // Full implementation would create a new entity with TokenComponent
        return result(
            state,
            EcsEvent.TokenCreated(context.controllerId, effect.count, "${effect.power}/${effect.toughness}")
        )
    }
}
