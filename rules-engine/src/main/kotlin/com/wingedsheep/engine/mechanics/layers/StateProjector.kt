package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.mechanics.text.SubtypeReplacer
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

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
class StateProjector(
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(projectForBattlefieldCounting = false)
) {

    /**
     * Project the full game state with all continuous effects applied.
     */
    fun project(state: GameState): ProjectedState {
        // Initialize projected values from base state
        val projectedValues = mutableMapOf<EntityId, MutableProjectedValues>()

        // Track entities with dynamic stats (CDAs) for resolution after initialization
        val dynamicStatEntities = mutableListOf<Pair<EntityId, CardComponent>>()

        // Initialize all permanents with their base values
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down creatures are 2/2 colorless creatures with no name, types, or abilities
            if (container.has<FaceDownComponent>()) {
                projectedValues[entityId] = MutableProjectedValues(
                    power = 2,
                    toughness = 2,
                    keywords = mutableSetOf(),  // No keywords
                    colors = mutableSetOf(),     // Colorless
                    types = mutableSetOf("CREATURE"),  // Just creature type
                    subtypes = mutableSetOf(),   // No subtypes (Rule 707.2)
                    controllerId = container.get<ControllerComponent>()?.playerId,
                    isFaceDown = true
                )
            } else {
                val baseStats = cardComponent.baseStats
                projectedValues[entityId] = MutableProjectedValues(
                    power = baseStats?.basePower,  // null for dynamic stats
                    toughness = baseStats?.baseToughness,  // null for dynamic stats
                    keywords = (cardComponent.baseKeywords.map { it.name } +
                        (container.get<ProtectionComponent>()?.colors?.map { "PROTECTION_FROM_${it.name}" } ?: emptyList()) +
                        (container.get<ProtectionComponent>()?.subtypes?.map { "PROTECTION_FROM_SUBTYPE_${it.uppercase()}" } ?: emptyList())).toMutableSet(),
                    colors = cardComponent.colors.map { it.name }.toMutableSet(),
                    types = extractTypes(cardComponent),
                    subtypes = cardComponent.typeLine.subtypes.map { it.value }.toMutableSet(),
                    controllerId = container.get<ControllerComponent>()?.playerId,
                    isFaceDown = false
                )

                // Track entities with dynamic stats for CDA resolution
                if (baseStats?.isDynamic == true) {
                    dynamicStatEntities.add(entityId to cardComponent)
                }
            }
        }

        // Apply Layer 3 text-changing effects (TextReplacementComponent)
        // This modifies subtypes and protection keywords before other layers are applied
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val textReplacement = container.get<TextReplacementComponent>() ?: continue
            val values = projectedValues[entityId] ?: continue

            val transformedSubtypes = values.subtypes.map { textReplacement.applyToCreatureType(it) }.toMutableSet()
            values.subtypes.clear()
            values.subtypes.addAll(transformedSubtypes)

            // Also update the 'types' set which includes subtypes
            val oldSubtypesInTypes = values.types.filter { type ->
                // Check if this type value was a subtype (not a card type or supertype)
                container.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == type } == true
            }.toSet()
            values.types.removeAll(oldSubtypesInTypes)
            values.types.addAll(transformedSubtypes)

            // Also update protection-from-subtype keywords (Rule 702.16 + Layer 3)
            // e.g., "Protection from Goblins" → "Protection from Elves" when text changes Goblin → Elf
            val protectionSubtypePrefix = "PROTECTION_FROM_SUBTYPE_"
            val protectionKeywords = values.keywords.filter { it.startsWith(protectionSubtypePrefix) }
            for (keyword in protectionKeywords) {
                val originalSubtype = keyword.removePrefix(protectionSubtypePrefix)
                val transformed = textReplacement.applyToCreatureType(originalSubtype).uppercase()
                if (transformed != originalSubtype) {
                    values.keywords.remove(keyword)
                    values.keywords.add("$protectionSubtypePrefix$transformed")
                }
            }
        }

        // Collect all active continuous effects (pass projected values so subtype filters use Layer 3 results)
        val effects = collectContinuousEffects(state, projectedValues)

        // Sort effects by layer and dependency
        val sortedEffects = sortByLayerAndDependency(effects, state)

        // Apply continuous effects for layers 1-6 first (before CDA resolution)
        // This ensures type-changing effects (Layer 4) are applied before CDAs count subtypes
        for (effect in sortedEffects) {
            if (effect.layer != Layer.POWER_TOUGHNESS) {
                applyEffect(effect, state, projectedValues)
            }
        }

        // Resolve CDAs (Layer 7a) - evaluate dynamic power/toughness
        // Done after layers 1-6 so type changes are visible to AggregateBattlefield
        if (dynamicStatEntities.isNotEmpty()) {
            // Build intermediate projected state so counting uses updated types/subtypes
            val intermediateProjected = buildIntermediateProjectedState(state, projectedValues)
            for ((entityId, cardComponent) in dynamicStatEntities) {
                val values = projectedValues[entityId] ?: continue
                val controllerId = values.controllerId ?: continue
                val context = EffectContext(
                    sourceId = entityId,
                    controllerId = controllerId,
                    opponentId = state.getOpponent(controllerId)
                )
                val baseStats = cardComponent.baseStats ?: continue
                val textReplacement = state.getEntity(entityId)?.get<TextReplacementComponent>()

                fun resolveDynamicAmount(source: com.wingedsheep.sdk.scripting.DynamicAmount): Int {
                    val effective = if (textReplacement != null) {
                        SubtypeReplacer.replaceDynamicAmount(source, textReplacement)
                    } else {
                        source
                    }
                    return dynamicAmountEvaluator.evaluate(state, effective, context, intermediateProjected)
                }

                when (val power = baseStats.power) {
                    is CharacteristicValue.Dynamic ->
                        values.power = resolveDynamicAmount(power.source)
                    is CharacteristicValue.DynamicWithOffset ->
                        values.power = resolveDynamicAmount(power.source) + power.offset
                    is CharacteristicValue.Fixed -> {} // Already set
                }
                when (val toughness = baseStats.toughness) {
                    is CharacteristicValue.Dynamic ->
                        values.toughness = resolveDynamicAmount(toughness.source)
                    is CharacteristicValue.DynamicWithOffset ->
                        values.toughness = resolveDynamicAmount(toughness.source) + toughness.offset
                    is CharacteristicValue.Fixed -> {} // Already set
                }
            }
        }

        // Re-resolve affected entities for Layer 7 effects that depend on subtypes,
        // since Layer 4 type-changing effects may have changed creature subtypes.
        // For example, Imagecrafter changing a Soldier to a Beast should cause
        // Aven Brigadier's "Other Soldier creatures get +1/+1" to stop applying.
        val resolvedLayer7Effects = sortedEffects.map { effect ->
            if (effect.layer == Layer.POWER_TOUGHNESS && effect.affectsFilter != null && isSubtypeDependentFilter(effect.affectsFilter)) {
                effect.copy(affectedEntities = resolveAffectedEntities(state, effect.sourceId, effect.affectsFilter, projectedValues))
            } else {
                effect
            }
        }

        // Apply layer 7 continuous effects (P/T modifications from spells/abilities)
        for (effect in resolvedLayer7Effects) {
            if (effect.layer == Layer.POWER_TOUGHNESS) {
                applyEffect(effect, state, projectedValues)
            }
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
                subtypes = v.subtypes.toSet(),
                controllerId = v.controllerId,
                isFaceDown = v.isFaceDown,
                cantAttack = v.cantAttack,
                cantBlock = v.cantBlock,
                mustAttack = v.mustAttack,
                mustBlock = v.mustBlock
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
     * Get the projected keywords of an entity.
     */
    fun getProjectedKeywords(state: GameState, entityId: EntityId): Set<Keyword> {
        val projected = project(state)
        return projected.getKeywords(entityId).mapNotNull { keywordName ->
            try {
                Keyword.valueOf(keywordName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()
    }

    /**
     * Check if an entity has a specific keyword after applying continuous effects.
     */
    fun hasProjectedKeyword(state: GameState, entityId: EntityId, keyword: Keyword): Boolean {
        val projected = project(state)
        return projected.hasKeyword(entityId, keyword)
    }

    /**
     * Build an intermediate ProjectedState from the in-progress projected values.
     * Used during CDA resolution so that AggregateBattlefield can see type changes
     * from layers 1-6 (especially Layer 4 type-changing effects).
     */
    private fun buildIntermediateProjectedState(
        state: GameState,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): ProjectedState {
        val frozen = projectedValues.mapValues { (_, v) ->
            ProjectedValues(
                power = v.power,
                toughness = v.toughness,
                keywords = v.keywords.toSet(),
                colors = v.colors.toSet(),
                types = v.types.toSet(),
                subtypes = v.subtypes.toSet(),
                controllerId = v.controllerId,
                isFaceDown = v.isFaceDown,
                cantAttack = v.cantAttack,
                cantBlock = v.cantBlock,
                mustAttack = v.mustAttack,
                mustBlock = v.mustBlock
            )
        }
        return ProjectedState(state, frozen)
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
    private fun collectContinuousEffects(
        state: GameState,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): List<ContinuousEffect> {
        val effects = mutableListOf<ContinuousEffect>()

        // 1. Collect effects from static abilities on permanents
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue

            // Check for continuous effect components
            val continuousEffectComponent = container.get<ContinuousEffectSourceComponent>()
            if (continuousEffectComponent != null) {
                // Apply text replacement to AffectsFilter if source has been text-changed (Gap 1)
                val textReplacement = container.get<TextReplacementComponent>()
                effects.addAll(continuousEffectComponent.effects.map { effect ->
                    val effectiveFilter = if (textReplacement != null && effect.affectsFilter != null) {
                        SubtypeReplacer.replaceAffectsFilter(effect.affectsFilter, textReplacement)
                    } else {
                        effect.affectsFilter
                    }
                    ContinuousEffect(
                        sourceId = entityId,
                        layer = effect.layer,
                        sublayer = effect.sublayer,
                        timestamp = container.get<com.wingedsheep.engine.state.components.battlefield.TimestampComponent>()?.timestamp
                            ?: state.timestamp,
                        modification = effect.modification,
                        affectedEntities = resolveAffectedEntities(state, entityId, effectiveFilter, projectedValues),
                        sourceCondition = effect.sourceCondition,
                        affectsFilter = effectiveFilter
                    )
                })
            }
        }

        // 2. Collect floating effects (from resolved spells like Giant Growth)
        for (floating in state.floatingEffects) {
            // Skip WhileSourceTapped effects if source is no longer tapped or on battlefield
            if (floating.duration is Duration.WhileSourceTapped) {
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId) ||
                    state.getEntity(sourceId)?.has<TappedComponent>() != true) {
                    continue
                }
            }

            // Only include effects whose affected entities still exist on battlefield
            val validAffectedEntities = floating.effect.affectedEntities.filter { entityId ->
                state.getBattlefield().contains(entityId)
            }.toSet()

            if (validAffectedEntities.isNotEmpty()) {
                effects.add(
                    ContinuousEffect(
                        sourceId = floating.sourceId ?: EntityId("floating-${floating.id}"),
                        layer = floating.effect.layer,
                        sublayer = floating.effect.sublayer,
                        timestamp = floating.timestamp,
                        modification = floating.effect.modification.toModification(),
                        affectedEntities = validAffectedEntities
                    )
                )
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
        filter: AffectsFilter?,
        projectedValues: Map<EntityId, MutableProjectedValues> = emptyMap()
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
                    // Face-down permanents are always creatures (Rule 707.2)
                    (card.typeLine.isCreature || container.has<FaceDownComponent>()) && entityController == controller
                }.toSet()
            }
            is AffectsFilter.AllCreatures -> {
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    // Face-down permanents are always creatures (Rule 707.2)
                    container.get<CardComponent>()?.typeLine?.isCreature == true || container.has<FaceDownComponent>()
                }.toSet()
            }
            is AffectsFilter.AllCreaturesOpponentsControl -> {
                val controller = state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val entityController = container.get<ControllerComponent>()?.playerId
                    // Face-down permanents are always creatures (Rule 707.2)
                    (card.typeLine.isCreature || container.has<FaceDownComponent>()) && entityController != controller
                }.toSet()
            }
            is AffectsFilter.SpecificEntities -> filter.entityIds
            is AffectsFilter.WithSubtype -> {
                state.getBattlefield().filter { entityId ->
                    val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
                    val projected = projectedValues[entityId]
                    if (projected != null) {
                        projected.subtypes.any { it.equals(filter.subtype, ignoreCase = true) }
                    } else {
                        card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(filter.subtype))
                    }
                }.toSet()
            }
            is AffectsFilter.OtherCreaturesWithSubtype -> {
                state.getBattlefield().filter { entityId ->
                    if (entityId == sourceId) return@filter false // Exclude self
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val projected = projectedValues[entityId]
                    val hasSubtype = if (projected != null) {
                        projected.subtypes.any { it.equals(filter.subtype, ignoreCase = true) }
                    } else {
                        card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(filter.subtype))
                    }
                    // Face-down permanents are always creatures (Rule 707.2)
                    (card.typeLine.isCreature || container.has<FaceDownComponent>()) && hasSubtype
                }.toSet()
            }
            is AffectsFilter.OtherTappedCreaturesYouControl -> {
                val controller = state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    if (entityId == sourceId) return@filter false // Exclude self
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val entityController = container.get<ControllerComponent>()?.playerId
                    val isTapped = container.has<TappedComponent>()
                    // Face-down permanents are always creatures (Rule 707.2)
                    (card.typeLine.isCreature || container.has<FaceDownComponent>()) && entityController == controller && isTapped
                }.toSet()
            }
            is AffectsFilter.OtherCreaturesYouControl -> {
                val controller = state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    if (entityId == sourceId) return@filter false // Exclude self
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val entityController = container.get<ControllerComponent>()?.playerId
                    // Face-down permanents are always creatures (Rule 707.2)
                    (card.typeLine.isCreature || container.has<FaceDownComponent>()) && entityController == controller
                }.toSet()
            }
            is AffectsFilter.AllOtherCreatures -> {
                state.getBattlefield().filter { entityId ->
                    if (entityId == sourceId) return@filter false // Exclude self
                    val container = state.getEntity(entityId) ?: return@filter false
                    // Face-down permanents are always creatures (Rule 707.2)
                    container.get<CardComponent>()?.typeLine?.isCreature == true || container.has<FaceDownComponent>()
                }.toSet()
            }
            is AffectsFilter.AttachedPermanent -> {
                val attachedTo = state.getEntity(sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                if (attachedTo != null) setOf(attachedTo.targetId) else emptySet()
            }
            is AffectsFilter.FaceDownCreatures -> {
                state.getBattlefield().filter { entityId ->
                    state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }.toSet()
            }
            is AffectsFilter.CreaturesWithCounter -> {
                val counterType = try {
                    CounterType.valueOf(
                        filter.counterType.uppercase()
                            .replace(' ', '_')
                            .replace('+', 'P')
                            .replace('-', 'M')
                            .replace("/", "_")
                    )
                } catch (e: IllegalArgumentException) {
                    return emptySet()
                }
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val counters = container.get<CountersComponent>()
                    // Face-down permanents are always creatures (Rule 707.2)
                    (card.typeLine.isCreature || container.has<FaceDownComponent>()) && (counters?.getCount(counterType) ?: 0) > 0
                }.toSet()
            }
            is AffectsFilter.ChosenCreatureTypeCreatures -> {
                val chosenType = state.getEntity(sourceId)
                    ?.get<ChosenCreatureTypeComponent>()?.creatureType
                    ?: return emptySet()
                state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val projected = projectedValues[entityId]
                    val hasSubtype = if (projected != null) {
                        projected.subtypes.any { it.equals(chosenType, ignoreCase = true) }
                    } else {
                        card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(chosenType))
                    }
                    // Face-down permanents are always creatures (Rule 707.2)
                    (card.typeLine.isCreature || container.has<FaceDownComponent>()) && hasSubtype
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
        // Use identity-based tracking to avoid deduplicating equal-but-distinct effects
        // (e.g., two +1/+1 lord effects from the same source affecting the same entities)
        val result = mutableListOf<ContinuousEffect>()
        val remaining = effects.toMutableList()

        while (remaining.isNotEmpty()) {
            // Find effects with no remaining dependencies
            val ready = remaining.filter { effect ->
                dependencies[effect]?.none { dep -> remaining.any { it === dep } } ?: true
            }.sortedBy { it.timestamp } // Timestamp tiebreaker

            if (ready.isEmpty()) {
                // Circular dependency - fall back to timestamp
                result.addAll(remaining.sortedBy { it.timestamp })
                break
            }

            val next = ready.first()
            result.add(next)
            remaining.removeAt(remaining.indexOfFirst { it === next })
        }

        return result
    }

    /**
     * Check if an AffectsFilter depends on creature subtypes.
     * These filters need re-resolution after Layer 4 type-changing effects.
     */
    private fun isSubtypeDependentFilter(filter: AffectsFilter): Boolean {
        return filter is AffectsFilter.OtherCreaturesWithSubtype ||
            filter is AffectsFilter.WithSubtype ||
            filter is AffectsFilter.ChosenCreatureTypeCreatures
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
        // Check source projection condition against current projected values
        val sourceCondition = effect.sourceCondition
        if (sourceCondition != null) {
            val sourceValues = projectedValues[effect.sourceId]
            val conditionMet = when (sourceCondition) {
                is SourceProjectionCondition.HasSubtype ->
                    sourceValues?.subtypes?.any { it.equals(sourceCondition.subtype, ignoreCase = true) } == true
                is SourceProjectionCondition.ControllerControlsCreatureOfType -> {
                    val controllerId = sourceValues?.controllerId
                    if (controllerId != null) {
                        state.getBattlefield(controllerId).any { entityId ->
                            entityId != effect.sourceId &&
                            projectedValues[entityId]?.subtypes?.any {
                                it.equals(sourceCondition.subtype, ignoreCase = true)
                            } == true
                        }
                    } else false
                }
            }
            if (!conditionMet) return
        }

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
                is Modification.AddSubtype -> {
                    values.types.add(mod.subtype)
                    values.subtypes.add(mod.subtype)
                }
                is Modification.RemoveType -> {
                    values.types.remove(mod.type)
                }
                is Modification.SetCreatureSubtypes -> {
                    // Remove all creature subtypes from types and subtypes sets
                    val creatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES.toSet()
                    values.subtypes.removeAll { it in creatureTypes }
                    values.types.removeAll { it in creatureTypes }
                    // Add the new creature subtypes
                    values.subtypes.addAll(mod.subtypes)
                    values.types.addAll(mod.subtypes)
                }
                is Modification.SetBasicLandTypes -> {
                    // Remove all basic land subtypes (Rule 305.7)
                    val basicLandTypes = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES
                    values.subtypes.removeAll { it in basicLandTypes }
                    values.types.removeAll { it in basicLandTypes }
                    // Add the new land subtypes
                    values.subtypes.addAll(mod.subtypes)
                    values.types.addAll(mod.subtypes)
                }
                is Modification.ChangeController -> {
                    values.controllerId = mod.newControllerId
                }
                is Modification.ChangeControllerToSourceController -> {
                    val sourceController = state.getEntity(effect.sourceId)
                        ?.get<ControllerComponent>()?.playerId
                    if (sourceController != null) {
                        values.controllerId = sourceController
                    }
                }
                is Modification.GrantProtectionFromColor -> {
                    values.keywords.add("PROTECTION_FROM_${mod.color}")
                }
                is Modification.SetCantAttack -> {
                    values.cantAttack = true
                }
                is Modification.SetCantBlock -> {
                    values.cantBlock = true
                }
                is Modification.SetMustAttack -> {
                    values.mustAttack = true
                }
                is Modification.SetMustBlock -> {
                    values.mustBlock = true
                }
                is Modification.ModifyPowerToughnessPerSourceCounter -> {
                    // Read counter count from the source permanent (e.g., the aura)
                    val counterType = try {
                        CounterType.valueOf(
                            mod.counterType.uppercase()
                                .replace(' ', '_')
                                .replace('+', 'P')
                                .replace('-', 'M')
                                .replace("/", "_")
                        )
                    } catch (e: IllegalArgumentException) { null }
                    val counterCount = if (counterType != null) {
                        state.getEntity(effect.sourceId)
                            ?.get<CountersComponent>()
                            ?.getCount(counterType) ?: 0
                    } else 0
                    if (counterCount > 0) {
                        values.power = (values.power ?: 0) + mod.powerModPerCounter * counterCount
                        values.toughness = (values.toughness ?: 0) + mod.toughnessModPerCounter * counterCount
                    }
                }
                is Modification.NoOp -> {
                    // No-op: effect doesn't modify projected state (e.g., combat restrictions)
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
@Serializable
data class ContinuousEffectSourceComponent(
    val effects: List<ContinuousEffectData>
) : com.wingedsheep.engine.state.Component

/**
 * Data for a single continuous effect.
 */
@Serializable
data class ContinuousEffectData(
    val layer: Layer,
    val sublayer: Sublayer? = null,
    val modification: Modification,
    val affectsFilter: AffectsFilter? = null,
    val sourceCondition: SourceProjectionCondition? = null
)

/**
 * Filter for determining which entities are affected.
 */
@Serializable
sealed interface AffectsFilter {
    @Serializable
    data object Self : AffectsFilter
    @Serializable
    data object AllCreatures : AffectsFilter
    @Serializable
    data object AllCreaturesYouControl : AffectsFilter
    @Serializable
    data object AllCreaturesOpponentsControl : AffectsFilter
    @Serializable
    data class SpecificEntities(val entityIds: Set<EntityId>) : AffectsFilter
    @Serializable
    data class WithSubtype(val subtype: String) : AffectsFilter

    /**
     * Other creatures with a specific subtype (excludes the source permanent).
     * Used for lord effects like "Other Bird creatures get +1/+1."
     */
    @Serializable
    data class OtherCreaturesWithSubtype(val subtype: String) : AffectsFilter

    /**
     * Other tapped creatures you control (excludes the source permanent).
     * Used for effects like "Other tapped creatures you control have indestructible."
     */
    @Serializable
    data object OtherTappedCreaturesYouControl : AffectsFilter

    /**
     * Other creatures you control (excludes the source permanent).
     * Used for lord effects like "Other creatures you control get +1/+1."
     */
    @Serializable
    data object OtherCreaturesYouControl : AffectsFilter

    /**
     * All other creatures (excludes the source permanent).
     */
    @Serializable
    data object AllOtherCreatures : AffectsFilter

    /**
     * The permanent this Aura is attached to.
     * Used for Aura effects like "You control enchanted permanent."
     */
    @Serializable
    data object AttachedPermanent : AffectsFilter

    /**
     * All creatures that have a specific counter type.
     * Used for Aurification: "Each creature with a gold counter on it..."
     */
    @Serializable
    data class CreaturesWithCounter(val counterType: String) : AffectsFilter

    /**
     * All face-down creatures.
     * Used for Ixidor, Reality Sculptor: "Face-down creatures get +1/+1."
     */
    @Serializable
    data object FaceDownCreatures : AffectsFilter

    /**
     * All creatures of the chosen creature type (resolved dynamically from source's ChosenCreatureTypeComponent).
     * Used for Shared Triumph: "Creatures of the chosen type get +1/+1."
     */
    @Serializable
    data object ChosenCreatureTypeCreatures : AffectsFilter
}

/**
 * Represents a continuous effect that modifies the game state.
 */
@Serializable
data class ContinuousEffect(
    val sourceId: EntityId,
    val layer: Layer,
    val sublayer: Sublayer? = null,
    val timestamp: Long,
    val modification: Modification,
    val affectedEntities: Set<EntityId> = emptySet(),
    val sourceCondition: SourceProjectionCondition? = null,
    val affectsFilter: AffectsFilter? = null
)

/**
 * Conditions evaluated during state projection against projected values.
 *
 * Unlike SDK Conditions (evaluated by ConditionEvaluator against base GameState),
 * these conditions are checked during layer application so they see the effects
 * of earlier layers. For example, a Layer 6 ability condition can see Layer 4
 * type changes.
 */
@Serializable
sealed interface SourceProjectionCondition {
    /**
     * The source permanent must have a specific creature subtype.
     * Used for "has [keyword] as long as it's a [subtype]."
     */
    @Serializable
    data class HasSubtype(val subtype: String) : SourceProjectionCondition

    /**
     * The source permanent's controller must control a creature with a specific subtype.
     * Used for "has [keyword] as long as you control a [subtype]."
     */
    @Serializable
    data class ControllerControlsCreatureOfType(val subtype: String) : SourceProjectionCondition
}

/**
 * The layers in which continuous effects are applied (Rule 613).
 */
@Serializable
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
@Serializable
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
@Serializable
sealed interface Modification {
    @Serializable
    data class SetPowerToughness(val power: Int, val toughness: Int) : Modification
    @Serializable
    data class ModifyPowerToughness(val powerMod: Int, val toughnessMod: Int) : Modification
    @Serializable
    data class SwitchPowerToughness(val targetId: EntityId) : Modification
    @Serializable
    data class GrantKeyword(val keyword: String) : Modification
    @Serializable
    data class RemoveKeyword(val keyword: String) : Modification
    @Serializable
    data class ChangeColor(val colors: Set<String>) : Modification
    @Serializable
    data class AddColor(val colors: Set<String>) : Modification
    @Serializable
    data class AddType(val type: String) : Modification
    @Serializable
    data class RemoveType(val type: String) : Modification

    /**
     * Add a subtype (also adds to types set).
     * Used for effects like "is a Wall in addition to its other creature types."
     */
    @Serializable
    data class AddSubtype(val subtype: String) : Modification

    /**
     * Replace all creature subtypes with the given set.
     * Used by "becomes the creature type of your choice" effects.
     */
    @Serializable
    data class SetCreatureSubtypes(val subtypes: Set<String>) : Modification

    /**
     * Replace all basic land subtypes with the given set (Rule 305.7).
     * Used by "is an Island" effects that change a land's basic land types.
     * Removes existing basic land subtypes (Plains, Island, Swamp, Mountain, Forest)
     * and replaces them with the specified types.
     */
    @Serializable
    data class SetBasicLandTypes(val subtypes: Set<String>) : Modification
    @Serializable
    data class ChangeController(val newControllerId: EntityId) : Modification

    /**
     * Change controller to whoever controls the source of this effect.
     * Used for Aura effects like "You control enchanted permanent" where
     * the controller is resolved dynamically at projection time.
     */
    @Serializable
    data object ChangeControllerToSourceController : Modification

    @Serializable
    data class GrantProtectionFromColor(val color: String) : Modification

    @Serializable
    data object SetCantAttack : Modification
    @Serializable
    data object SetCantBlock : Modification

    @Serializable
    data object SetMustAttack : Modification
    @Serializable
    data object SetMustBlock : Modification

    /**
     * Dynamic power/toughness modification based on counters on the source permanent.
     * The actual modification is computed at projection time by reading counter count from source.
     */
    @Serializable
    data class ModifyPowerToughnessPerSourceCounter(
        val counterType: String,
        val powerModPerCounter: Int,
        val toughnessModPerCounter: Int
    ) : Modification

    /** No-op modification for effects that don't modify projected state (e.g., combat restrictions) */
    @Serializable
    data object NoOp : Modification
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
    val subtypes: MutableSet<String> = mutableSetOf(),
    var controllerId: EntityId? = null,
    var isFaceDown: Boolean = false,
    var cantAttack: Boolean = false,
    var cantBlock: Boolean = false,
    var mustAttack: Boolean = false,
    var mustBlock: Boolean = false
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
    val subtypes: Set<String> = emptySet(),
    val controllerId: EntityId? = null,
    val isFaceDown: Boolean = false,
    val cantAttack: Boolean = false,
    val cantBlock: Boolean = false,
    val mustAttack: Boolean = false,
    val mustBlock: Boolean = false
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
     * Get projected subtypes for an entity.
     * Face-down creatures have no subtypes (Rule 707.2).
     */
    fun getSubtypes(entityId: EntityId): Set<String> = projectedValues[entityId]?.subtypes ?: emptySet()

    /**
     * Check if an entity has a specific subtype.
     */
    fun hasSubtype(entityId: EntityId, subtype: String): Boolean =
        getSubtypes(entityId).any { it.equals(subtype, ignoreCase = true) }

    /**
     * Check if an entity is face-down.
     */
    fun isFaceDown(entityId: EntityId): Boolean = projectedValues[entityId]?.isFaceDown == true

    /**
     * Check if an entity can't attack (e.g., enchanted by Pacifism).
     */
    fun cantAttack(entityId: EntityId): Boolean = projectedValues[entityId]?.cantAttack == true

    /**
     * Check if an entity can't block (e.g., enchanted by Pacifism).
     */
    fun cantBlock(entityId: EntityId): Boolean = projectedValues[entityId]?.cantBlock == true

    /**
     * Check if an entity must attack each combat if able (e.g., Grand Melee).
     */
    fun mustAttack(entityId: EntityId): Boolean = projectedValues[entityId]?.mustAttack == true

    /**
     * Check if an entity must block each combat if able (e.g., Grand Melee).
     */
    fun mustBlock(entityId: EntityId): Boolean = projectedValues[entityId]?.mustBlock == true

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

    /**
     * Get all battlefield entities whose projected controller is the given player.
     * This accounts for control-changing effects (Rule 613 Layer 2).
     */
    fun getBattlefieldControlledBy(playerId: EntityId): List<EntityId> {
        return baseState.getBattlefield().filter { entityId ->
            getController(entityId) == playerId
        }
    }
}
