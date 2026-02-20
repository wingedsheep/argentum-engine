package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.FightEffect
import kotlin.reflect.KClass

/**
 * Executor for FightEffect.
 * Each creature deals damage equal to its power to the other creature.
 */
class FightEffectExecutor : EffectExecutor<FightEffect> {

    override val effectType: KClass<FightEffect> = FightEffect::class

    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: FightEffect,
        context: EffectContext
    ): ExecutionResult {
        val target1Id = resolveTarget(effect.target1, context, state)
            ?: return ExecutionResult.error(state, "No valid first target for fight")

        val target2Id = resolveTarget(effect.target2, context, state)
            ?: return ExecutionResult.error(state, "No valid second target for fight")

        // Get projected power for each creature (projected state accounts for buffs/debuffs)
        val projected = stateProjector.project(state)
        val power1 = projected.getPower(target1Id) ?: 0
        val power2 = projected.getPower(target2Id) ?: 0

        // Creature 1 deals damage equal to its power to creature 2
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        if (power1 > 0) {
            val result1 = dealDamageToTarget(currentState, target2Id, power1, target1Id)
            currentState = result1.newState
            allEvents.addAll(result1.events)
        }

        // Creature 2 deals damage equal to its power to creature 1
        if (power2 > 0) {
            val result2 = dealDamageToTarget(currentState, target1Id, power2, target2Id)
            currentState = result2.newState
            allEvents.addAll(result2.events)
        }

        return ExecutionResult.success(currentState, allEvents)
    }
}
