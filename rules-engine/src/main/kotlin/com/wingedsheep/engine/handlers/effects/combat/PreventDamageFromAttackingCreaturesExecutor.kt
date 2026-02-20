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
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,  // Layer doesn't matter for this effect
                sublayer = null,
                modification = SerializableModification.PreventDamageFromAttackingCreatures,
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
