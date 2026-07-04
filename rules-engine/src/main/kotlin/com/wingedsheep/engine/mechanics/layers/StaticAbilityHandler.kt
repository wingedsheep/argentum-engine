package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.battlefield.GrantCantBeBlockedToSmallCreaturesComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameFromLifeComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerHexproofComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerProtectionComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsStationUsingToughnessComponent
import com.wingedsheep.engine.state.components.battlefield.GrantsControllerShroudComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.battlefield.SuppressesHexproofForGroupComponent
import com.wingedsheep.engine.state.components.battlefield.SuppressesWardForGroupComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
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
import com.wingedsheep.sdk.scripting.MustBeBlocked
import com.wingedsheep.sdk.scripting.MustBlock
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.EquipAbilitiesAtInstantSpeed
import com.wingedsheep.sdk.scripting.FreeFirstEquipEachTurn
import com.wingedsheep.sdk.scripting.ReduceEquipCost
import com.wingedsheep.sdk.scripting.SetEnchantedLandType
import com.wingedsheep.sdk.scripting.SetEnchantedLandTypeFromChosen
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.IsAllCreatureTypes
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.GrantSupertype
import com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionFromCardType
import com.wingedsheep.sdk.scripting.GrantProtectionFromControlledColors
import com.wingedsheep.sdk.scripting.GrantHexproofFromOwnColorsToGroup
import com.wingedsheep.sdk.scripting.GrantHexproofFromMonocoloredToGroup
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.GrantAdditionalTypesToGroup
import com.wingedsheep.sdk.scripting.SetLandTypesForGroup
import com.wingedsheep.sdk.scripting.GrantColor
import com.wingedsheep.sdk.scripting.GrantChosenColor
import com.wingedsheep.sdk.scripting.CantBeTurnedFaceUp
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.SetName
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.SetBaseToughnessForCreatureGroup
import com.wingedsheep.engine.handlers.SourceTypeTargeting
import com.wingedsheep.sdk.scripting.CantBeTargetedBySourceTypeAbilities
import com.wingedsheep.sdk.scripting.CantBeTargetedByOpponentAbilities
import com.wingedsheep.sdk.scripting.CantBeSacrificed
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.GrantProtectionToController
import com.wingedsheep.sdk.scripting.GrantShroudToController
import com.wingedsheep.sdk.scripting.StationUsingToughness
import com.wingedsheep.sdk.scripting.AdditionalAttackTriggers
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.BlockTax
import com.wingedsheep.sdk.scripting.AttackerCountLimit
import com.wingedsheep.sdk.scripting.BlockerCountLimit
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBeAttackedWithout
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.CantBeBlockedIfCastSpellType
import com.wingedsheep.sdk.scripting.CantBeBlockedUnlessDefenderSharesCreatureType
import com.wingedsheep.sdk.scripting.CantBlockCreaturesWithGreaterPower
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.CantCastSpellsSharingColorWithLastCast
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.DamagePersistsThroughCleanup
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.DivideCombatDamageFreely
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfLinkedExiledCard
import com.wingedsheep.sdk.scripting.SpendAnyManaTypeForActivatedAbilities
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.GrantAlternativeCastingCost
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.GrantWarpToCardsInHand
import com.wingedsheep.sdk.scripting.GrantMiracleToCardsInHand
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveFlashback
import com.wingedsheep.sdk.scripting.LookAtFaceDownCreatures
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.ModifyPlotCost
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyUnlockCost
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.NoncombatDamageBonus
import com.wingedsheep.sdk.scripting.OpponentsPlayWithHandsRevealed
import com.wingedsheep.sdk.scripting.OverrideEnchantedLandManaColor
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlotFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.PreventCycling
import com.wingedsheep.sdk.scripting.SuppressEntersTriggers
import com.wingedsheep.sdk.scripting.ConvertEmptyingManaToRed
import com.wingedsheep.sdk.scripting.PreventManaPoolEmptying
import com.wingedsheep.sdk.scripting.ReplaceLandManaColor
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn
import com.wingedsheep.sdk.scripting.WinCoinFlips
import com.wingedsheep.sdk.scripting.RevealTopOfLibrary
import com.wingedsheep.sdk.scripting.SuppressHexproofForGroup
import com.wingedsheep.sdk.scripting.SuppressWardForGroup
import com.wingedsheep.sdk.scripting.UntapDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.UntapFilteredDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps
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

        // Effective static abilities, including class-level abilities and — for a Room permanent —
        // the static abilities of every currently-unlocked face (CR 709.5). Routing through
        // RoomFaceStatics is what lets a Room face's continuous statics project once its door is
        // unlocked; the component is re-baked on later unlocks by RoomDoorUnlocker.
        val allStaticAbilities = RoomFaceStatics.activeStaticAbilities(container, cardDefinition)

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
        val controllerProtectionScopes = allStaticAbilities
            .filterIsInstance<GrantProtectionToController>()
            .map { it.scope }
        if (controllerProtectionScopes.isNotEmpty()) {
            result = result.with(GrantsControllerProtectionComponent(controllerProtectionScopes))
        }

        // Add tag component for "you can't lose the game"
        if (allStaticAbilities.any { it is com.wingedsheep.sdk.scripting.GrantCantLoseGame }) {
            result = result.with(GrantsCantLoseGameComponent)
        }

        // Add tag component for the narrow "you don't lose for 0 or less life" (Marina's Grimoire)
        if (allStaticAbilities.any { it is com.wingedsheep.sdk.scripting.GrantCantLoseGameFromLife }) {
            result = result.with(GrantsCantLoseGameFromLifeComponent)
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
            is SetLandTypesForGroup -> convertSetLandTypesForGroup(ability)
            is TransformPermanent -> convertTransformPermanent(ability)
            // A ConditionalStaticAbility wrapping a multi-effect ability (e.g. TransformPermanent)
            // must lower through the plural converter too, then gate every resulting effect on the
            // condition — otherwise the singular path returns null and the whole gated ability is
            // silently dropped (Enduring's "becomes an enchantment" return).
            is ConditionalStaticAbility ->
                convertStaticAbilities(ability.ability).map { it.copy(sourceCondition = ability.condition) }
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
     * Convert SetLandTypesForGroup to two continuous effects, realizing CR 305.7 for a whole
     * group of lands (Blood Moon / Magus of the Moon / Zhao's "nonbasic lands are Mountains").
     * - Layer 4 (TYPE): SetBasicLandTypes replaces the old basic land subtypes with the new ones.
     * - Layer 6 (ABILITY): RemoveAllAbilities strips the lands' printed abilities. The new type's
     *   intrinsic mana ability (Mountain → "{T}: Add {R}") is derived from the projected subtypes
     *   by IntrinsicManaAbilities and survives the ability suppression, so the lands still tap for
     *   the appropriate color.
     */
    private fun convertSetLandTypesForGroup(ability: SetLandTypesForGroup): List<ContinuousEffectData> {
        val filter = convertGroupFilter(ability.filter)
        return listOf(
            ContinuousEffectData(
                modification = Modification.SetBasicLandTypes(ability.landTypes),
                affectsFilter = filter
            ),
            ContinuousEffectData(
                modification = Modification.RemoveAllAbilities,
                affectsFilter = filter
            ),
        )
    }

    /**
     * Convert TransformPermanent to multiple continuous effects across layers.
     * "Enchanted permanent is a colorless Food artifact..."
     */
    private fun convertTransformPermanent(ability: TransformPermanent): List<ContinuousEffectData> {
        val filter = convertGroupFilter(ability.filter)
        val effects = mutableListOf<ContinuousEffectData>()

        // Layer 3 (TEXT): Set name (CR 612.8 — overwrites any existing name)
        val newName = ability.setName
        if (newName != null) {
            effects.add(ContinuousEffectData(
                modification = Modification.SetName(newName),
                affectsFilter = filter
            ))
        }

        // Layer 4 (TYPE): Set card types (replaces all existing)
        if (ability.setCardTypes.isNotEmpty()) {
            effects.add(ContinuousEffectData(
                modification = Modification.SetCardTypes(ability.setCardTypes),
                affectsFilter = filter
            ))
        }

        // Layer 4 (TYPE): Set subtypes (replaces all existing). A non-empty set replaces all
        // subtypes; clearSubtypes replaces them with the empty set ("has no subtypes").
        if (ability.setSubtypes.isNotEmpty() || ability.clearSubtypes) {
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
            is GrantProtectionFromCardType -> {
                // Projected as the keyword PROTECTION_FROM_CARDTYPE_<TYPE>; enforced at targeting
                // by TargetValidator.checkProtectionFromCardType.
                ContinuousEffectData(
                    modification = Modification.GrantKeyword(
                        "PROTECTION_FROM_CARDTYPE_${ability.cardType.name}"
                    ),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is CantBeTargetedBySourceTypeAbilities -> {
                // Projected as the keyword CANT_BE_TARGETED_BY_CARDTYPE_<TYPE>_SOURCE_ABILITIES;
                // enforced at targeting by SourceTypeTargeting.cantBeTargetedBySourceTypeAbility
                // (Artifact Ward). Mirrors the PROTECTION_FROM_CARDTYPE_<TYPE> keyword idiom.
                ContinuousEffectData(
                    modification = Modification.GrantKeyword(
                        SourceTypeTargeting.keyword(ability.sourceType.name)
                    ),
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
            // Enforced directly by BlockPhaseManager (scans attackers' statics); no continuous effect.
            is MustBeBlocked -> null
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
                // CastChoicesComponent; replaces all basic land subtypes (Rule 305.7).
                ContinuousEffectData(
                    modification = Modification.SetBasicLandTypesFromChosen,
                    affectsFilter = AffectsFilter.AttachedPermanent
                )
            }
            is GrantLandwalkOfChosenType -> {
                // "Enchanted creature has landwalk of the chosen type" - Layer 6 ability-adding.
                // The landwalk keyword is resolved at projection time from the source's
                // CastChoicesComponent (Plains→Plainswalk, Island→Islandwalk, etc.).
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
            is GrantChosenSubtype -> {
                ContinuousEffectData(
                    modification = Modification.AddChosenSubtype(
                        includeControlledSpells = ability.includeControlledSpells,
                        includeOwnedCardsOutsideBattlefield = ability.includeOwnedCardsOutsideBattlefield
                    ),
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
            is SetBasePowerToughnessDynamicStatic -> {
                ContinuousEffectData(
                    modification = Modification.SetPowerToughnessDynamic(ability.power, ability.toughness),
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
            is SetName -> {
                // "named X" — Layer 3 (TEXT) name override (CR 612 / 613.1c).
                ContinuousEffectData(
                    modification = Modification.SetName(ability.name),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is CantBeTurnedFaceUp -> {
                ContinuousEffectData(
                    modification = Modification.SetCantBeTurnedFaceUp,
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is ConditionalStaticAbility -> convertConditionalStaticAbility(ability)

            // ------------------------------------------------------------------
            // Everything below is NOT projected through the layer system — each
            // type is consulted by the dedicated subsystem named in its group
            // comment. They are listed explicitly (no `else`) so this `when` is
            // exhaustiveness-checked: a new StaticAbility subtype fails
            // compilation here until a deliberate decision is made about its
            // engine half (sdk-analysis-2026-06 §1.1).
            // ------------------------------------------------------------------

            // Multi-effect conversions handled by convertStaticAbilities before
            // this function is reached:
            is AnimateLandGroup,
            is GrantAdditionalTypesToGroup,
            is SetLandTypesForGroup,
            is TransformPermanent,

            // Trigger system (TriggerDetector / TriggerAbilityResolver / TriggerIndex):
            is AdditionalAttackTriggers,
            is AdditionalETBOrLTBTriggers,
            is AdditionalSourceTriggers,
            is GrantTriggeredAbility,

            // Mana production (ManaSolver / ActivateAbilityHandler / ManaAbilityEnumerator):
            is AdditionalManaOnSourceTap,
            is AdditionalManaOnTap,
            is DampLandManaProduction,
            is OverrideEnchantedLandManaColor,
            is ReplaceLandManaColor,

            // Combat: attack/block legality (AttackPhaseManager / BlockPhaseManager /
            // AttackRestrictionRules / BlockEvasionRules / CombatEnumerator):
            is AttackTax,
            is BlockTax,
            is AttackerCountLimit,
            is BlockerCountLimit,
            is CanAttackDespiteDefender,
            is CanBlockAnyNumber,
            is CantAttackUnless,
            is CantAttackUnlessCoAttacker,
            is CantBeAttackedWithout,
            is CantBeBlockedBy,
            is CantBeBlockedByCreaturesWithLessPower,
            is CantBeBlockedByMoreThan,
            is com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan,
            is CantBeBlockedIfCastSpellType,
            is CantBeBlockedUnlessDefenderSharesCreatureType,
            is CantBlockCreaturesWithGreaterPower,
            is CantBlockUnless,
            is CantBlockUnlessCoBlocker,

            // Combat: damage assignment (CombatDamageManager / CombatDamageUtils / DamageUtils):
            is AssignCombatDamageAsUnblocked,
            is AssignDamageEqualToToughness,
            is DivideCombatDamageFreely,
            is NoncombatDamageBonus,

            // Casting permissions, zones and land drops (CastPermissionUtils /
            // CastZoneResolver / CastFromZoneEnumerator / CastSpellHandler /
            // CastSpellEnumerator / PlayLandHandler):
            is CantCastSpellsSharingColorWithLastCast,
            is CastSpellTypesFromTopOfLibrary,
            is GrantAdditionalLandDrop,
            is GrantFlashToSpellType,
            is GrantMayCastFromLinkedExile,
            is GrantWarpToCardsInHand,
            is GrantMiracleToCardsInHand,
            is MayCastFromGraveyard,
            is GraveyardCardsHaveFlashback,
            is com.wingedsheep.sdk.scripting.GraveyardCreaturesHaveSneak,
            is MayCastSelfFromZones,
            is MayCastWithoutPayingManaCost,
            is MayPlayLandsFromGraveyard,
            is MayPlayPermanentsFromGraveyard,
            // Equip-timing/cost permissions (consulted by CastPermissionUtils /
            // ActivatedAbilityEnumerator / ActivateAbilityHandler, not continuous effects):
            is EquipAbilitiesAtInstantSpeed,
            is FreeFirstEquipEachTurn,
            is ReduceEquipCost,
            is ReduceActivatedAbilityCost,
            is PlayFromTopOfLibrary,
            is PlayLandsAndCastFilteredFromTopOfLibrary,
            is PlotFromTopOfLibrary,
            is PlayersCantCastSpells,
            is RestrictSpellsCastPerTurn,

            // Spell costs (CostCalculator):
            is GrantAlternativeCastingCost,
            is ModifySpellCost,

            // Plot special-action cost (PlotCostReducer / PlotEnumerator / PlotCardHandler):
            is ModifyPlotCost,

            // Door-unlock special-action cost (UnlockCostReducer / UnlockRoomDoorEnumerator /
            // UnlockRoomDoorHandler):
            is ModifyUnlockCost,

            // Spells on the stack (StackResolver / GrantedKeywordResolver):
            is GrantCantBeCountered,
            is GrantKeywordToOwnSpells,

            // Activated abilities (ActivateAbilityHandler / ActivatedAbilityEnumerator):
            is ExtraLoyaltyActivation,
            is GrantActivatedAbility,
            is HasAllActivatedAbilitiesOfLinkedExiledCard,
            is com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard,
            is GainActivatedAbilitiesOfPermanents,
            is SpendAnyManaTypeForActivatedAbilities,
            is PreventActivatedAbilities,
            is PlayersCantActivateAbilities,
            is PreventCycling,

            // Trigger detection (TriggerDetector.suppressEntersTriggers) — not a continuous
            // projection effect; consulted as a final filter over the batch's pending triggers:
            is SuppressEntersTriggers,

            // Turn-based actions (BeginningPhaseManager / CleanupPhaseManager):
            is DamagePersistsThroughCleanup,
            is NoMaximumHandSize,
            is SetMaximumHandSize,
            is PreventManaPoolEmptying,
            is ConvertEmptyingManaToRed,
            is UntapDuringOtherUntapSteps,
            is UntapFilteredDuringOtherUntapSteps,
            is UntapSelfDuringOtherUntapSteps,
            is UntapLimitPerStep,

            // Visibility / information (ClientStateTransformer / DrawCardPrimitive):
            is LookAtFaceDownCreatures,
            is LookAtTopOfLibrary,
            is OpponentsPlayWithHandsRevealed,
            is RevealFirstDrawEachTurn,
            is RevealTopOfLibrary,

            // Coin-flip result replacement (CR 705.3), queried by the coin-flip executors via
            // CoinFlipModifiers — not a Rule 613 continuous effect:
            is WinCoinFlips,

            // Stamped as marker components by addContinuousEffectComponent in this
            // handler and read from those components by their subsystems:
            is CantBeTargetedByOpponentAbilities,
            is GrantCantBeBlockedToSmallCreatures,
            is GrantCantLoseGame,
            is com.wingedsheep.sdk.scripting.GrantCantLoseGameFromLife,
            is GrantHexproofToController,
            is GrantProtectionToController,
            is GrantShroudToController,
            is StationUsingToughness,
            is SuppressHexproofForGroup,
            is SuppressWardForGroup -> null
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

    /**
     * Classifies every [com.wingedsheep.sdk.scripting.ReplacementEffect] as either
     * runtime-relevant (stamped into [ReplacementEffectSourceComponent] and consulted while
     * the permanent is on the battlefield) or entry-time (consumed once by the ETB paths in
     * StackResolver / PlayLandHandler / the clone continuations).
     *
     * Exhaustive `when` with no `else` on purpose: a new ReplacementEffect subtype fails
     * compilation here until it's deliberately classified (sdk-analysis-2026-06 §1.1).
     */
    private fun isRuntimeReplacementEffect(it: com.wingedsheep.sdk.scripting.ReplacementEffect): Boolean =
        when (it) {
            // Damage replacement/modification:
            is PreventDamage,
            is DoubleDamage,
            is ModifyDamageAmount,
            is com.wingedsheep.sdk.scripting.CapDamage,
            is com.wingedsheep.sdk.scripting.RedirectDamage,
            is com.wingedsheep.sdk.scripting.DamageCantBePrevented,
            is ReplaceDamageWithCounters,
            is com.wingedsheep.sdk.scripting.ReplaceDamageWithMill,
            // Life gain/loss:
            is PreventLifeGain,
            is com.wingedsheep.sdk.scripting.ModifyLifeGain,
            is com.wingedsheep.sdk.scripting.ModifyLifeLoss,
            is com.wingedsheep.sdk.scripting.LifeLossFloor,
            // Draws:
            is com.wingedsheep.sdk.scripting.PreventDraw,
            is com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect,
            is com.wingedsheep.sdk.scripting.ModifyDrawAmount,
            // Mill:
            is com.wingedsheep.sdk.scripting.ModifyMillAmount,
            // Counter placement:
            is com.wingedsheep.sdk.scripting.ModifyCounterPlacement,
            is com.wingedsheep.sdk.scripting.DoubleCounterPlacement,
            is com.wingedsheep.sdk.scripting.EntersWithCounters,
            is com.wingedsheep.sdk.scripting.EntersWithDynamicCounters,
            // "Lands you control enter untapped" (The Wandering Minstrel): a static effect
            // consulted from the battlefield against OTHER permanents as they enter.
            is com.wingedsheep.sdk.scripting.EntersUntapped,
            // "Nonbasic lands enter tapped" (Zhao, the Moon Slayer): the global counterpart of
            // the self-only EntersTapped, consulted from the battlefield against OTHER permanents.
            is com.wingedsheep.sdk.scripting.PermanentsEnterTapped,
            // Zone changes and turns:
            is com.wingedsheep.sdk.scripting.RedirectZoneChange,
            is com.wingedsheep.sdk.scripting.RedirectZoneChangeWithEffect,
            is com.wingedsheep.sdk.scripting.PreventExtraTurns,
            // Token creation:
            is com.wingedsheep.sdk.scripting.ReplaceTokenCreationWithAttachedCopy,
            is com.wingedsheep.sdk.scripting.DoubleTokenCreation,
            is com.wingedsheep.sdk.scripting.ModifyTokenCount,
            is com.wingedsheep.sdk.scripting.CreateAdditionalToken -> true

            // Entry-time replacements, consumed once as the permanent enters
            // (StackResolver / PlayLandHandler / ModalAndCloneContinuations):
            is com.wingedsheep.sdk.scripting.EntersAsCopy,
            is com.wingedsheep.sdk.scripting.EntersTapped,
            is com.wingedsheep.sdk.scripting.EntersWithChoice,
            is com.wingedsheep.sdk.scripting.EntersWithDevour,
            is com.wingedsheep.sdk.scripting.EntersWithRevealCounters,
            is com.wingedsheep.sdk.scripting.OnEnterRunEffect -> false
        }

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
        // read CastChoicesComponent from the source entity
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

        // Handle controller-only filters (no subtype or other complex predicates).
        // Only predicates this fast path understands map to the simple filters; anything
        // else (composed And/Or/Not, referenced-player, …) must keep full predicate
        // evaluation via Generic rather than silently widening to AllCreatures.
        if (subtypePredicate == null && !hasAnyOfSubtypes && baseFilter.statePredicates.isEmpty()) {
            return when (controllerPredicate) {
                ControllerPredicate.ControlledByYou -> AffectsFilter.AllCreaturesYouControl
                ControllerPredicate.ControlledByOpponent -> AffectsFilter.AllCreaturesOpponentsControl
                null, ControllerPredicate.ControlledByAny -> AffectsFilter.AllCreatures
                else -> AffectsFilter.Generic(filter)
            }
        }

        // Fallback: use Generic filter to preserve full predicate evaluation
        return AffectsFilter.Generic(filter)
    }
}
