package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.state.components.battlefield.chosenLandType
import com.wingedsheep.engine.state.components.battlefield.chosenColor
import com.wingedsheep.engine.handlers.ConditionEvaluationContext
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Applies continuous effects and counters to mutable projected values.
 */
internal class EffectApplicator(
    private val dynamicAmountEvaluator: DynamicAmountEvaluator
) {

    // Source conditions are evaluated while the projection is still being built. Hand the
    // ConditionEvaluator a non-reentrant projection (mirroring StateProjector's own evaluator):
    // reaching for the canonical lazy GameState.projectedState here would re-enter our own
    // initializer and recurse until the stack overflows (e.g. a Layer-7 conditional P/T static
    // ability whose source condition counts creatures via a battlefield aggregate). The empty
    // projection falls back to base CardComponent values, exactly as CDA resolution does.
    private val conditionEvaluator = ConditionEvaluator(defaultProjection = { ProjectedState(it, emptyMap()) })

    fun applyEffect(
        effect: ContinuousEffect,
        state: GameState,
        projectedValues: MutableMap<EntityId, MutableProjectedValues>
    ) {
        val sourceCondition = effect.sourceCondition
        if (sourceCondition != null) {
            val sourceValues = projectedValues[effect.sourceId]
            val ctx = ConditionEvaluationContext.Projection(
                sourceId = effect.sourceId,
                sourceValues = sourceValues,
                projectedValues = projectedValues
            )
            if (!conditionEvaluator.evaluate(state, sourceCondition, ctx)) return
        }

        for (entityId in effect.affectedEntities) {
            val values = projectedValues.getOrPut(entityId) { MutableProjectedValues() }

            when (val mod = effect.modification) {
                is Modification.SetPowerToughness -> {
                    values.power = mod.power
                    values.toughness = mod.toughness
                }
                is Modification.SetPowerToughnessDynamic -> {
                    // CDA (CR 604.3): the dynamic value *is* the base P/T, set at Layer 7b.
                    val controllerId = projectedValues[effect.sourceId]?.controllerId
                        ?: state.getEntity(effect.sourceId)?.get<ControllerComponent>()?.playerId
                    if (controllerId != null && "CREATURE" in values.types) {
                        val context = EffectContext(
                            sourceId = effect.sourceId,
                            controllerId = controllerId,
                            affectedEntityId = entityId
                        )
                        val intermediateProjected = buildIntermediateProjectedState(state, projectedValues)
                        values.power = dynamicAmountEvaluator.evaluate(state, mod.power, context, intermediateProjected)
                        values.toughness = dynamicAmountEvaluator.evaluate(state, mod.toughness, context, intermediateProjected)
                    }
                }
                is Modification.SetPower -> {
                    values.power = mod.power
                }
                is Modification.SetToughness -> {
                    values.toughness = mod.toughness
                }
                is Modification.ModifyPowerToughness -> {
                    // CR 208.3: noncreature permanents have no power/toughness, even if printed
                    // values appear on them (e.g., a Vehicle that isn't currently a creature).
                    // P/T modifications apply only while the affected permanent is a creature.
                    if ("CREATURE" in values.types) {
                        values.power = (values.power ?: 0) + mod.powerMod
                        values.toughness = (values.toughness ?: 0) + mod.toughnessMod
                    }
                }
                is Modification.SwitchPowerToughness -> {
                    val p = values.power
                    val t = values.toughness
                    values.power = t
                    values.toughness = p
                }
                is Modification.GrantKeyword -> {
                    values.keywords.add(mod.keyword)
                    // Changeling grants all creature types (Rule 702.73)
                    if (mod.keyword == Keyword.CHANGELING.name) {
                        values.subtypes.addAll(com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES)
                    }
                }
                is Modification.RemoveKeyword -> {
                    values.keywords.remove(mod.keyword)
                }
                is Modification.GrantLandwalkFromChosen -> {
                    val chosenLandType = state.getEntity(effect.sourceId)
                        ?.chosenLandType()
                    val landwalk = when (chosenLandType) {
                        com.wingedsheep.sdk.core.Subtype.PLAINS.value -> Keyword.PLAINSWALK
                        com.wingedsheep.sdk.core.Subtype.ISLAND.value -> Keyword.ISLANDWALK
                        com.wingedsheep.sdk.core.Subtype.SWAMP.value -> Keyword.SWAMPWALK
                        com.wingedsheep.sdk.core.Subtype.MOUNTAIN.value -> Keyword.MOUNTAINWALK
                        com.wingedsheep.sdk.core.Subtype.FOREST.value -> Keyword.FORESTWALK
                        else -> null
                    }
                    if (landwalk != null) {
                        values.keywords.add(landwalk.name)
                    }
                }
                is Modification.ChangeColor -> {
                    values.colors.clear()
                    values.colors.addAll(mod.colors)
                }
                is Modification.AddColor -> {
                    values.colors.addAll(mod.colors)
                }
                is Modification.AddChosenColor -> {
                    val chosenColor = state.getEntity(effect.sourceId)
                        ?.chosenColor()
                    if (chosenColor != null) {
                        values.colors.add(chosenColor.name)
                    }
                }
                is Modification.AddType -> {
                    values.types.add(mod.type)
                }
                is Modification.AddSubtype -> {
                    values.types.add(mod.subtype)
                    values.subtypes.add(mod.subtype)
                }
                is Modification.AddAllCreatureTypes -> {
                    values.subtypes.addAll(com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES)
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
                is Modification.SetBasicLandTypesFromChosen -> {
                    val chosenLandType = state.getEntity(effect.sourceId)
                        ?.chosenLandType()
                    if (chosenLandType != null) {
                        val basicLandTypes = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES
                        values.subtypes.removeAll { it in basicLandTypes }
                        values.types.removeAll { it in basicLandTypes }
                        values.subtypes.add(chosenLandType)
                        values.types.add(chosenLandType)
                    }
                }
                is Modification.SetCardTypes -> {
                    val cardTypeStrings = setOf("CREATURE", "ARTIFACT", "ENCHANTMENT", "LAND", "PLANESWALKER", "INSTANT", "SORCERY", "BATTLE", "KINDRED")
                    values.types.removeAll { it in cardTypeStrings }
                    values.types.addAll(mod.types)
                }
                is Modification.SetAllSubtypes -> {
                    values.subtypes.clear()
                    values.subtypes.addAll(mod.subtypes)
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
                        ?.chosenColor()
                    if (chosenColor != null) {
                        values.keywords.add("PROTECTION_FROM_${chosenColor.name}")
                    }
                }
                is Modification.GrantHexproofFromOwnColors -> {
                    for (colorName in values.colors) {
                        values.keywords.add("HEXPROOF_FROM_$colorName")
                    }
                }
                is Modification.GrantHexproofFromMonocolored -> {
                    values.keywords.add("HEXPROOF_FROM_MONOCOLORED")
                }
                is Modification.GrantProtectionFromControlledColors -> {
                    // Protection from the colors of permanents the source's controller controls
                    // (e.g., Pledge of Loyalty). Read the projected controller + projected colors
                    // so color-changing effects (Layer 5) are reflected; colorless permanents add
                    // no color.
                    val sourceController = projectedValues[effect.sourceId]?.controllerId
                        ?: state.getEntity(effect.sourceId)?.get<ControllerComponent>()?.playerId
                    if (sourceController != null) {
                        for (other in projectedValues.values) {
                            if (other.controllerId != sourceController) continue
                            for (colorName in other.colors) {
                                values.keywords.add("PROTECTION_FROM_$colorName")
                            }
                        }
                    }
                }
                is Modification.SetCantAttack -> {
                    values.cantAttack = true
                }
                is Modification.SetSuspected -> {
                    values.isSuspected = true
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
                is Modification.CantBeBlockedExceptBy -> {
                    values.cantBeBlockedExceptByFilters.add(mod.blockerFilter)
                }
                is Modification.CanOnlyBlockCreaturesWith -> {
                    values.canOnlyBlockCreaturesWithFilters.add(mod.blockerFilter)
                }
                is Modification.ModifyPowerToughnessDynamic -> {
                    val controllerId = projectedValues[effect.sourceId]?.controllerId
                        ?: state.getEntity(effect.sourceId)?.get<ControllerComponent>()?.playerId
                    // See ModifyPowerToughness above: P/T mods apply only to creatures.
                    if (controllerId != null && "CREATURE" in values.types) {
                        val context = EffectContext(
                            sourceId = effect.sourceId,
                            controllerId = controllerId,
                            affectedEntityId = entityId
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
