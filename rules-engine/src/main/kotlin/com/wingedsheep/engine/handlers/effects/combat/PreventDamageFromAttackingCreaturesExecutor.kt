package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.PreventDamageFromAttackingCreaturesThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for PreventDamageFromAttackingCreaturesThisTurnEffect.
 * "Prevent all damage that would be dealt to you this turn by attacking creatures."
 *
 * This creates a floating effect that marks the player as protected from combat damage
 * by attacking creatures. The CombatManager checks for this effect before dealing
 * combat damage to players from unblocked attackers.
 */
class PreventDamageFromAttackingCreaturesExecutor : EffectExecutor<PreventDamageFromAttackingCreaturesThisTurnEffect> {

    override val effectType: KClass<PreventDamageFromAttackingCreaturesThisTurnEffect> =
        PreventDamageFromAttackingCreaturesThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: PreventDamageFromAttackingCreaturesThisTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        // The affected entity is the controller of the spell (the player casting Deep Wood)
        val protectedPlayerId = context.controllerId

        // Create a floating effect marking this player as protected from attacking creature damage
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.PreventDamageFromAttackingCreatures,
            affectedEntities = setOf(protectedPlayerId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
