package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.ability.DynamicAmount
import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.ComponentContainer
import com.wingedsheep.rulesengine.ecs.components.*

/**
 * Extension functions for applying Modifications to ComponentContainer.
 *
 * These functions transform a ComponentContainer by applying a single modification,
 * returning a new container with updated projected components. This is the core
 * of the component-based projection system.
 *
 * Each modification type knows how to update the relevant projected component,
 * reading from base components if no projected component exists yet.
 */

/**
 * Apply this modification to a ComponentContainer, returning a new container
 * with updated projected components.
 *
 * @param container The current component container
 * @param context Context for evaluating dynamic values (CDAs, etc.)
 * @return A new container with the modification applied
 */
fun Modification.apply(container: ComponentContainer, context: ProjectionContext): ComponentContainer {
    return when (this) {
        // =========================================================================
        // Layer 2: Control-changing
        // =========================================================================
        is Modification.ChangeControl -> {
            container.with(ProjectedControlComponent(newControllerId))
        }

        // =========================================================================
        // Layer 4: Type-changing
        // =========================================================================
        is Modification.AddType -> {
            val current = container.get<ProjectedTypesComponent>()
                ?: container.get<BaseTypesComponent>()?.toProjected()
                ?: ProjectedTypesComponent(emptySet(), emptySet())
            container.with(current.copy(types = current.types + type))
        }

        is Modification.RemoveType -> {
            val current = container.get<ProjectedTypesComponent>()
                ?: container.get<BaseTypesComponent>()?.toProjected()
                ?: ProjectedTypesComponent(emptySet(), emptySet())
            container.with(current.copy(types = current.types - type))
        }

        is Modification.AddSubtype -> {
            val current = container.get<ProjectedTypesComponent>()
                ?: container.get<BaseTypesComponent>()?.toProjected()
                ?: ProjectedTypesComponent(emptySet(), emptySet())
            container.with(current.copy(subtypes = current.subtypes + subtype))
        }

        is Modification.RemoveSubtype -> {
            val current = container.get<ProjectedTypesComponent>()
                ?: container.get<BaseTypesComponent>()?.toProjected()
                ?: ProjectedTypesComponent(emptySet(), emptySet())
            val abilities = container.get<ProjectedAbilitiesComponent>()
                ?: container.get<BaseKeywordsComponent>()?.toProjected()
            // Rule 702.73a: Changeling grants all creature types, so removal doesn't apply
            if (abilities?.keywords?.contains(Keyword.CHANGELING) == true &&
                current.types.contains(CardType.CREATURE)) {
                container
            } else {
                container.with(current.copy(subtypes = current.subtypes - subtype))
            }
        }

        is Modification.SetSubtypes -> {
            val current = container.get<ProjectedTypesComponent>()
                ?: container.get<BaseTypesComponent>()?.toProjected()
                ?: ProjectedTypesComponent(emptySet(), emptySet())
            val abilities = container.get<ProjectedAbilitiesComponent>()
                ?: container.get<BaseKeywordsComponent>()?.toProjected()
            // If it has Changeling, setting subtypes doesn't wipe "all creature types"
            if (abilities?.keywords?.contains(Keyword.CHANGELING) == true) {
                container.with(current.copy(subtypes = current.subtypes + subtypes))
            } else {
                container.with(current.copy(subtypes = subtypes))
            }
        }

        // =========================================================================
        // Layer 5: Color-changing
        // =========================================================================
        is Modification.AddColor -> {
            val current = container.get<ProjectedColorsComponent>()
                ?: container.get<BaseColorsComponent>()?.toProjected()
                ?: ProjectedColorsComponent(emptySet())
            container.with(current.copy(colors = current.colors + color))
        }

        is Modification.RemoveColor -> {
            val current = container.get<ProjectedColorsComponent>()
                ?: container.get<BaseColorsComponent>()?.toProjected()
                ?: ProjectedColorsComponent(emptySet())
            container.with(current.copy(colors = current.colors - color))
        }

        is Modification.SetColors -> {
            container.with(ProjectedColorsComponent(colors))
        }

        // =========================================================================
        // Layer 6: Ability-adding/removing
        // =========================================================================
        is Modification.AddKeyword -> {
            val current = container.get<ProjectedAbilitiesComponent>()
                ?: container.get<BaseKeywordsComponent>()?.toProjected()
                ?: ProjectedAbilitiesComponent(emptySet())
            container.with(current.copy(keywords = current.keywords + keyword))
        }

        is Modification.RemoveKeyword -> {
            val current = container.get<ProjectedAbilitiesComponent>()
                ?: container.get<BaseKeywordsComponent>()?.toProjected()
                ?: ProjectedAbilitiesComponent(emptySet())
            container.with(current.copy(keywords = current.keywords - keyword))
        }

        is Modification.RemoveAllAbilities -> {
            container.with(ProjectedAbilitiesComponent(
                keywords = emptySet(),
                hasAbilities = false,
                cantBlock = false,
                assignsDamageEqualToToughness = false
            ))
        }

        is Modification.AddCantBlockRestriction -> {
            val current = container.get<ProjectedAbilitiesComponent>()
                ?: container.get<BaseKeywordsComponent>()?.toProjected()
                ?: ProjectedAbilitiesComponent(emptySet())
            container.with(current.copy(cantBlock = true))
        }

        is Modification.AssignDamageEqualToToughness -> {
            val current = container.get<ProjectedAbilitiesComponent>()
                ?: container.get<BaseKeywordsComponent>()?.toProjected()
                ?: ProjectedAbilitiesComponent(emptySet())
            // For conditional variant, check if toughness > power
            if (onlyWhenToughnessGreaterThanPower) {
                val ptComponent = container.get<ProjectedPTComponent>()
                    ?: container.get<BaseStatsComponent>()?.toProjected()
                val power = ptComponent?.power ?: 0
                val toughness = ptComponent?.toughness ?: 0
                if (toughness > power) {
                    container.with(current.copy(assignsDamageEqualToToughness = true))
                } else {
                    container
                }
            } else {
                container.with(current.copy(assignsDamageEqualToToughness = true))
            }
        }

        // =========================================================================
        // Layer 7a: P/T from CDAs
        // =========================================================================
        is Modification.SetPTFromCDA -> {
            val (power, toughness) = evaluateCDA(cdaType, context)
            container.with(ProjectedPTComponent(power, toughness))
        }

        // =========================================================================
        // Layer 7b: P/T setting
        // =========================================================================
        is Modification.SetPT -> {
            container.with(ProjectedPTComponent(power, toughness))
        }

        is Modification.SetPower -> {
            val current = container.get<ProjectedPTComponent>()
                ?: container.get<BaseStatsComponent>()?.toProjected()
                ?: ProjectedPTComponent(null, null)
            container.with(current.copy(power = power))
        }

        is Modification.SetToughness -> {
            val current = container.get<ProjectedPTComponent>()
                ?: container.get<BaseStatsComponent>()?.toProjected()
                ?: ProjectedPTComponent(null, null)
            container.with(current.copy(toughness = toughness))
        }

        // =========================================================================
        // Layer 7c: P/T modification
        // =========================================================================
        is Modification.ModifyPT -> {
            val current = container.get<ProjectedPTComponent>()
                ?: container.get<BaseStatsComponent>()?.toProjected()
                ?: ProjectedPTComponent(null, null)
            container.with(ProjectedPTComponent(
                power = current.power?.plus(powerDelta),
                toughness = current.toughness?.plus(toughnessDelta)
            ))
        }

        is Modification.ModifyPower -> {
            val current = container.get<ProjectedPTComponent>()
                ?: container.get<BaseStatsComponent>()?.toProjected()
                ?: ProjectedPTComponent(null, null)
            container.with(current.copy(power = current.power?.plus(delta)))
        }

        is Modification.ModifyToughness -> {
            val current = container.get<ProjectedPTComponent>()
                ?: container.get<BaseStatsComponent>()?.toProjected()
                ?: ProjectedPTComponent(null, null)
            container.with(current.copy(toughness = current.toughness?.plus(delta)))
        }

        is Modification.ModifyPTDynamic -> {
            val current = container.get<ProjectedPTComponent>()
                ?: container.get<BaseStatsComponent>()?.toProjected()
                ?: ProjectedPTComponent(null, null)
            val powerBonus = evaluateDynamicAmount(powerSource, context)
            val toughnessBonus = evaluateDynamicAmount(toughnessSource, context)
            container.with(ProjectedPTComponent(
                power = current.power?.plus(powerBonus),
                toughness = current.toughness?.plus(toughnessBonus)
            ))
        }

        // =========================================================================
        // Layer 7e: P/T switching
        // =========================================================================
        is Modification.SwitchPT -> {
            val current = container.get<ProjectedPTComponent>()
                ?: container.get<BaseStatsComponent>()?.toProjected()
                ?: ProjectedPTComponent(null, null)
            container.with(ProjectedPTComponent(
                power = current.toughness,
                toughness = current.power
            ))
        }
    }
}

