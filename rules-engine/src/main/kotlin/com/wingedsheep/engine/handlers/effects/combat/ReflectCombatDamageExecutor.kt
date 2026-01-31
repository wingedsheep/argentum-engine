package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ReflectCombatDamageEffect
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
    ): ExecutionResult {
        // The protected player is the controller of the spell (the player casting Harsh Justice)
        val protectedPlayerId = context.controllerId

        // Create a floating effect marking this player as having damage reflection
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,  // Layer doesn't matter for this effect
                sublayer = null,
                modification = SerializableModification.ReflectCombatDamage(
                    protectedPlayerId = protectedPlayerId.toString()
                ),
                affectedEntities = setOf(protectedPlayerId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        // Add the floating effect to game state
        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
