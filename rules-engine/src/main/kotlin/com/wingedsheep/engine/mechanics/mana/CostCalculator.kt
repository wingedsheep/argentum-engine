package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.FaceDownSpellCostReduction
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ReduceFaceDownCastingCost
import com.wingedsheep.sdk.scripting.SpellCostReduction

/**
 * Calculates effective spell costs after applying cost reductions.
 *
 * Supports:
 * - SpellCostReduction static abilities (e.g., Ghalta's "costs {X} less where X is total power")
 * - Affinity keyword abilities (e.g., "Affinity for artifacts")
 *
 * Cost reductions only reduce generic mana costs, never colored costs.
 * A spell's cost can never be reduced below its colored mana requirements.
 */
class CostCalculator(
    private val cardRegistry: CardRegistry? = null,
    private val stateProjector: StateProjector? = null
) {

    /**
     * Calculate the effective cost of casting a spell after applying all cost reductions.
     *
     * @param state The current game state
     * @param cardDef The card definition being cast
     * @param casterId The player casting the spell
     * @return The effective mana cost after reductions
     */
    fun calculateEffectiveCost(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId
    ): ManaCost {
        var totalReduction = 0

        // Evaluate SpellCostReduction static abilities
        for (ability in cardDef.script.staticAbilities) {
            if (ability is SpellCostReduction) {
                totalReduction += evaluateReduction(state, ability.reductionSource, casterId)
            }
        }

        // Evaluate Affinity keyword abilities
        for (keywordAbility in cardDef.keywordAbilities) {
            if (keywordAbility is KeywordAbility.Affinity) {
                totalReduction += countPermanentsOfType(state, casterId, keywordAbility.forType)
            }
        }

        return reduceGenericCost(cardDef.manaCost, totalReduction)
    }

    /**
     * Calculate the effective cost of casting a face-down creature spell.
     * Checks battlefield permanents controlled by the caster for ReduceFaceDownCastingCost abilities.
     *
     * @param state The current game state
     * @param casterId The player casting the spell
     * @return The effective mana cost after reductions (base {3} minus reductions)
     */
    fun calculateFaceDownCost(
        state: GameState,
        casterId: EntityId
    ): ManaCost {
        val baseCost = ManaCost.parse("{3}")
        var totalReduction = 0

        for (entityId in state.getBattlefield(casterId)) {
            val cardComponent = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry?.getCard(cardComponent.cardDefinitionId) ?: continue

            for (ability in cardDef.script.staticAbilities) {
                if (ability is ReduceFaceDownCastingCost) {
                    totalReduction += ability.amount
                }
            }
        }

        return reduceGenericCost(baseCost, totalReduction)
    }

    /**
     * Evaluate the reduction amount from a CostReductionSource.
     */
    private fun evaluateReduction(
        state: GameState,
        source: CostReductionSource,
        playerId: EntityId
    ): Int {
        return when (source) {
            is CostReductionSource.Fixed -> source.amount
            is CostReductionSource.CreaturesYouControl -> countCreatures(state, playerId)
            is CostReductionSource.TotalPowerYouControl -> sumPower(state, playerId)
            is CostReductionSource.ArtifactsYouControl -> countArtifacts(state, playerId)
            is CostReductionSource.ColorsAmongPermanentsYouControl -> countColors(state, playerId)
        }
    }

    /**
     * Count creatures controlled by a player.
     */
    private fun countCreatures(state: GameState, playerId: EntityId): Int {
        return state.getBattlefield(playerId).count { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            card?.typeLine?.isCreature == true
        }
    }

    /**
     * Sum the power of all creatures controlled by a player.
     * Uses projected state if available to account for continuous effects.
     */
    private fun sumPower(state: GameState, playerId: EntityId): Int {
        // Get projected state if projector is available
        val projectedState = stateProjector?.project(state)

        var totalPower = 0
        for (entityId in state.getBattlefield(playerId)) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue

            if (!card.typeLine.isCreature) continue

            // Use projected power if available, otherwise fall back to base stats
            val basePower: Int = when (val p = card.baseStats?.power) {
                is CharacteristicValue.Fixed -> p.value
                is CharacteristicValue.Dynamic -> 0  // Dynamic values need state evaluation, use 0 for now
                is CharacteristicValue.DynamicWithOffset -> p.offset  // Use base offset for now
                null -> 0
            }
            val projectedPower: Int? = projectedState?.getPower(entityId)
            val power: Int = (projectedPower ?: basePower).coerceAtLeast(0)

            totalPower += power  // Negative power doesn't reduce cost
        }
        return totalPower
    }

    /**
     * Count artifacts controlled by a player.
     */
    private fun countArtifacts(state: GameState, playerId: EntityId): Int {
        return state.getBattlefield(playerId).count { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            card?.typeLine?.isArtifact == true
        }
    }

    /**
     * Count unique colors among permanents controlled by a player.
     * Used for Vivid cost reduction.
     */
    private fun countColors(state: GameState, playerId: EntityId): Int {
        val colors = mutableSetOf<Color>()

        state.getBattlefield(playerId).forEach { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@forEach
            colors.addAll(card.colors)
        }

        return colors.size
    }

    /**
     * Count permanents of a specific card type controlled by a player.
     * Used for Affinity keyword.
     */
    private fun countPermanentsOfType(state: GameState, playerId: EntityId, cardType: CardType): Int {
        return state.getBattlefield(playerId).count { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            card?.typeLine?.cardTypes?.contains(cardType) == true
        }
    }

    /**
     * Reduce the generic mana cost by the specified amount.
     * Never reduces below 0 generic mana. Colored costs are preserved.
     */
    private fun reduceGenericCost(cost: ManaCost, reduction: Int): ManaCost {
        if (reduction <= 0) return cost

        // Separate colored and generic symbols
        val coloredSymbols = cost.symbols.filter { it !is ManaSymbol.Generic }
        val genericAmount = cost.genericAmount

        // Apply reduction to generic cost only
        val newGenericAmount = (genericAmount - reduction).coerceAtLeast(0)

        // Rebuild the mana cost
        val newSymbols = if (newGenericAmount > 0) {
            listOf(ManaSymbol.Generic(newGenericAmount)) + coloredSymbols
        } else {
            coloredSymbols
        }

        return ManaCost(newSymbols)
    }

    /**
     * Calculate the effective cost of casting a face-down creature spell (morph).
     * The base cost is {3}, reduced by FaceDownSpellCostReduction abilities
     * on permanents the caster controls.
     *
     * @param state The current game state
     * @param casterId The player casting the spell
     * @return The effective morph cost after reductions
     */
    fun calculateFaceDownCost(state: GameState, casterId: EntityId): ManaCost {
        val baseMorphCost = ManaCost.parse("{3}")
        var totalReduction = 0

        // Scan battlefield permanents controlled by the caster for FaceDownSpellCostReduction
        for (entityId in state.getBattlefield(casterId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry?.getCard(card.cardDefinitionId) ?: continue

            for (ability in cardDef.script.staticAbilities) {
                if (ability is FaceDownSpellCostReduction) {
                    totalReduction += evaluateReduction(state, ability.reductionSource, casterId)
                }
            }
        }

        return reduceGenericCost(baseMorphCost, totalReduction)
    }

    companion object {
        /**
         * Check if a card has any cost reduction abilities.
         */
        fun hasCostReduction(cardDef: CardDefinition): Boolean {
            val hasSpellCostReduction = cardDef.script.staticAbilities.any { it is SpellCostReduction }
            val hasAffinity = cardDef.keywordAbilities.any { it is KeywordAbility.Affinity }
            return hasSpellCostReduction || hasAffinity
        }
    }
}
