package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId

/**
 * Applies continuous effects and counters to mutable projected values.
 */
internal class EffectApplicator(
    private val dynamicAmountEvaluator: DynamicAmountEvaluator
) {

    fun applyEffect(
        effect: ContinuousEffect,
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        val sourceCondition = effect.sourceCondition
        if (sourceCondition != null) {
            val sourceValues = projectedValues[effect.sourceId]
            val conditionMet = evaluateSourceCondition(sourceCondition, effect, state, projectedValues, sourceValues)
            if (!conditionMet) return
        }

        for (entityId in effect.affectedEntities) {
            val values = projectedValues.getOrPut(entityId) { MutableProjectedValues() }

            when (val mod = effect.modification) {
                is Modification.SetPowerToughness -> {
                    values.power = mod.power
                    values.toughness = mod.toughness
                }
                is Modification.SetPower -> {
                    values.power = mod.power
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
                    val creatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES.toSet()
                    values.subtypes.removeAll { it in creatureTypes }
                    values.types.removeAll { it in creatureTypes }
                    values.subtypes.addAll(mod.subtypes)
                    values.types.addAll(mod.subtypes)
                }
                is Modification.SetBasicLandTypes -> {
                    val basicLandTypes = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES
                    values.subtypes.removeAll { it in basicLandTypes }
                    values.types.removeAll { it in basicLandTypes }
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
                is Modification.GrantProtectionFromChosenColor -> {
                    val chosenColor = state.getEntity(effect.sourceId)
                        ?.get<ChosenColorComponent>()?.color
                    if (chosenColor != null) {
                        values.keywords.add("PROTECTION_FROM_${chosenColor.name}")
                    }
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
                is Modification.CanBlockAdditional -> {
                    values.additionalBlockCount += mod.count
                }
                is Modification.ModifyPowerToughnessPerSourceCounter -> {
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
                is Modification.ModifyPowerToughnessPerSharedCreatureType -> {
                    val entitySubtypes = values.subtypes
                    if (entitySubtypes.isNotEmpty()) {
                        val creatureSubtypes = entitySubtypes.toSet()
                        val count = state.getBattlefield().count { otherId ->
                            if (otherId == entityId) return@count false
                            val otherValues = projectedValues[otherId] ?: return@count false
                            val isCreature = "CREATURE" in otherValues.types
                            if (!isCreature) return@count false
                            otherValues.subtypes.any { it in creatureSubtypes }
                        }
                        if (count > 0) {
                            values.power = (values.power ?: 0) + mod.powerModPerCreature * count
                            values.toughness = (values.toughness ?: 0) + mod.toughnessModPerCreature * count
                        }
                    }
                }
                is Modification.CantBeBlockedExceptBySubtype -> {
                    values.cantBeBlockedExceptBySubtypes.add(mod.subtype)
                }
                is Modification.ModifyPowerToughnessDynamic -> {
                    val controllerId = projectedValues[effect.sourceId]?.controllerId
                        ?: state.getEntity(effect.sourceId)?.get<ControllerComponent>()?.playerId
                    if (controllerId != null) {
                        val context = EffectContext(
                            sourceId = effect.sourceId,
                            controllerId = controllerId,
                            opponentId = state.getOpponent(controllerId)
                        )
                        val intermediateProjected = buildIntermediateProjectedState(state, projectedValues)
                        val powerMod = dynamicAmountEvaluator.evaluate(state, mod.powerBonus, context, intermediateProjected)
                        val toughnessMod = dynamicAmountEvaluator.evaluate(state, mod.toughnessBonus, context, intermediateProjected)
                        values.power = (values.power ?: 0) + powerMod
                        values.toughness = (values.toughness ?: 0) + toughnessMod
                    }
                }
                is Modification.RemoveAllAbilities -> {
                    values.keywords.clear()
                    values.lostAllAbilities = true
                }
                is Modification.NoOp -> {
                    // No-op: effect doesn't modify projected state
                }
            }
        }
    }

    fun evaluateSourceCondition(
        condition: SourceProjectionCondition,
        effect: ContinuousEffect,
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>,
        sourceValues: MutableProjectedValues?
    ): Boolean = when (condition) {
        is SourceProjectionCondition.HasSubtype ->
            sourceValues?.subtypes?.any { it.equals(condition.subtype, ignoreCase = true) } == true
        is SourceProjectionCondition.HasKeyword ->
            sourceValues?.keywords?.any { it.equals(condition.keyword, ignoreCase = true) } == true
        is SourceProjectionCondition.ControllerControlsCreatureOfType -> {
            val controllerId = sourceValues?.controllerId
            if (controllerId != null) {
                state.getBattlefield(controllerId).any { entityId ->
                    entityId != effect.sourceId &&
                    projectedValues[entityId]?.subtypes?.any {
                        it.equals(condition.subtype, ignoreCase = true)
                    } == true
                }
            } else false
        }
        is SourceProjectionCondition.EnchantedCreatureHasSubtype -> {
            val attachedTo = state.getEntity(effect.sourceId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
            if (attachedTo != null) {
                projectedValues[attachedTo.targetId]?.subtypes?.any {
                    it.equals(condition.subtype, ignoreCase = true)
                } == true
            } else false
        }
        is SourceProjectionCondition.OpponentControlsCreature -> {
            val controllerId = sourceValues?.controllerId
            if (controllerId != null) {
                state.getBattlefield().any { entityId ->
                    val values = projectedValues[entityId]
                    values?.types?.contains("CREATURE") == true &&
                    values.controllerId != null &&
                    values.controllerId != controllerId
                }
            } else false
        }
        is SourceProjectionCondition.Not -> !evaluateSourceCondition(condition.condition, effect, state, projectedValues, sourceValues)
    }

    fun applyCounters(
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val values = projectedValues.getOrPut(entityId) { MutableProjectedValues() }

            val plusOneCounters = counters.getCount(CounterType.PLUS_ONE_PLUS_ONE)
            val minusOneCounters = counters.getCount(CounterType.MINUS_ONE_MINUS_ONE)

            val netCounters = plusOneCounters - minusOneCounters

            if (netCounters != 0) {
                values.power = (values.power ?: 0) + netCounters
                values.toughness = (values.toughness ?: 0) + netCounters
            }
        }
    }
}
