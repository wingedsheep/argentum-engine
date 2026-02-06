package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GlobalEffect
import com.wingedsheep.sdk.scripting.GlobalEffectType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ControllerPredicate
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StatePredicate
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.StaticTarget

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
    private val cardRegistry: CardRegistry? = null
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
        val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId) ?: return container

        // Convert static abilities to continuous effect data
        val effectsData = cardDef.staticAbilities.mapNotNull { ability ->
            convertStaticAbility(ability)
        }

        if (effectsData.isEmpty()) {
            return container
        }

        // Add the ContinuousEffectSourceComponent
        return container.with(ContinuousEffectSourceComponent(effectsData))
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
        // Convert static abilities to continuous effect data
        val effectsData = cardDefinition.staticAbilities.mapNotNull { ability ->
            convertStaticAbility(ability)
        }

        if (effectsData.isEmpty()) {
            return container
        }

        // Add the ContinuousEffectSourceComponent
        return container.with(ContinuousEffectSourceComponent(effectsData))
    }

    /**
     * Convert a static ability to ContinuousEffectData.
     *
     * @param ability The static ability to convert
     * @return The continuous effect data, or null if this ability type isn't supported yet
     */
    private fun convertStaticAbility(ability: StaticAbility): ContinuousEffectData? {
        return when (ability) {
            is GrantKeywordToCreatureGroup -> {
                ContinuousEffectData(
                    layer = Layer.ABILITY,
                    sublayer = null,
                    modification = Modification.GrantKeyword(ability.keyword.name),
                    affectsFilter = convertGroupFilter(ability.filter)
                )
            }
            is ModifyStats -> {
                ContinuousEffectData(
                    layer = Layer.POWER_TOUGHNESS,
                    sublayer = Sublayer.MODIFICATIONS,
                    modification = Modification.ModifyPowerToughness(ability.powerBonus, ability.toughnessBonus),
                    affectsFilter = convertStaticTarget(ability.target)
                )
            }
            is GrantKeyword -> {
                ContinuousEffectData(
                    layer = Layer.ABILITY,
                    sublayer = null,
                    modification = Modification.GrantKeyword(ability.keyword.name),
                    affectsFilter = convertStaticTarget(ability.target)
                )
            }
            is CantAttack -> {
                ContinuousEffectData(
                    layer = Layer.ABILITY,
                    sublayer = null,
                    modification = Modification.SetCantAttack,
                    affectsFilter = convertStaticTarget(ability.target)
                )
            }
            is CantBlock -> {
                ContinuousEffectData(
                    layer = Layer.ABILITY,
                    sublayer = null,
                    modification = Modification.SetCantBlock,
                    affectsFilter = convertStaticTarget(ability.target)
                )
            }
            is ControlEnchantedPermanent -> {
                // "You control enchanted permanent" - Layer 2 control-changing effect
                // The actual newControllerId is resolved dynamically by the StateProjector
                // using a placeholder; the Aura's controller is used at projection time
                ContinuousEffectData(
                    layer = Layer.CONTROL,
                    sublayer = null,
                    modification = Modification.ChangeControllerToSourceController,
                    affectsFilter = AffectsFilter.AttachedPermanent
                )
            }
            is GlobalEffect -> convertGlobalEffect(ability)
            else -> null
        }
    }

    /**
     * Convert a GlobalEffect to ContinuousEffectData.
     */
    private fun convertGlobalEffect(effect: GlobalEffect): ContinuousEffectData {
        val (layer, sublayer, modification) = when (effect.effectType) {
            GlobalEffectType.ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE,
            GlobalEffectType.YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE ->
                Triple(Layer.POWER_TOUGHNESS, Sublayer.MODIFICATIONS, Modification.ModifyPowerToughness(1, 1))

            GlobalEffectType.OPPONENT_CREATURES_GET_MINUS_ONE_MINUS_ONE ->
                Triple(Layer.POWER_TOUGHNESS, Sublayer.MODIFICATIONS, Modification.ModifyPowerToughness(-1, -1))

            GlobalEffectType.ALL_CREATURES_HAVE_FLYING ->
                Triple(Layer.ABILITY, null, Modification.GrantKeyword("FLYING"))

            GlobalEffectType.YOUR_CREATURES_HAVE_VIGILANCE ->
                Triple(Layer.ABILITY, null, Modification.GrantKeyword("VIGILANCE"))

            GlobalEffectType.YOUR_CREATURES_HAVE_LIFELINK ->
                Triple(Layer.ABILITY, null, Modification.GrantKeyword("LIFELINK"))

            GlobalEffectType.CREATURES_CANT_ATTACK,
            GlobalEffectType.CREATURES_CANT_BLOCK ->
                Triple(Layer.ABILITY, null, Modification.NoOp)
        }

        return ContinuousEffectData(
            layer = layer,
            sublayer = sublayer,
            modification = modification,
            affectsFilter = convertGroupFilter(effect.filter)
        )
    }

    /**
     * Convert a StaticTarget to an AffectsFilter.
     */
    private fun convertStaticTarget(target: StaticTarget): AffectsFilter {
        return when (target) {
            is StaticTarget.AttachedCreature -> AffectsFilter.AttachedPermanent
            is StaticTarget.SourceCreature -> AffectsFilter.Self
            is StaticTarget.AllControlledCreatures -> AffectsFilter.AllCreaturesYouControl
            is StaticTarget.Controller -> AffectsFilter.Self
            is StaticTarget.SpecificCard -> AffectsFilter.SpecificEntities(setOf(target.entityId))
        }
    }

    /**
     * Convert a GroupFilter to an AffectsFilter.
     */
    private fun convertGroupFilter(filter: GroupFilter): AffectsFilter {
        val baseFilter = filter.baseFilter
        val controllerPredicate = baseFilter.controllerPredicate
        val hasExcludeSelf = filter.excludeSelf
        val hasTappedPredicate = baseFilter.statePredicates.any { it == StatePredicate.IsTapped }

        // Handle "other tapped creatures you control" pattern
        if (hasExcludeSelf && hasTappedPredicate && controllerPredicate == ControllerPredicate.ControlledByYou) {
            return AffectsFilter.OtherTappedCreaturesYouControl
        }

        // Handle "other creatures" pattern
        if (hasExcludeSelf) {
            return AffectsFilter.AllOtherCreatures
        }

        // Handle controller-based filters
        return when (controllerPredicate) {
            ControllerPredicate.ControlledByYou -> AffectsFilter.AllCreaturesYouControl
            ControllerPredicate.ControlledByOpponent -> AffectsFilter.AllCreaturesOpponentsControl
            else -> AffectsFilter.AllCreatures
        }
    }
}
