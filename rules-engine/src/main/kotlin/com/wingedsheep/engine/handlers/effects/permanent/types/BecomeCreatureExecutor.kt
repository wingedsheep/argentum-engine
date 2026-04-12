package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import kotlin.reflect.KClass

/**
 * Executor for BecomeCreatureEffect.
 * Turns a permanent into a creature by creating floating effects across multiple layers.
 *
 * Used for Sarkhan, the Dragonspeaker's +1 and similar "becomes a creature" effects.
 * Creates all floating effects atomically to avoid validation issues with intermediate states.
 */
class BecomeCreatureExecutor : EffectExecutor<BecomeCreatureEffect> {

    override val effectType: KClass<BecomeCreatureEffect> = BecomeCreatureEffect::class

    override fun execute(
        state: GameState,
        effect: BecomeCreatureEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        // Verify the target is still on the battlefield
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val affectedEntities = setOf(targetId)

        val floatingEffects = mutableListOf<ActiveFloatingEffect>()

        // Layer 4 (TYPE): Add CREATURE type
        floatingEffects.add(state.createFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.AddType("CREATURE"),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        ))

        // Layer 4 (TYPE): Remove specified types (e.g., PLANESWALKER)
        for (type in effect.removeTypes) {
            floatingEffects.add(state.createFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.RemoveType(type),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            ))
        }

        // Layer 4 (TYPE): Set creature subtypes
        if (effect.creatureTypes.isNotEmpty()) {
            floatingEffects.add(state.createFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.SetCreatureSubtypes(effect.creatureTypes),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            ))
        }

        // Layer 5 (COLOR): Change color if specified
        if (effect.colors != null) {
            floatingEffects.add(state.createFloatingEffect(
                layer = Layer.COLOR,
                modification = SerializableModification.ChangeColor(effect.colors!!),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            ))
        }

        // Layer 6 (ABILITY): Grant keywords
        for (keyword in effect.keywords) {
            floatingEffects.add(state.createFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.GrantKeyword(keyword.name),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            ))
        }

        // Layer 7b (POWER_TOUGHNESS, SET_VALUES): Set base P/T
        floatingEffects.add(state.createFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            sublayer = Sublayer.SET_VALUES,
            modification = SerializableModification.SetPowerToughness(effect.power, effect.toughness),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        ))

        val newState = state.addFloatingEffects(floatingEffects)

        return EffectResult.success(newState)
    }
}
