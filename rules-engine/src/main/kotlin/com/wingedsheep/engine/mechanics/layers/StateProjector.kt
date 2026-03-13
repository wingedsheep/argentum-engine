package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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
    private val filterResolver = AffectsFilterResolver()
    private val effectApplicator = EffectApplicator(dynamicAmountEvaluator)
    private val effectSorter = EffectSorter()

    /**
     * Project the full game state with all continuous effects applied.
     */
    fun project(state: GameState): ProjectedState {
        val projectedValues = mutableMapOf<EntityId, MutableProjectedValues>()
        val dynamicStatEntities = mutableListOf<Pair<EntityId, CardComponent>>()

        // Initialize all permanents with their base values
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (container.has<FaceDownComponent>()) {
                projectedValues[entityId] = MutableProjectedValues(
                    power = 2,
                    toughness = 2,
                    keywords = mutableSetOf(),
                    colors = mutableSetOf(),
                    types = mutableSetOf("CREATURE"),
                    subtypes = mutableSetOf(),
                    controllerId = container.get<ControllerComponent>()?.playerId,
                    isFaceDown = true
                )
            } else {
                val baseStats = cardComponent.baseStats
                projectedValues[entityId] = MutableProjectedValues(
                    power = baseStats?.basePower,
                    toughness = baseStats?.baseToughness,
                    keywords = (cardComponent.baseKeywords.map { it.name } +
                        cardComponent.baseFlags.map { it.name } +
                        (container.get<ProtectionComponent>()?.colors?.map { "PROTECTION_FROM_${it.name}" } ?: emptyList()) +
                        (container.get<ProtectionComponent>()?.subtypes?.map { "PROTECTION_FROM_SUBTYPE_${it.uppercase()}" } ?: emptyList())).toMutableSet(),
                    colors = cardComponent.colors.map { it.name }.toMutableSet(),
                    types = extractTypes(cardComponent),
                    subtypes = cardComponent.typeLine.subtypes.map { it.value }.toMutableSet(),
                    controllerId = container.get<ControllerComponent>()?.playerId,
                    isFaceDown = false
                )

                if (Keyword.CHANGELING in cardComponent.baseKeywords) {
                    projectedValues[entityId]?.subtypes?.addAll(Subtype.ALL_CREATURE_TYPES)
                }

                if (baseStats?.isDynamic == true) {
                    dynamicStatEntities.add(entityId to cardComponent)
                }
            }
        }

        // Apply Layer 3 text-changing effects (TextReplacementComponent)
        applyTextReplacements(state, projectedValues)

        // Collect all active continuous effects
        val effects = collectContinuousEffects(state, projectedValues)

        // Sort effects by layer and dependency
        val sortedEffects = effectSorter.sortByLayerAndDependency(effects, state)

        // Apply continuous effects for layers 1-6 first (before CDA resolution)
        for (effect in sortedEffects) {
            if (effect.layer != Layer.POWER_TOUGHNESS) {
                effectApplicator.applyEffect(effect, state, projectedValues)
            }
        }

        // Resolve CDAs (Layer 7a) - evaluate dynamic power/toughness
        resolveCDAs(state, projectedValues, dynamicStatEntities)

        // Re-resolve affected entities for Layer 7 effects that depend on subtypes
        val resolvedLayer7Effects = sortedEffects.map { effect ->
            if (effect.layer == Layer.POWER_TOUGHNESS && effect.affectsFilter != null && filterResolver.isSubtypeDependentFilter(effect.affectsFilter)) {
                effect.copy(affectedEntities = filterResolver.resolveAffectedEntities(state, effect.sourceId, effect.affectsFilter, projectedValues))
            } else {
                effect
            }
        }

        // Apply layer 7 continuous effects
        for (effect in resolvedLayer7Effects) {
            if (effect.layer == Layer.POWER_TOUGHNESS) {
                effectApplicator.applyEffect(effect, state, projectedValues)
            }
        }

        // Apply counters (layer 7d)
        effectApplicator.applyCounters(state, projectedValues)

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
                mustBlock = v.mustBlock,
                cantBeBlockedExceptBySubtypes = v.cantBeBlockedExceptBySubtypes.toSet(),
                additionalBlockCount = v.additionalBlockCount,
                lostAllAbilities = v.lostAllAbilities
            )
        }

        return ProjectedState(state, finalValues)
    }

    fun getProjectedPower(state: GameState, entityId: EntityId): Int {
        val projected = project(state)
        return projected.getPower(entityId) ?: 0
    }

    fun getProjectedToughness(state: GameState, entityId: EntityId): Int {
        val projected = project(state)
        return projected.getToughness(entityId) ?: 0
    }

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

    fun hasProjectedKeyword(state: GameState, entityId: EntityId, keyword: Keyword): Boolean {
        val projected = project(state)
        return projected.hasKeyword(entityId, keyword)
    }

    private fun applyTextReplacements(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val textReplacement = container.get<TextReplacementComponent>() ?: continue
            val values = projectedValues[entityId] ?: continue

            val transformedSubtypes = values.subtypes.map { textReplacement.applyToCreatureType(it) }.toMutableSet()
            values.subtypes.clear()
            values.subtypes.addAll(transformedSubtypes)

            val oldSubtypesInTypes = values.types.filter { type ->
                container.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == type } == true
            }.toSet()
            values.types.removeAll(oldSubtypesInTypes)
            values.types.addAll(transformedSubtypes)

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
    }

    private fun extractTypes(card: CardComponent): MutableSet<String> {
        val types = mutableSetOf<String>()
        types.addAll(card.typeLine.supertypes.map { it.name })
        types.addAll(card.typeLine.cardTypes.map { it.name })
        types.addAll(card.typeLine.subtypes.map { it.value })
        return types
    }

    private fun collectContinuousEffects(
        state: GameState,
        projectedValues: Map<EntityId, MutableProjectedValues>
    ): List<ContinuousEffect> {
        val effects = mutableListOf<ContinuousEffect>()

        // 1. Collect effects from static abilities on permanents
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val continuousEffectComponent = container.get<ContinuousEffectSourceComponent>()
            if (continuousEffectComponent != null) {
                val textReplacement = container.get<TextReplacementComponent>()
                effects.addAll(continuousEffectComponent.effects.map { effect ->
                    val effectiveFilter = if (textReplacement != null && effect.affectsFilter != null) {
                        effect.affectsFilter.applyTextReplacement(textReplacement)
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
                        affectedEntities = filterResolver.resolveAffectedEntities(state, entityId, effectiveFilter, projectedValues),
                        sourceCondition = effect.sourceCondition,
                        affectsFilter = effectiveFilter
                    )
                })
            }
        }

        // 2. Collect floating effects (from resolved spells like Giant Growth)
        for (floating in state.floatingEffects) {
            if (floating.duration is Duration.WhileSourceTapped) {
                val sourceId = floating.sourceId
                if (sourceId == null || !state.getBattlefield().contains(sourceId) ||
                    state.getEntity(sourceId)?.has<TappedComponent>() != true) {
                    continue
                }
            }

            val validAffectedEntities = if (floating.effect.dynamicGroupFilter != null) {
                // Rule 611.2c: re-evaluate filter dynamically to include entities that entered later
                filterResolver.resolveAffectedEntities(
                    state,
                    floating.sourceId ?: EntityId("floating-${floating.id}"),
                    AffectsFilter.Generic(floating.effect.dynamicGroupFilter),
                    projectedValues
                )
            } else {
                floating.effect.affectedEntities.filter { entityId ->
                    state.getBattlefield().contains(entityId)
                }.toSet()
            }

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

    private fun resolveCDAs(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>,
        dynamicStatEntities: List<Pair<EntityId, CardComponent>>
    ) {
        if (dynamicStatEntities.isEmpty()) return

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

            fun resolveDynamicAmount(source: DynamicAmount): Int {
                val effective = if (textReplacement != null) {
                    source.applyTextReplacement(textReplacement)
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
                is CharacteristicValue.Fixed -> {}
            }
            when (val toughness = baseStats.toughness) {
                is CharacteristicValue.Dynamic ->
                    values.toughness = resolveDynamicAmount(toughness.source)
                is CharacteristicValue.DynamicWithOffset ->
                    values.toughness = resolveDynamicAmount(toughness.source) + toughness.offset
                is CharacteristicValue.Fixed -> {}
            }
        }
    }
}
