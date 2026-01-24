package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId

/**
 * Projects the game state by applying continuous effects in layer order (Rule 613).
 *
 * The StateProjector transforms "base state" (stored components) into
 * "projected state" (what the game sees after all effects are applied).
 *
 * Layer Order:
 * 1. Copy effects
 * 2. Control-changing effects
 * 3. Text-changing effects
 * 4. Type-changing effects
 * 5. Color-changing effects
 * 6. Ability-adding/removing effects
 * 7. Power/toughness modifications
 *    a. Characteristic-defining abilities
 *    b. Setting P/T to specific values
 *    c. Modifications from +N/+N effects
 *    d. Counters (+1/+1, -1/-1)
 *    e. Effects that switch P/T
 */
class StateProjector {

    /**
     * Project the full game state with all continuous effects applied.
     */
    fun project(state: GameState): ProjectedState {
        // Initialize projected values from base state
        val projectedValues = mutableMapOf<EntityId, MutableProjectedValues>()

        // Initialize all permanents with their base values
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            projectedValues[entityId] = MutableProjectedValues(
                power = cardComponent.baseStats?.basePower,
                toughness = cardComponent.baseStats?.baseToughness,
                keywords = cardComponent.baseKeywords.map { it.name }.toMutableSet(),
                colors = cardComponent.colors.map { it.name }.toMutableSet(),
                types = extractTypes(cardComponent),
                controllerId = container.get<ControllerComponent>()?.playerId
            )
        }

        // Collect all active continuous effects
        val effects = collectContinuousEffects(state)

        // Sort effects by layer and dependency
        val sortedEffects = sortByLayerAndDependency(effects, state)

        // Apply effects in order
        for (effect in sortedEffects) {
            applyEffect(effect, state, projectedValues)
        }

        // Apply counters (layer 7d) - this is always done from base state
        applyCounters(state, projectedValues)

        // Convert to immutable
        val finalValues = projectedValues.mapValues { (_, v) ->
            ProjectedValues(
                power = v.power,
                toughness = v.toughness,
                keywords = v.keywords.toSet(),
                colors = v.colors.toSet(),
                types = v.types.toSet(),
                controllerId = v.controllerId
            )
        }

