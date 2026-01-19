package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.CompositeEffect
import com.wingedsheep.rulesengine.ability.Condition
import com.wingedsheep.rulesengine.ability.ConditionalEffect
import com.wingedsheep.rulesengine.ability.Effect
import com.wingedsheep.rulesengine.ability.LookAtTopCardsEffect
import com.wingedsheep.rulesengine.ability.ShuffleIntoLibraryEffect
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.layers.Modifier
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

/**
 * Handler for CompositeEffect.
 *
 * Takes a callback to execute sub-effects, avoiding circular dependency on the registry.
 */
class CompositeHandler(
    private val executeEffect: (Effect, GameState, ExecutionContext) -> ExecutionResult
) : BaseEffectHandler<CompositeEffect>() {
    override val effectClass: KClass<CompositeEffect> = CompositeEffect::class

    override fun execute(
        state: GameState,
        effect: CompositeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<EffectEvent>()
        val allModifiers = mutableListOf<Modifier>()

        for (subEffect in effect.effects) {
            val result = executeEffect(subEffect, currentState, context)
            currentState = result.state
            allEvents.addAll(result.events)
            allModifiers.addAll(result.temporaryModifiers)
        }

        return ExecutionResult(currentState, allEvents, allModifiers)
    }
}

/**
 * Handler for ConditionalEffect.
 *
 * Takes a callback to execute sub-effects, avoiding circular dependency on the registry.
 * Uses ConditionEvaluator to check conditions against the ECS game state.
 */
class ConditionalHandler(
    private val executeEffect: (Effect, GameState, ExecutionContext) -> ExecutionResult
) : BaseEffectHandler<ConditionalEffect>() {
    override val effectClass: KClass<ConditionalEffect> = ConditionalEffect::class

    override fun execute(
        state: GameState,
        effect: ConditionalEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val conditionMet = ConditionEvaluator.evaluate(state, effect.condition, context)

        return if (conditionMet) {
            executeEffect(effect.effect, state, context)
        } else if (effect.elseEffect != null) {
            executeEffect(effect.elseEffect, state, context)
        } else {
            noOp(state)
        }
    }
}

/**
 * Handler for ShuffleIntoLibraryEffect.
 */
class ShuffleIntoLibraryHandler : BaseEffectHandler<ShuffleIntoLibraryEffect>() {
    override val effectClass: KClass<ShuffleIntoLibraryEffect> = ShuffleIntoLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: ShuffleIntoLibraryEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Placeholder - would shuffle the source card back into library
        return noOp(state)
    }
}

/**
 * Handler for LookAtTopCardsEffect.
 */
class LookAtTopCardsHandler : BaseEffectHandler<LookAtTopCardsEffect>() {
    override val effectClass: KClass<LookAtTopCardsEffect> = LookAtTopCardsEffect::class

    override fun execute(
        state: GameState,
        effect: LookAtTopCardsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Placeholder - would need player choice mechanism
        return noOp(state)
    }
}
