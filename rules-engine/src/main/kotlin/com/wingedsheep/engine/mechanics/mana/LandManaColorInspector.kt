package com.wingedsheep.engine.mechanics.mana
import com.wingedsheep.engine.state.components.battlefield.chosenColor

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Inspects what colors a land's mana abilities *could* produce, regardless of whether
 * those abilities can currently be activated. Used by Fellwar Stone-style effects
 * (CR rulings: "checks the effects of all mana-producing abilities of lands ... but
 * doesn't check their costs").
 *
 * Colorless production is intentionally ignored — Fellwar Stone-style abilities
 * produce only colored mana ("Fellwar Stone can't be tapped for colorless mana, even
 * if a land an opponent controls could produce colorless mana").
 */
object LandManaColorInspector {

    /**
     * The colors that any land in [landIds] could produce.
     */
    fun colorsLandsCouldProduce(
        state: GameState,
        projected: ProjectedState,
        landIds: Iterable<EntityId>,
        cardRegistry: CardRegistry,
    ): Set<Color> {
        val colors = mutableSetOf<Color>()
        for (landId in landIds) {
            colors.addAll(colorsLandCouldProduce(state, projected, landId, cardRegistry))
            if (colors.size == Color.entries.size) return colors
        }
        return colors
    }

    /**
     * The colors a single land could produce via any of its mana abilities.
     * Returns the empty set for non-lands.
     */
    fun colorsLandCouldProduce(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        cardRegistry: CardRegistry,
    ): Set<Color> {
        val container = state.getEntity(entityId) ?: return emptySet()
        val card = container.get<CardComponent>() ?: return emptySet()
        if (!card.typeLine.isLand) return emptySet()

        val colors = mutableSetOf<Color>()

        // Intrinsic abilities derived from projected basic-land subtypes (Plains/Island/...)
        for (ability in IntrinsicManaAbilities.forEntity(state, projected, entityId)) {
            collectColors(ability.effect, container, colors)
        }

        // Card-defined activated abilities (if any). Skip when intrinsic abilities are
        // present — basic land types replace the printed mana ability (matches
        // ManaAbilityEnumerator's behavior for shock lands etc.).
        val intrinsicLandColors = colors.toSet()
        if (intrinsicLandColors.isEmpty()) {
            val cardDef = cardRegistry.getCard(card.cardDefinitionId)
            if (cardDef != null) {
                for (ability in cardDef.script.activatedAbilities) {
                    if (!ability.isManaAbility) continue
                    collectColors(ability.effect, container, colors)
                }
            }
        }

        // Granted mana abilities (e.g., from auras / continuous effects)
        val grantedAbilities = state.grantedActivatedAbilities
            .asSequence()
            .filter { it.entityId == entityId }
            .map { it.ability }
            .filter { it.isManaAbility }
        for (ability in grantedAbilities) {
            collectColors(ability.effect, container, colors)
        }

        return colors
    }

    private fun collectColors(
        effect: Effect,
        sourceContainer: com.wingedsheep.engine.state.ComponentContainer,
        out: MutableSet<Color>,
    ) {
        when (effect) {
            is AddManaEffect -> out.add(effect.color)
            is AddColorlessManaEffect -> Unit
            is AddManaOfChoiceEffect -> collectChoiceColors(effect.colorSet, sourceContainer, out)
            is AddAnyColorManaSpendOnChosenTypeEffect -> out.addAll(Color.entries)
            is AddDynamicManaEffect -> out.addAll(effect.allowedColors)
            is CompositeEffect -> {
                for (sub in effect.effects) collectColors(sub, sourceContainer, out)
            }
            else -> Unit
        }
    }

    /**
     * Stateless approximation of [ManaColorSetResolver] used here because we only have a
     * source container, not full game state. For color sets that need state to resolve
     * (commander identity, colors among permanents, lands could produce), we
     * conservatively report all five colors — matches the legacy `AddManaOfColorAmong`
     * behavior and is the safe choice for "could this land produce X?" lookups.
     */
    private fun collectChoiceColors(
        colorSet: ManaColorSet,
        sourceContainer: com.wingedsheep.engine.state.ComponentContainer,
        out: MutableSet<Color>,
    ) {
        when (colorSet) {
            is ManaColorSet.AnyColor -> out.addAll(Color.entries)
            is ManaColorSet.Specific -> out.addAll(colorSet.colors)
            is ManaColorSet.SourceChosenColor -> {
                sourceContainer.chosenColor()?.let { out.add(it) }
            }
            is ManaColorSet.CommanderIdentity,
            is ManaColorSet.AmongPermanents,
            is ManaColorSet.AmongCardsInGraveyard,
            is ManaColorSet.LandsCouldProduce -> out.addAll(Color.entries)
        }
    }
}
