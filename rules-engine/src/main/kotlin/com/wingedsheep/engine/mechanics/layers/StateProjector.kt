package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.GrantCantBeBlockedToSmallCreaturesComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.HexproofFromColorComponent
import com.wingedsheep.engine.state.components.identity.ProtectionComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
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
/**
 * Mapping from keyword counter types to the keyword name they grant (Rule 122.1b).
 */
private val KEYWORD_COUNTER_MAP = mapOf(
    CounterType.FLYING to Keyword.FLYING.name,
    CounterType.INDESTRUCTIBLE to Keyword.INDESTRUCTIBLE.name
)

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
                        (container.get<ProtectionComponent>()?.subtypes?.map { "PROTECTION_FROM_SUBTYPE_${it.uppercase()}" } ?: emptyList()) +
                        (container.get<HexproofFromColorComponent>()?.colors?.map { "HEXPROOF_FROM_${it.name}" } ?: emptyList())).toMutableSet(),
                    colors = cardComponent.colors.map { it.name }.toMutableSet(),
                    types = extractTypes(cardComponent),
                    subtypes = cardComponent.typeLine.subtypes.map { it.value }.toMutableSet(),
                    controllerId = container.get<ControllerComponent>()?.playerId,
                    isFaceDown = false
                )

                // Rule 122.1b: Keyword counters grant their keyword
                val countersComponent = container.get<CountersComponent>()
                if (countersComponent != null) {
                    KEYWORD_COUNTER_MAP.forEach { (counterType, keywordName) ->
                        if (countersComponent.getCount(counterType) > 0) {
                            projectedValues[entityId]?.keywords?.add(keywordName)
                        }
                    }
                }

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
        // Split into two passes: Layer 2 (control) first, then re-resolve controller-dependent
        // filters for layers 3-6 so that control-changing effects are respected (e.g., stealing
        // Ainok Bond-Kin with Act of Treason should grant first strike to the new controller's creatures).
        val preControlEffects = sortedEffects.filter { it.layer == Layer.CONTROL }
        for (effect in preControlEffects) {
            effectApplicator.applyEffect(effect, state, projectedValues)
        }

        // Re-resolve affected entities for post-control layers with controller-dependent filters
        val postControlEffects = sortedEffects.filter { it.layer != Layer.CONTROL && it.layer != Layer.POWER_TOUGHNESS }
            .map { effect ->
                if (effect.affectsFilter != null && filterResolver.isControllerDependentFilter(effect.affectsFilter)) {
                    effect.copy(affectedEntities = filterResolver.resolveAffectedEntities(state, effect.sourceId, effect.affectsFilter, projectedValues))
                } else {
                    effect
                }
            }
        for (effect in postControlEffects) {
            effectApplicator.applyEffect(effect, state, projectedValues)
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

        // Post-layer pass: grant CANT_BE_BLOCKED to creatures qualifying via
        // GrantCantBeBlockedToSmallCreatures (e.g., Tetsuko Umezawa, Fugitive).
        // Must happen after all P/T layers so projected power/toughness is final.
        applyGrantCantBeBlockedToSmallCreatures(state, projectedValues)

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

    /**
     * Scan for permanents with GrantCantBeBlockedToSmallCreaturesComponent and
     * add CANT_BE_BLOCKED to creatures they control whose projected power or
     * toughness is at most the threshold.
     */
    private fun applyGrantCantBeBlockedToSmallCreatures(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        // Collect all grant sources: (controllerId, maxValue)
        val sources = mutableListOf<Pair<EntityId, Int>>()
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val grant = container.get<GrantCantBeBlockedToSmallCreaturesComponent>() ?: continue
            val controllerId = projectedValues[entityId]?.controllerId ?: continue
            sources.add(controllerId to grant.maxValue)
        }
        if (sources.isEmpty()) return

        // For each creature on the battlefield, check if any source applies
        for (entityId in state.getBattlefield()) {
            val values = projectedValues[entityId] ?: continue
            if (!values.types.contains("CREATURE")) continue
            val power = values.power ?: continue
            val toughness = values.toughness ?: continue
            val controllerId = values.controllerId ?: continue

            for ((sourceController, maxValue) in sources) {
                if (sourceController == controllerId && (power <= maxValue || toughness <= maxValue)) {
                    values.keywords.add(AbilityFlag.CANT_BE_BLOCKED.name)
                    break
                }
            }
        }
    }
}
