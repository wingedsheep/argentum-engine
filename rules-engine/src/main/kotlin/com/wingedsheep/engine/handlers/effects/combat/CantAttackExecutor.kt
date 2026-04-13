package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.CantAttackEffect
import kotlin.reflect.KClass

/**
 * Executor for CantAttackEffect.
 *
 * Creates a floating effect with SetCantAttack for the targeted creature.
 */
class CantAttackExecutor : EffectExecutor<CantAttackEffect> {

    override val effectType: KClass<CantAttackEffect> = CantAttackEffect::class

    override fun execute(
        state: GameState,
        effect: CantAttackEffect,
        context: EffectContext
    ): EffectResult {
        val entityId = TargetResolutionUtils.resolveTarget(effect.target, context)
            ?: return EffectResult.success(state)
        val container = state.getEntity(entityId)
            ?: return EffectResult.success(state)
        container.get<CardComponent>()
            ?: return EffectResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetCantAttack,
            affectedEntities = setOf(entityId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
