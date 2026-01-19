package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.ecs.EcsGameState

/**
 * Interface for providing modifiers from various sources.
 * This allows scripts and the ability registry to contribute modifiers to the StateProjector.
 */
interface ModifierProvider {
    /**
     * Get all active modifiers for the current game state.
     */
    fun getModifiers(state: EcsGameState): List<Modifier>
}

/**
 * Simple modifier provider that returns a static list.
 * Useful for testing or simple scenarios.
 */
class StaticModifierProvider(private val modifiers: List<Modifier>) : ModifierProvider {
    override fun getModifiers(state: EcsGameState): List<Modifier> = modifiers
}