/**
 * Evaluate a characteristic-defining ability.
 */
private fun evaluateCDA(cdaType: CDAType, context: ProjectionContext): Pair<Int, Int> {
    val state = context.state
    val controllerId = context.controllerId

    return when (cdaType) {
        CDAType.CARDS_IN_GRAVEYARD -> {
            val count = state.getGraveyard(controllerId).size
            count to count
        }
        CDAType.CREATURES_YOU_CONTROL -> {
            val count = state.getCreaturesControlledBy(controllerId).size
            count to count
        }
        CDAType.LANDS_YOU_CONTROL -> {
            val count = state.getPermanentsControlledBy(controllerId).count { id ->
                state.getComponent<CardComponent>(id)?.definition?.isLand == true
            }
            count to count
        }
        CDAType.CARDS_IN_HAND -> {
            val count = state.getHand(controllerId).size
            count to count
        }
        CDAType.DEVOTION -> {
            // Would need to know which color - return 0 for now
            0 to 0
        }
        CDAType.CUSTOM -> {
            // Custom CDAs need script support
            0 to 0
        }
    }
}

/**
 * Evaluate a dynamic amount for effects like "+X/+X where X is..."
 */
private fun evaluateDynamicAmount(amount: DynamicAmount, context: ProjectionContext): Int {
    val state = context.state
    val sourceId = context.sourceId
    val controllerId = context.controllerId

    return when (amount) {
        is DynamicAmount.Fixed -> amount.amount
        is DynamicAmount.OtherCreaturesYouControl -> {
            state.getCreaturesControlledBy(controllerId).count { it != sourceId }
        }
        is DynamicAmount.CreaturesYouControl -> {
            state.getCreaturesControlledBy(controllerId).size
        }
        is DynamicAmount.AllCreatures -> {
            state.getBattlefield().count { entityId ->
                state.getComponent<CardComponent>(entityId)?.definition?.isCreature == true
            }
        }
        is DynamicAmount.YourLifeTotal -> {
            state.getComponent<LifeComponent>(controllerId)?.life ?: 0
        }
        is DynamicAmount.CreaturesEnteredThisTurn -> {
            // TODO: Track creatures that entered this turn
            0
        }
        is DynamicAmount.AttackingCreaturesYouControl -> {
            state.getBattlefield().count { entityId ->
                state.getComponent<AttackingComponent>(entityId) != null &&
                    state.getComponent<ControllerComponent>(entityId)?.controllerId == controllerId
            }
        }
        is DynamicAmount.ColorsAmongPermanentsYouControl -> {
            state.getPermanentsControlledBy(controllerId)
                .mapNotNull { state.getComponent<CardComponent>(it)?.definition?.colors }
                .flatten()
                .toSet()
                .size
        }
        is DynamicAmount.OtherCreaturesWithSubtypeYouControl -> {
            state.getCreaturesControlledBy(controllerId).count { entityId ->
                entityId != sourceId &&
                    state.getComponent<CardComponent>(entityId)?.definition?.typeLine?.subtypes?.contains(amount.subtype) == true
            }
        }
    }
}
