package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ReflectCombatDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for ReflectCombatDamageEffect.
 * "This turn, whenever an attacking creature deals combat damage to you,
 *  it deals that much damage to its controller."
 *
 * This creates a floating effect that marks the player as having damage reflection active.
 * The CombatManager checks for this effect after dealing combat damage to players from
 * unblocked attackers, and reflects the damage back to the attacker's controller.
 */
class ReflectCombatDamageExecutor : EffectExecutor<ReflectCombatDamageEffect> {

    override val effectType: KClass<ReflectCombatDamageEffect> = ReflectCombatDamageEffect::class

    override fun execute(
        state: GameState,
        effect: ReflectCombatDamageEffect,
        context: EffectContext
    ): EffectResult {
        // The protected player is the controller of the spell (the player casting Harsh Justice)
        val protectedPlayerId = context.controllerId

        // Create a floating effect marking this player as having damage reflection
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.ReflectCombatDamage(
                protectedPlayerId = protectedPlayerId.toString()
            ),
            affectedEntities = setOf(protectedPlayerId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
