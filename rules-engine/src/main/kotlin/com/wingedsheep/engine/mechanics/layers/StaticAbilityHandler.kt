package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.battlefield.GrantCantBeBlockedToSmallCreaturesComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerHexproofComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsStationUsingToughnessComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.battlefield.SuppressesHexproofForGroupComponent
import com.wingedsheep.engine.state.components.battlefield.SuppressesWardForGroupComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.ReplaceDamageWithCounters
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.AddCreatureTypeByCounter
import com.wingedsheep.sdk.scripting.AddLandTypeByCounter
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.MustBlock
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.SetEnchantedLandType
import com.wingedsheep.sdk.scripting.SetEnchantedLandTypeFromChosen
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.IsAllCreatureTypes
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.GrantSupertype
import com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionFromControlledColors
import com.wingedsheep.sdk.scripting.GrantHexproofFromOwnColorsToGroup
import com.wingedsheep.sdk.scripting.GrantHexproofFromMonocoloredToGroup
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.GrantColor
import com.wingedsheep.sdk.scripting.GrantChosenColor
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.SetBaseToughnessForCreatureGroup
import com.wingedsheep.sdk.scripting.CantBeTargetedByOpponentAbilities
import com.wingedsheep.sdk.scripting.CantBeSacrificed
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.GrantShroudToController
import com.wingedsheep.sdk.scripting.StationUsingToughness
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureIsLegendary
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.conditions.IsYourTurn
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantLandwalkOfChosenType
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.GrantCantBeBlockedToSmallCreatures
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * Converts static abilities from card definitions into ContinuousEffectSourceComponent.
 *
 * When a permanent enters the battlefield, this handler checks if it has any static
 * abilities that should generate continuous effects (like "Other creatures you control
 * have flying" or "Other tapped creatures you control have indestructible").
 *
 * These effects are converted to ContinuousEffectData which the StateProjector then
 * uses to calculate the projected game state.
 */
class StaticAbilityHandler(
    private val cardRegistry: CardRegistry
) {

    /**
     * Creates a ContinuousEffectSourceComponent for a permanent if it has any
     * static abilities that should generate continuous effects.
     *
     * @param container The entity's component container
     * @return The updated container with ContinuousEffectSourceComponent, or original if no static abilities
     */
    fun addContinuousEffectComponent(container: ComponentContainer): ComponentContainer {
        val cardComponent = container.get<CardComponent>() ?: return container

        // Get the card definition to access static abilities
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return container

        return addContinuousEffectComponent(container, cardDef)
    }

    /**
     * Creates a ContinuousEffectSourceComponent for a permanent using a CardDefinition directly.
     *
     * @param container The entity's component container
     * @param cardDefinition The card definition with static abilities
     * @return The updated container with ContinuousEffectSourceComponent, or original if no static abilities
     */
    fun addContinuousEffectComponent(
        container: ComponentContainer,
        cardDefinition: CardDefinition
    ): ComponentContainer {
        var result = container

        // Use effective static abilities which includes class level abilities
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val allStaticAbilities = cardDefinition.script.effectiveStaticAbilities(classLevel)

        // Convert static abilities to continuous effect data
        val effectsData = allStaticAbilities.flatMap { ability ->
            convertStaticAbilities(ability)
        }

        if (effectsData.isNotEmpty()) {
            result = result.with(ContinuousEffectSourceComponent(effectsData))
        }

        // Add tag component for abilities that grant controller-level effects
        if (allStaticAbilities.any { it is GrantShroudToController }) {
            result = result.with(GrantsControllerShroudComponent)
        }
        if (allStaticAbilities.any { it is GrantHexproofToController }) {
            result = result.with(GrantsControllerHexproofComponent)
        }

        // Add tag component for "you can't lose the game"
        if (allStaticAbilities.any { it is com.wingedsheep.sdk.scripting.GrantCantLoseGame }) {
            result = result.with(GrantsCantLoseGameComponent)
        }

        // Add tag component for "station using toughness"
        if (allStaticAbilities.any { it is StationUsingToughness }) {
            result = result.with(GrantsStationUsingToughnessComponent)
        }

        // Add tag component for "can't be the target of abilities your opponents control"
        if (allStaticAbilities.any { it is CantBeTargetedByOpponentAbilities }) {
            result = result.with(CantBeTargetedByOpponentAbilitiesComponent)
        }

        // Add component for "creatures you control with power or toughness N or less can't be blocked"
        val smallCreaturesAbility = allStaticAbilities
            .filterIsInstance<GrantCantBeBlockedToSmallCreatures>()
            .firstOrNull()
        if (smallCreaturesAbility != null) {
            result = result.with(GrantCantBeBlockedToSmallCreaturesComponent(smallCreaturesAbility.maxValue))
        }

        // Add component for "creatures matching filter can be targeted as though they didn't have hexproof"
        val suppressHexproofFilters = allStaticAbilities
            .filterIsInstance<com.wingedsheep.sdk.scripting.SuppressHexproofForGroup>()
            .map { it.filter }
        if (suppressHexproofFilters.isNotEmpty()) {
            result = result.with(SuppressesHexproofForGroupComponent(suppressHexproofFilters))
        }

        // Add component for "ward abilities of creatures matching filter don't trigger"
        val suppressWardFilters = allStaticAbilities
            .filterIsInstance<com.wingedsheep.sdk.scripting.SuppressWardForGroup>()
            .map { it.filter }
        if (suppressWardFilters.isNotEmpty()) {
            result = result.with(SuppressesWardForGroupComponent(suppressWardFilters))
        }

        return result
    }

    /**
     * Add continuous effect component from a list of static abilities directly.
     * Used for tokens with static abilities that don't have a CardDefinition.
     */
    fun addContinuousEffectComponentFromAbilities(
        container: ComponentContainer,
        staticAbilities: List<StaticAbility>
    ): ComponentContainer {
        val effectsData = staticAbilities.flatMap { ability ->
            convertStaticAbilities(ability)
        }
        return if (effectsData.isNotEmpty()) container.with(ContinuousEffectSourceComponent(effectsData))
        else container
    }

    /**
     * Convert a static ability to a list of ContinuousEffectData.
     * Most abilities produce a single effect, but some (like AnimateLandGroup) produce multiple.
     */
    private fun convertStaticAbilities(ability: StaticAbility): List<ContinuousEffectData> {
        return when (ability) {
            is AnimateLandGroup -> convertAnimateLandGroup(ability)
            is GrantAdditionalTypesToGroup -> convertGrantAdditionalTypesToGroup(ability)
            is TransformPermanent -> convertTransformPermanent(ability)
            else -> listOfNotNull(convertStaticAbility(ability))
        }
    }

    /**
     * Convert AnimateLandGroup to multiple continuous effects across layers.
     * "Forests you control are 1/1 green Elf creatures that are still lands."
     */
    private fun convertAnimateLandGroup(ability: AnimateLandGroup): List<ContinuousEffectData> {
        val filter = convertGroupFilter(ability.filter)
        val effects = mutableListOf<ContinuousEffectData>()

        // Layer 4 (TYPE): Add "Creature" type
        effects.add(ContinuousEffectData(
            modification = Modification.AddType("CREATURE"),
            affectsFilter = filter
        ))

        // Layer 4 (TYPE): Add creature subtypes (e.g., "Elf")
        for (subtype in ability.creatureSubtypes) {
            effects.add(ContinuousEffectData(
                modification = Modification.AddSubtype(subtype),
                affectsFilter = filter
            ))
        }

        // Layer 5 (COLOR): Add colors (e.g., GREEN)
        if (ability.colors.isNotEmpty()) {
            effects.add(ContinuousEffectData(
                modification = Modification.AddColor(ability.colors.map { it.name }.toSet()),
                affectsFilter = filter
            ))
        }

        // Layer 7b (POWER_TOUGHNESS, SET_VALUES): Set P/T
        effects.add(ContinuousEffectData(
            modification = Modification.SetPowerToughness(ability.power, ability.toughness),
            affectsFilter = filter
        ))

        return effects
    }

    /**
     * Convert GrantAdditionalTypesToGroup to multiple Layer 4 continuous effects.
     * "Other creatures are Food artifacts in addition to their other types."
     */
    private fun convertGrantAdditionalTypesToGroup(ability: GrantAdditionalTypesToGroup): List<ContinuousEffectData> {
        val filter = convertGroupFilter(ability.filter)
        val effects = mutableListOf<ContinuousEffectData>()

        for (cardType in ability.addCardTypes) {
            effects.add(ContinuousEffectData(
                modification = Modification.AddType(cardType),
                affectsFilter = filter
            ))
        }

        for (subtype in ability.addSubtypes) {
            effects.add(ContinuousEffectData(
                modification = Modification.AddSubtype(subtype),
                affectsFilter = filter
            ))
        }

        return effects
    }

    /**
     * Convert TransformPermanent to multiple continuous effects across layers.
     * "Enchanted permanent is a colorless Food artifact..."
     */
    private fun convertTransformPermanent(ability: TransformPermanent): List<ContinuousEffectData> {
        val filter = convertGroupFilter(ability.filter)
        val effects = mutableListOf<ContinuousEffectData>()

        // Layer 4 (TYPE): Set card types (replaces all existing)
        if (ability.setCardTypes.isNotEmpty()) {
            effects.add(ContinuousEffectData(
                modification = Modification.SetCardTypes(ability.setCardTypes),
                affectsFilter = filter
            ))
        }

        // Layer 4 (TYPE): Set subtypes (replaces all existing)
        if (ability.setSubtypes.isNotEmpty()) {
            effects.add(ContinuousEffectData(
                modification = Modification.SetAllSubtypes(ability.setSubtypes),
                affectsFilter = filter
            ))
        }

        // Layer 5 (COLOR): Set colors (empty set = colorless)
        val colors = ability.setColors
        if (colors != null) {
            effects.add(ContinuousEffectData(
                modification = Modification.ChangeColor(colors.map { it.name }.toSet()),
                affectsFilter = filter
            ))
        }

        return effects
    }

    /**
     * Convert a static ability to ContinuousEffectData.
     *
     * @param ability The static ability to convert
     * @return The continuous effect data, or null if this ability type isn't supported yet
     */
    private fun convertStaticAbility(ability: StaticAbility): ContinuousEffectData? {
        return when (ability) {
            is GrantKeyword -> {
                ContinuousEffectData(
                    modification = Modification.GrantKeyword(ability.keyword),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantWard -> {
                // Grant the WARD keyword for display; the triggered ability is generated
                // by TriggerAbilityResolver.getWardTriggeredAbilities()
                ContinuousEffectData(
                    modification = Modification.GrantKeyword("WARD"),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantProtectionFromChosenColorToGroup -> {
                ContinuousEffectData(
                    modification = Modification.GrantProtectionFromChosenColor,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantHexproofFromOwnColorsToGroup -> {
                ContinuousEffectData(
                    modification = Modification.GrantHexproofFromOwnColors,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantHexproofFromMonocoloredToGroup -> {
                ContinuousEffectData(
                    modification = Modification.GrantHexproofFromMonocolored,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantProtectionFromControlledColors -> {
                ContinuousEffectData(
                    modification = Modification.GrantProtectionFromControlledColors,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is ModifyStats -> {
                ContinuousEffectData(
                    modification = Modification.ModifyPowerToughness(ability.powerBonus, ability.toughnessBonus),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantDynamicStatsEffect -> {
                ContinuousEffectData(
                    modification = Modification.ModifyPowerToughnessDynamic(ability.powerBonus, ability.toughnessBonus),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is RemoveKeywordStatic -> {
                ContinuousEffectData(
                    modification = Modification.RemoveKeyword(ability.keyword),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is CantBeBlocked -> {
                ContinuousEffectData(
                    modification = Modification.GrantKeyword(com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_BLOCKED.name),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is CantAttack -> {
                ContinuousEffectData(
                    modification = Modification.SetCantAttack,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is CantBlock -> {
                ContinuousEffectData(
                    modification = Modification.SetCantBlock,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is MustBlock -> {
                ContinuousEffectData(
                    modification = Modification.SetMustBlock,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is CanBlockAdditionalForCreatureGroup -> {
                ContinuousEffectData(
                    modification = Modification.CanBlockAdditional(ability.count),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is MustAttack -> {
                ContinuousEffectData(
                    modification = Modification.SetMustAttack,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is ControlEnchantedPermanent -> {
                // "You control enchanted permanent" - Layer 2 control-changing effect
                // The actual newControllerId is resolved dynamically by the StateProjector
                // using a placeholder; the Aura's controller is used at projection time
                ContinuousEffectData(
                    modification = Modification.ChangeControllerToSourceController,
                    affectsFilter = AffectsFilter.AttachedPermanent
                )
            }
            is SetEnchantedLandType -> {
                // "Enchanted land is an [type]" - Layer 4 type-changing effect
                // Replaces all basic land subtypes with the specified type (Rule 305.7)
                ContinuousEffectData(
                    modification = Modification.SetBasicLandTypes(setOf(ability.landType)),
                    affectsFilter = AffectsFilter.AttachedPermanent
                )
            }
            is SetEnchantedLandTypeFromChosen -> {
                // "Enchanted land is the chosen type" - Layer 4 type-changing effect.
                // The chosen type is resolved at projection time from the source's
                // ChosenLandTypeComponent; replaces all basic land subtypes (Rule 305.7).
                ContinuousEffectData(
                    modification = Modification.SetBasicLandTypesFromChosen,
                    affectsFilter = AffectsFilter.AttachedPermanent
                )
            }
            is GrantLandwalkOfChosenType -> {
                // "Enchanted creature has landwalk of the chosen type" - Layer 6 ability-adding.
                // The landwalk keyword is resolved at projection time from the source's
                // ChosenLandTypeComponent (Plains→Plainswalk, Island→Islandwalk, etc.).
                ContinuousEffectData(
                    modification = Modification.GrantLandwalkFromChosen,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantKeywordByCounter -> {
                ContinuousEffectData(
                    modification = Modification.GrantKeyword(ability.keyword.name),
                    affectsFilter = if (ability.controllerOnly)
                        AffectsFilter.OwnCreaturesWithCounter(ability.counterType)
                    else
                        AffectsFilter.CreaturesWithCounter(ability.counterType)
                )
            }
            is AddCreatureTypeByCounter -> {
                ContinuousEffectData(
                    modification = Modification.AddSubtype(ability.creatureType),
                    affectsFilter = AffectsFilter.CreaturesWithCounter(ability.counterType)
                )
            }
            is AddLandTypeByCounter -> {
                ContinuousEffectData(
                    modification = Modification.AddSubtype(ability.landType),
                    affectsFilter = AffectsFilter.LandsWithCounter(ability.counterType)
                )
            }
            is GrantSubtype -> {
                ContinuousEffectData(
                    modification = Modification.AddSubtype(ability.subtype),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is IsAllCreatureTypes -> {
                ContinuousEffectData(
                    modification = Modification.AddAllCreatureTypes,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantSupertype -> {
                ContinuousEffectData(
                    modification = Modification.AddType(ability.supertype),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantCardType -> {
                ContinuousEffectData(
                    modification = Modification.AddType(ability.cardType.uppercase()),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is RemoveCardType -> {
                ContinuousEffectData(
                    modification = Modification.RemoveType(ability.cardType.uppercase()),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantProtection -> {
                ContinuousEffectData(
                    modification = Modification.GrantProtectionFromColor(ability.color.name),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is CantReceiveCounters -> {
                ContinuousEffectData(
                    modification = Modification.GrantKeyword(com.wingedsheep.sdk.core.AbilityFlag.CANT_RECEIVE_COUNTERS.name),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is CantBeSacrificed -> {
                ContinuousEffectData(
                    modification = Modification.GrantKeyword(com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_SACRIFICED.name),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy -> {
                ContinuousEffectData(
                    modification = Modification.CantBeBlockedExceptBy(ability.blockerFilter),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith -> {
                ContinuousEffectData(
                    modification = Modification.CanOnlyBlockCreaturesWith(ability.blockerFilter),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantColor -> {
                ContinuousEffectData(
                    modification = Modification.AddColor(setOf(ability.color.name)),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is GrantChosenColor -> {
                ContinuousEffectData(
                    modification = Modification.AddChosenColor,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is SetBasePowerToughnessStatic -> {
                ContinuousEffectData(
                    modification = Modification.SetPowerToughness(ability.power, ability.toughness),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is SetBaseToughnessForCreatureGroup -> {
                ContinuousEffectData(
                    modification = Modification.SetToughness(ability.toughness),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is LoseAllAbilities -> {
                ContinuousEffectData(
                    modification = Modification.RemoveAllAbilities,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is ConditionalStaticAbility -> convertConditionalStaticAbility(ability)
            else -> null
        }
    }

    /**
     * Convert a ConditionalStaticAbility to ContinuousEffectData. The SDK [Condition] is
     * passed through unchanged; [ConditionEvaluator] evaluates it under
     * [ConditionEvaluationContext.Projection] during layer application.
     */
    private fun convertConditionalStaticAbility(conditional: ConditionalStaticAbility): ContinuousEffectData? {
        val baseEffect = convertStaticAbility(conditional.ability) ?: return null
        return baseEffect.copy(sourceCondition = conditional.condition)
    }

    /**
     * Creates a ReplacementEffectSourceComponent for a permanent if it has any
     * runtime-relevant replacement effects (e.g., PreventDamage).
     * Zone-change replacement effects like EntersTapped are handled elsewhere.
     */
    fun addReplacementEffectComponent(container: ComponentContainer): ComponentContainer {
        val cardComponent = container.get<CardComponent>() ?: return container
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return container
        return addReplacementEffectComponent(container, cardDef)
    }

    /**
     * Creates a ReplacementEffectSourceComponent using a CardDefinition directly.
     */
    fun addReplacementEffectComponent(
        container: ComponentContainer,
        cardDefinition: CardDefinition
    ): ComponentContainer {
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val runtimeEffects = collectRuntimeReplacementEffects(cardDefinition, classLevel)
        if (runtimeEffects.isEmpty()) return container
        return container.with(ReplacementEffectSourceComponent(runtimeEffects))
    }

    private fun collectRuntimeReplacementEffects(
        cardDefinition: CardDefinition,
        classLevel: Int?
    ): List<com.wingedsheep.sdk.scripting.ReplacementEffect> {
        val effects = cardDefinition.script.replacementEffects.filter { isRuntimeReplacementEffect(it) }.toMutableList()
        if (classLevel != null) {
            for (level in cardDefinition.script.classLevels) {
                if (level.level <= classLevel) {
                    effects.addAll(level.replacementEffects.filter { isRuntimeReplacementEffect(it) })
                }
            }
        }
        return effects
    }

    private fun isRuntimeReplacementEffect(it: com.wingedsheep.sdk.scripting.ReplacementEffect): Boolean =
        it is PreventDamage || it is DoubleDamage || it is ModifyDamageAmount || it is PreventLifeGain ||
        it is com.wingedsheep.sdk.scripting.CapDamage || it is com.wingedsheep.sdk.scripting.RedirectDamage ||
        it is com.wingedsheep.sdk.scripting.ModifyLifeLoss ||
        it is com.wingedsheep.sdk.scripting.ModifyLifeGain ||
        it is com.wingedsheep.sdk.scripting.PreventDraw ||
        it is com.wingedsheep.sdk.scripting.DamageCantBePrevented || it is ReplaceDamageWithCounters ||
        it is com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect || it is com.wingedsheep.sdk.scripting.ModifyDrawAmount ||
        it is com.wingedsheep.sdk.scripting.ModifyCounterPlacement ||
        it is com.wingedsheep.sdk.scripting.RedirectZoneChange || it is com.wingedsheep.sdk.scripting.PreventExtraTurns ||
        it is com.wingedsheep.sdk.scripting.RedirectZoneChangeWithEffect || it is com.wingedsheep.sdk.scripting.EntersWithCounters ||
        it is com.wingedsheep.sdk.scripting.EntersWithDynamicCounters || it is com.wingedsheep.sdk.scripting.DoubleCounterPlacement ||
        it is com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithEquippedCopy ||
        it is com.wingedsheep.sdk.scripting.DoubleTokenCreation || it is com.wingedsheep.sdk.scripting.ModifyTokenCount

    /**
     * Convert a GroupFilter to an AffectsFilter.
     */
    private fun convertGroupFilter(filter: GroupFilter): AffectsFilter {
        // Scope short-circuits — non-Battlefield filters refer to specific entities
        // relative to the source rather than scanning the battlefield.
        when (val scope = filter.scope) {
            is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self -> return AffectsFilter.Self
            is com.wingedsheep.sdk.scripting.filters.unified.Scope.AttachedTo -> return AffectsFilter.AttachedPermanent
            is com.wingedsheep.sdk.scripting.filters.unified.Scope.Specific ->
                return AffectsFilter.SpecificEntities(setOf(scope.entityId))
            is com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield -> { /* fall through */ }
        }

        val baseFilter = filter.baseFilter
        val controllerPredicate = baseFilter.controllerPredicate
        val hasExcludeSelf = filter.excludeSelf
        val hasTappedPredicate = baseFilter.statePredicates.any { it == StatePredicate.IsTapped }
        val hasFaceDownPredicate = baseFilter.statePredicates.any { it == StatePredicate.IsFaceDown }
        val subtypePredicate = baseFilter.cardPredicates.filterIsInstance<CardPredicate.HasSubtype>().firstOrNull()
        val hasAnyOfSubtypes = baseFilter.cardPredicates.any { it is CardPredicate.HasAnyOfSubtypes }

        // Check if the card predicates are creature-compatible (empty, IsCreature only, or IsCreature + subtype).
        // Non-creature type predicates (e.g., IsPlaneswalker) must fall through to Generic.
        val hasNonCreatureTypePredicate = baseFilter.cardPredicates.any {
            it != CardPredicate.IsCreature && it !is CardPredicate.HasSubtype && it !is CardPredicate.HasAnyOfSubtypes
        }
        if (hasNonCreatureTypePredicate) {
            return AffectsFilter.Generic(filter)
        }

        // Chosen subtype filters must go through Generic so resolveGenericFilter can
        // read ChosenCreatureTypeComponent from the source entity
        if (filter.chosenSubtypeKey != null) {
            return AffectsFilter.Generic(filter)
        }

        // Handle "face-down creatures" pattern (e.g., "Face-down creatures get +1/+1")
        // Only use the simple filter when there's no controller restriction;
        // with a controller predicate (e.g., "you control"), fall through to Generic.
        if (hasFaceDownPredicate && controllerPredicate == null) {
            return AffectsFilter.FaceDownCreatures
        }

        // Handle "other [subtype] creatures" pattern (e.g., "Other Bird creatures get +1/+1")
        // Only use the simple filter when there's no controller restriction;
        // with a controller predicate (e.g., "you control"), fall through to Generic.
        if (hasExcludeSelf && subtypePredicate != null && controllerPredicate == null) {
            return AffectsFilter.OtherCreaturesWithSubtype(subtypePredicate.subtype.value)
        }

        // Handle "other tapped creatures you control" pattern
        if (hasExcludeSelf && hasTappedPredicate && controllerPredicate == ControllerPredicate.ControlledByYou) {
            return AffectsFilter.OtherTappedCreaturesYouControl
        }

        // Handle "other creatures you control" pattern
        if (hasExcludeSelf && subtypePredicate == null && !hasAnyOfSubtypes && controllerPredicate == ControllerPredicate.ControlledByYou && baseFilter.statePredicates.isEmpty()) {
            return AffectsFilter.OtherCreaturesYouControl
        }

        // Handle "other creatures" pattern (only when no other restrictions)
        if (hasExcludeSelf && subtypePredicate == null && !hasAnyOfSubtypes && controllerPredicate == null && baseFilter.statePredicates.isEmpty()) {
            return AffectsFilter.AllOtherCreatures
        }

        // Handle "[subtype] creatures" pattern without controller restriction
        if (subtypePredicate != null && controllerPredicate == null) {
            return AffectsFilter.WithSubtype(subtypePredicate.subtype.value)
        }

        // Handle controller-only filters (no subtype or other complex predicates)
        if (subtypePredicate == null && !hasAnyOfSubtypes && baseFilter.statePredicates.isEmpty()) {
            return when (controllerPredicate) {
                ControllerPredicate.ControlledByYou -> AffectsFilter.AllCreaturesYouControl
                ControllerPredicate.ControlledByOpponent -> AffectsFilter.AllCreaturesOpponentsControl
                else -> AffectsFilter.AllCreatures
            }
        }

        // Fallback: use Generic filter to preserve full predicate evaluation
        return AffectsFilter.Generic(filter)
    }
}
