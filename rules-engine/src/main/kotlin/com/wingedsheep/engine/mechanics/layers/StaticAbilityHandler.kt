package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ControllerPredicate
import com.wingedsheep.sdk.scripting.StatePredicate
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
            // TODO: Add support for other static ability types as needed:
            // - GlobalEffect (for effects like "All creatures get +1/+1")
            // - GrantKeyword (for equipment granting keywords)
            // - ModifyStats (for equipment granting P/T)
            else -> null
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