        return ProjectedState(state, finalValues)
    }

    /**
     * Get the projected power of a creature.
     */
    fun getProjectedPower(state: GameState, entityId: EntityId): Int {
        val projected = project(state)
        return projected.getPower(entityId) ?: 0
    }

    /**
     * Get the projected toughness of a creature.
     */
    fun getProjectedToughness(state: GameState, entityId: EntityId): Int {
        val projected = project(state)
        return projected.getToughness(entityId) ?: 0
    }

    /**
     * Extract type strings from a card component.
     */
    private fun extractTypes(card: CardComponent): MutableSet<String> {
        val types = mutableSetOf<String>()

        // Add supertypes
        types.addAll(card.typeLine.supertypes.map { it.name })

        // Add card types
        types.addAll(card.typeLine.cardTypes.map { it.name })

        // Add subtypes
        types.addAll(card.typeLine.subtypes.map { it.value })

        return types
    }

    /**
     * Collect continuous effects from all sources.
     */
    private fun collectContinuousEffects(state: GameState): List<ContinuousEffect> {
        val effects = mutableListOf<ContinuousEffect>()

        // Collect effects from static abilities on permanents
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue

            // Check for continuous effect components
            val continuousEffectComponent = container.get<ContinuousEffectSourceComponent>()
            if (continuousEffectComponent != null) {
                effects.addAll(continuousEffectComponent.effects.map { effect ->
                    ContinuousEffect(
                        sourceId = entityId,
                        layer = effect.layer,
                        sublayer = effect.sublayer,
                        timestamp = container.get<com.wingedsheep.engine.state.components.battlefield.TimestampComponent>()?.timestamp
                            ?: state.timestamp,
                        modification = effect.modification,
                        affectedEntities = resolveAffectedEntities(state, entityId, effect.affectsFilter)
                    )
                })
            }
        }

        return effects
    }

    /**
     * Resolve which entities are affected by a continuous effect.
     */
    private fun resolveAffectedEntities(
        state: GameState,
        sourceId: EntityId,
        filter: AffectsFilter?
    ): Set<EntityId> {
        if (filter == null) return setOf(sourceId) // Affects self by default

        return when (filter) {
            is AffectsFilter.Self -> setOf(sourceId)
            is AffectsFilter.AllCreaturesYouControl -> {
                val controller = state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val entityController = container.get<ControllerComponent>()?.playerId
                    card.typeLine.isCreature && entityController == controller
                }.toSet()
            }
            is AffectsFilter.AllCreatures -> {
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.isCreature == true
                }.toSet()
            }
            is AffectsFilter.AllCreaturesOpponentsControl -> {
                val controller = state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val entityController = container.get<ControllerComponent>()?.playerId
                    card.typeLine.isCreature && entityController != controller
                }.toSet()
            }
            is AffectsFilter.SpecificEntities -> filter.entityIds
            is AffectsFilter.WithSubtype -> {
                state.getBattlefield().filter { entityId ->
                    val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
                    card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(filter.subtype))
                }.toSet()
            }
        }
    }

    /**
     * Sort effects by layer, then by timestamp within the same layer.
     * Also handles dependencies (Rule 613.8).
     */
    private fun sortByLayerAndDependency(
        effects: List<ContinuousEffect>,
        state: GameState
    ): List<ContinuousEffect> {
        // Group by layer
        val byLayer = effects.groupBy { it.layer }

        val result = mutableListOf<ContinuousEffect>()

        // Process each layer in order
        for (layer in Layer.entries) {
            val layerEffects = byLayer[layer] ?: continue

            if (layer == Layer.POWER_TOUGHNESS) {
                // Within layer 7, sort by sublayer first
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

    /**
     * Sort effects within the same layer by dependency and timestamp.
     */
    private fun sortByDependencyAndTimestamp(
        effects: List<ContinuousEffect>,
        state: GameState
    ): List<ContinuousEffect> {
        if (effects.size <= 1) return effects

        // Check for dependencies using trial application
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
        val result = mutableListOf<ContinuousEffect>()
        val remaining = effects.toMutableSet()

        while (remaining.isNotEmpty()) {
            // Find effects with no remaining dependencies
            val ready = remaining.filter { effect ->
                dependencies[effect]?.none { it in remaining } ?: true
            }.sortedBy { it.timestamp } // Timestamp tiebreaker

            if (ready.isEmpty()) {
                // Circular dependency - fall back to timestamp
                result.addAll(remaining.sortedBy { it.timestamp })
                break
            }

            val next = ready.first()
            result.add(next)
            remaining.remove(next)
        }

        return result
    }

    /**
     * Check if effectA depends on effectB (Rule 613.8).
     * An effect depends on another if what it applies to would change based on whether
     * the other effect has been applied.
     */
    private fun dependsOn(
        effectA: ContinuousEffect,
        effectB: ContinuousEffect,
        state: GameState
    ): Boolean {
        // Type-changing effects can create dependencies
        if (effectB.modification is Modification.AddType || effectB.modification is Modification.RemoveType) {
            // If effectA's affected entities filter depends on types, it depends on effectB
            // This is a simplified check - full implementation would do trial application
            return effectA.affectedEntities.any { it in effectB.affectedEntities }
        }

        return false
    }

    /**
     * Apply a single continuous effect.
     */
    private fun applyEffect(
        effect: ContinuousEffect,
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        for (entityId in effect.affectedEntities) {
            val values = projectedValues.getOrPut(entityId) { MutableProjectedValues() }

            when (val mod = effect.modification) {
                is Modification.SetPowerToughness -> {
                    values.power = mod.power
                    values.toughness = mod.toughness
                }
                is Modification.ModifyPowerToughness -> {
                    values.power = (values.power ?: 0) + mod.powerMod
                    values.toughness = (values.toughness ?: 0) + mod.toughnessMod
                }
                is Modification.SwitchPowerToughness -> {
                    val p = values.power
                    val t = values.toughness
                    values.power = t
                    values.toughness = p
                }
                is Modification.GrantKeyword -> {
                    values.keywords.add(mod.keyword)
                }
                is Modification.RemoveKeyword -> {
                    values.keywords.remove(mod.keyword)
                }
                is Modification.ChangeColor -> {
                    values.colors.clear()
                    values.colors.addAll(mod.colors)
                }
                is Modification.AddColor -> {
                    values.colors.addAll(mod.colors)
                }
                is Modification.AddType -> {
                    values.types.add(mod.type)
                }
                is Modification.RemoveType -> {
                    values.types.remove(mod.type)
                }
                is Modification.ChangeController -> {
                    values.controllerId = mod.newControllerId
                }
            }
        }
    }

    /**
     * Apply +1/+1 and -1/-1 counters (layer 7d).
     */
    private fun applyCounters(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val values = projectedValues.getOrPut(entityId) { MutableProjectedValues() }

            val plusOneCounters = counters.getCount(CounterType.PLUS_ONE_PLUS_ONE)
            val minusOneCounters = counters.getCount(CounterType.MINUS_ONE_MINUS_ONE)

            // +1/+1 and -1/-1 counters annihilate each other
            val netCounters = plusOneCounters - minusOneCounters

            if (netCounters != 0) {
                values.power = (values.power ?: 0) + netCounters
                values.toughness = (values.toughness ?: 0) + netCounters
            }
        }
    }
}

/**
 * Component that stores continuous effects generated by a permanent.
 */
data class ContinuousEffectSourceComponent(
    val effects: List<ContinuousEffectData>
) : com.wingedsheep.engine.state.Component

/**
 * Data for a single continuous effect.
 */
data class ContinuousEffectData(
    val layer: Layer,
    val sublayer: Sublayer? = null,
    val modification: Modification,
    val affectsFilter: AffectsFilter? = null
)

/**
 * Filter for determining which entities are affected.
 */
sealed interface AffectsFilter {
    data object Self : AffectsFilter
    data object AllCreatures : AffectsFilter
    data object AllCreaturesYouControl : AffectsFilter
    data object AllCreaturesOpponentsControl : AffectsFilter
    data class SpecificEntities(val entityIds: Set<EntityId>) : AffectsFilter
    data class WithSubtype(val subtype: String) : AffectsFilter
}

/**
 * Represents a continuous effect that modifies the game state.
 */
data class ContinuousEffect(
    val sourceId: EntityId,
    val layer: Layer,
    val sublayer: Sublayer? = null,
    val timestamp: Long,
    val modification: Modification,
    val affectedEntities: Set<EntityId> = emptySet()
)

/**
 * The layers in which continuous effects are applied (Rule 613).
 */
enum class Layer {
    COPY,           // Layer 1
    CONTROL,        // Layer 2
    TEXT,           // Layer 3
    TYPE,           // Layer 4
    COLOR,          // Layer 5
    ABILITY,        // Layer 6
    POWER_TOUGHNESS // Layer 7
}

/**
 * Sublayers for layer 7 (power/toughness).
 */
enum class Sublayer {
    CHARACTERISTIC_DEFINING,  // 7a
    SET_VALUES,               // 7b
    MODIFICATIONS,            // 7c
    COUNTERS,                 // 7d
    SWITCH                    // 7e
}

/**
 * Types of modifications that can be applied.
 */
sealed interface Modification {
    data class SetPowerToughness(val power: Int, val toughness: Int) : Modification
    data class ModifyPowerToughness(val powerMod: Int, val toughnessMod: Int) : Modification
    data class SwitchPowerToughness(val targetId: EntityId) : Modification
    data class GrantKeyword(val keyword: String) : Modification
    data class RemoveKeyword(val keyword: String) : Modification
    data class ChangeColor(val colors: Set<String>) : Modification
    data class AddColor(val colors: Set<String>) : Modification
    data class AddType(val type: String) : Modification
    data class RemoveType(val type: String) : Modification
    data class ChangeController(val newControllerId: EntityId) : Modification
}

/**
 * Mutable projected values during calculation.
 */
private data class MutableProjectedValues(
    var power: Int? = null,
    var toughness: Int? = null,
    val keywords: MutableSet<String> = mutableSetOf(),
    val colors: MutableSet<String> = mutableSetOf(),
    val types: MutableSet<String> = mutableSetOf(),
    var controllerId: EntityId? = null
)

/**
 * Projected values for an entity after all effects are applied.
 */
data class ProjectedValues(
    val power: Int? = null,
    val toughness: Int? = null,
    val keywords: Set<String> = emptySet(),
    val colors: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val controllerId: EntityId? = null
)

/**
 * The full projected game state.
 */
class ProjectedState(
    private val baseState: GameState,
    private val projectedValues: Map<EntityId, ProjectedValues>
) {
    /**
     * Get the base (unmodified) game state.
     */
    fun getBaseState(): GameState = baseState

    /**
     * Get projected power for an entity.
     */
    fun getPower(entityId: EntityId): Int? = projectedValues[entityId]?.power

    /**
     * Get projected toughness for an entity.
     */
    fun getToughness(entityId: EntityId): Int? = projectedValues[entityId]?.toughness

    /**
     * Get projected keywords for an entity.
     */
    fun getKeywords(entityId: EntityId): Set<String> = projectedValues[entityId]?.keywords ?: emptySet()

    /**
     * Check if an entity has a specific keyword.
     */
    fun hasKeyword(entityId: EntityId, keyword: String): Boolean =
        getKeywords(entityId).contains(keyword)

    /**
     * Check if an entity has a specific keyword.
     */
    fun hasKeyword(entityId: EntityId, keyword: Keyword): Boolean =
        hasKeyword(entityId, keyword.name)

    /**
     * Get projected colors for an entity.
     */
    fun getColors(entityId: EntityId): Set<String> = projectedValues[entityId]?.colors ?: emptySet()

    /**
     * Check if an entity has a specific color.
     */
    fun hasColor(entityId: EntityId, color: Color): Boolean =
        getColors(entityId).contains(color.name)

    /**
     * Get projected types for an entity.
     */
    fun getTypes(entityId: EntityId): Set<String> = projectedValues[entityId]?.types ?: emptySet()

    /**
     * Check if an entity has a specific type.
     */
    fun hasType(entityId: EntityId, type: String): Boolean =
        getTypes(entityId).contains(type)

    /**
     * Get the projected controller of an entity.
     */
    fun getController(entityId: EntityId): EntityId? = projectedValues[entityId]?.controllerId

    /**
     * Get all projected values for an entity.
     */
    fun getProjectedValues(entityId: EntityId): ProjectedValues? = projectedValues[entityId]

    /**
     * Get all entities on the battlefield with their projected values.
     */
    fun getAllProjectedValues(): Map<EntityId, ProjectedValues> = projectedValues
}
