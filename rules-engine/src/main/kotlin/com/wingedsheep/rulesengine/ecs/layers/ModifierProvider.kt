package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.ecs.GameState

/**
 * Interface for providing modifiers from various sources.
 * This allows scripts and the ability registry to contribute modifiers to the StateProjector.
 */
interface ModifierProvider {
    /**
     * Get all active modifiers for the current game state.
     */
    fun getModifiers(state: GameState): List<Modifier>
}

/**
 * Simple modifier provider that returns a static list.
 * Useful for testing or simple scenarios.
 */
class StaticModifierProvider(private val modifiers: List<Modifier>) : ModifierProvider {
    override fun getModifiers(state: GameState): List<Modifier> = modifiers
}

/**
 * Modifier provider that extracts modifiers from the GameState's continuous effects.
 *
 * This bridges the gap between floating continuous effects (like Giant Growth's
 * "+3/+3 until end of turn") and the StateProjector's modifier system.
 *
 * Usage:
 * ```kotlin
 * val provider = ContinuousEffectModifierProvider()
 * val projector = StateProjector.forState(state, provider)
 * ```
 *
 * For combining with other modifier sources (like static abilities), use
 * CompositeModifierProvider.
 */
class ContinuousEffectModifierProvider : ModifierProvider {
    override fun getModifiers(state: GameState): List<Modifier> {
        return state.continuousEffects.map { it.toModifier() }
    }
}

/**
 * Combines multiple modifier providers into one.
 *
 * This is useful for combining modifiers from different sources:
 * - Static abilities from permanents (ScriptModifierProvider)
 * - Continuous effects from resolved spells (ContinuousEffectModifierProvider)
 * - Equipment/Aura effects
 *
 * Example:
 * ```kotlin
 * val provider = CompositeModifierProvider(
 *     ScriptModifierProvider(registry),
 *     ContinuousEffectModifierProvider()
 * )
 * val projector = StateProjector.forState(state, provider)
 * ```
 */
class CompositeModifierProvider(
    private vararg val providers: ModifierProvider
) : ModifierProvider {
    override fun getModifiers(state: GameState): List<Modifier> {
        return providers.flatMap { it.getModifiers(state) }
    }

    companion object {
        /**
         * Create a composite provider from a list of providers.
         */
        fun of(providers: List<ModifierProvider>): CompositeModifierProvider =
            CompositeModifierProvider(*providers.toTypedArray())

        /**
         * Create a standard provider that combines static abilities and continuous effects.
         * This is the most common configuration for a real game.
         */
        fun standard(registry: com.wingedsheep.rulesengine.ability.AbilityRegistry): CompositeModifierProvider =
            CompositeModifierProvider(
                com.wingedsheep.rulesengine.ecs.script.ScriptModifierProvider(registry),
                ContinuousEffectModifierProvider()
            )
    }
}