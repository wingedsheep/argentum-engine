package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.GameState

/**
 * Sorts continuous effects by layer, sublayer, dependency, and timestamp (Rule 613).
 */
internal class EffectSorter {

    fun sortByLayerAndDependency(
        effects: List<ContinuousEffect>,
        state: GameState
    ): List<ContinuousEffect> {
        val byLayer = effects.groupBy { it.layer }
        val result = mutableListOf<ContinuousEffect>()

        for (layer in Layer.entries) {
            val layerEffects = byLayer[layer] ?: continue

            if (layer == Layer.POWER_TOUGHNESS) {
                val bySublayer = layerEffects.groupBy { it.sublayer }
                for (sublayer in Sublayer.entries) {
                    val sublayerEffects = bySublayer[sublayer] ?: continue
                    result.addAll(sortByDependencyAndTimestamp(sublayerEffects, state))
                }
            } else {
                result.addAll(sortByDependencyAndTimestamp(layerEffects, state))
            }
        }

        return result
    }

    private fun sortByDependencyAndTimestamp(
        effects: List<ContinuousEffect>,
        state: GameState
    ): List<ContinuousEffect> {
        if (effects.size <= 1) return effects

        val dependencies = mutableMapOf<ContinuousEffect, Set<ContinuousEffect>>()

        for (effectA in effects) {
            val dependsOn = mutableSetOf<ContinuousEffect>()
            for (effectB in effects) {
                if (effectA === effectB) continue
                if (dependsOn(effectA, effectB, state)) {
                    dependsOn.add(effectB)
                }
            }
            dependencies[effectA] = dependsOn
        }

        // Topological sort with timestamp as tiebreaker
        // Use identity-based tracking to avoid deduplicating equal-but-distinct effects
        val result = mutableListOf<ContinuousEffect>()
        val remaining = effects.toMutableList()

        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { effect ->
                dependencies[effect]?.none { dep -> remaining.any { it === dep } } ?: true
            }.sortedBy { it.timestamp }

            if (ready.isEmpty()) {
                result.addAll(remaining.sortedBy { it.timestamp })
                break
            }

            val next = ready.first()
            result.add(next)
            remaining.removeAt(remaining.indexOfFirst { it === next })
        }

        return result
    }

    private fun dependsOn(
        effectA: ContinuousEffect,
        effectB: ContinuousEffect,
        state: GameState
    ): Boolean {
        if (effectB.modification is Modification.AddType || effectB.modification is Modification.RemoveType) {
            return effectA.affectedEntities.any { it in effectB.affectedEntities }
        }
        return false
    }
}
