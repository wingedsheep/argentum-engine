package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
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
class BecomeCreatureExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<BecomeCreatureEffect> {

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

        // Layer 4 (TYPE): Add CREATURE type
        var newState = state.addFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.AddType("CREATURE"),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        // Layer 4 (TYPE): Add any additional card types alongside CREATURE (e.g. ARTIFACT for
        // Mishra's Factory's "2/2 Assembly-Worker artifact creature. It's still a land").
        for (type in effect.addTypes) {
            newState = newState.addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.AddType(type),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 4 (TYPE): Remove specified types (e.g., PLANESWALKER)
        for (type in effect.removeTypes) {
            newState = newState.addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.RemoveType(type),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 4 (TYPE): Remove specified subtypes, keeping the rest ("it isn't an Equipment")
        for (subtype in effect.removeSubtypes) {
            newState = newState.addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.RemoveSubtype(subtype),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 4 (TYPE): Set creature subtypes
        if (effect.creatureTypes.isNotEmpty()) {
            newState = newState.addFloatingEffect(
                layer = Layer.TYPE,
                modification = SerializableModification.SetCreatureSubtypes(effect.creatureTypes),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 5 (COLOR): Change color if specified
        if (effect.colors != null) {
            newState = newState.addFloatingEffect(
                layer = Layer.COLOR,
                modification = SerializableModification.ChangeColor(effect.colors!!),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 6 (ABILITY): Grant keywords
        for (keyword in effect.keywords) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.GrantKeyword(keyword.name),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        // Layer 7b (POWER_TOUGHNESS, SET_VALUES, CR 613.4b — effects that set P/T to a value): Set
        // base P/T. When dynamic amounts are supplied (Xenic Poltergeist: P/T each equal to the
        // animated permanent's own mana value), use a dynamic modification recomputed per affected
        // entity at projection. Otherwise the fixed [power]/[toughness] amounts are evaluated once,
        // now, and stamped as a fixed set-value floating effect (CR 608.2h — game information is
        // determined once when the effect is applied; it does not keep recomputing).
        val dynamicPower = effect.dynamicPower
        val dynamicToughness = effect.dynamicToughness
        val ptModification = if (dynamicPower != null && dynamicToughness != null) {
            SerializableModification.SetPowerToughnessDynamic(dynamicPower, dynamicToughness)
        } else {
            val powerValue = amountEvaluator.evaluate(newState, effect.power, context)
            val toughnessValue = amountEvaluator.evaluate(newState, effect.toughness, context)
            SerializableModification.SetPowerToughness(powerValue, toughnessValue)
        }
        newState = newState.addFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            sublayer = Sublayer.SET_VALUES,
            modification = ptModification,
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        // Display-only: override the rendered card image for the duration of the animate (e.g. a
        // token's art for "becomes a Fractal"). Maps to NoOp in projection; read directly by the
        // client DTO transformer and expires with the rest of the animate at cleanup.
        if (effect.imageUri != null) {
            newState = newState.addFloatingEffect(
                layer = Layer.ABILITY,
                modification = SerializableModification.OverrideImage(effect.imageUri!!),
                affectedEntities = affectedEntities,
                duration = effect.duration,
                context = context
            )
        }

        return EffectResult.success(newState)
    }
}
