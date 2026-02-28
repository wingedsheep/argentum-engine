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
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.FaceDownSpellCostReduction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.IncreaseMorphCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.ReduceSpellColoredCostBySubtype
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype
import com.wingedsheep.sdk.scripting.SpellCostReduction
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

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

        // Evaluate ReduceSpellCostBySubtype from battlefield permanents controlled by the caster
        totalReduction += calculateSubtypeCostReduction(state, cardDef, casterId)

        // Evaluate ReduceSpellCostByFilter from battlefield permanents controlled by the caster
        totalReduction += calculateFilterCostReduction(state, cardDef, casterId)

        // First apply generic cost reduction
        var effectiveCost = reduceGenericCost(cardDef.manaCost, totalReduction)

        // Then apply colored cost reductions
        effectiveCost = applyColoredCostReductions(state, cardDef, casterId, effectiveCost)

        return effectiveCost
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
     * Calculate cost reduction from battlefield permanents that reduce spell costs by subtype.
     * Scans permanents controlled by the caster for ReduceSpellCostBySubtype abilities
     * and sums reductions for matching subtypes.
     *
     * Used for cards like Goblin Warchief ("Goblin spells you cast cost {1} less"),
     * Undead Warchief ("Zombie spells you cast cost {1} less"), etc.
     */
    private fun calculateSubtypeCostReduction(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId
    ): Int {
        val spellSubtypes = cardDef.typeLine.subtypes
        if (spellSubtypes.isEmpty()) return 0

        var reduction = 0
        for (entityId in state.getBattlefield(casterId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val permanentDef = cardRegistry?.getCard(card.cardDefinitionId) ?: continue

            for (ability in permanentDef.script.staticAbilities) {
                if (ability is ReduceSpellCostBySubtype &&
                    Subtype.of(ability.subtype) in spellSubtypes
                ) {
                    reduction += ability.amount
                }
            }
        }
        return reduction
    }

    /**
     * Calculate cost reduction from battlefield permanents with ReduceSpellCostByFilter abilities.
     * Evaluates each filter's card predicates against the spell's CardDefinition.
     * Passes source entity context for predicates like SharesCreatureTypeWithSource
     * that need to cross-reference the source permanent's projected state.
     *
     * Used for general filter-based cost reductions like Krosan Drover
     * ("Creature spells you cast with mana value 6 or greater cost {2} less to cast.")
     * and Mistform Warchief ("Creature spells that share a creature type with this creature cost {1} less").
     */
    private fun calculateFilterCostReduction(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId
    ): Int {
        val projectedState = stateProjector?.project(state)

        var reduction = 0
        for (entityId in state.getBattlefield(casterId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val permanentDef = cardRegistry?.getCard(card.cardDefinitionId) ?: continue

            for (ability in permanentDef.script.staticAbilities) {
                if (ability is ReduceSpellCostByFilter &&
                    matchesCardDefinition(cardDef, ability.filter, entityId, state, projectedState)
                ) {
                    reduction += ability.amount
                }
            }
        }
        return reduction
    }

    /**
     * Apply colored cost reductions from battlefield permanents with ReduceSpellColoredCostBySubtype.
     * Removes specific colored mana symbols from the spell's cost.
     * Only reduces colored mana, never generic mana.
     */
    private fun applyColoredCostReductions(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId,
        currentCost: ManaCost
    ): ManaCost {
        val spellSubtypes = cardDef.typeLine.subtypes
        if (spellSubtypes.isEmpty()) return currentCost

        // Collect all colored mana symbols to remove
        val symbolsToRemove = mutableListOf<ManaSymbol>()

        for (entityId in state.getBattlefield(casterId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val permanentDef = cardRegistry?.getCard(card.cardDefinitionId) ?: continue

            for (ability in permanentDef.script.staticAbilities) {
                if (ability is ReduceSpellColoredCostBySubtype &&
                    Subtype.of(ability.subtype) in spellSubtypes
                ) {
                    val reductionCost = ManaCost.parse(ability.manaReduction)
                    symbolsToRemove.addAll(reductionCost.symbols.filterIsInstance<ManaSymbol.Colored>())
                }
            }
        }

        if (symbolsToRemove.isEmpty()) return currentCost

        return reduceColoredCost(currentCost, symbolsToRemove)
    }

    /**
     * Remove specific colored mana symbols from a mana cost.
     * Each symbol in symbolsToRemove removes at most one matching colored symbol from the cost.
     * Does not affect generic mana.
     */
    private fun reduceColoredCost(cost: ManaCost, symbolsToRemove: List<ManaSymbol>): ManaCost {
        val remainingSymbols = cost.symbols.toMutableList()

        for (symbolToRemove in symbolsToRemove) {
            val index = remainingSymbols.indexOfFirst { it == symbolToRemove }
            if (index >= 0) {
                remainingSymbols.removeAt(index)
            }
        }

        return ManaCost(remainingSymbols)
    }

    /**
     * Match a CardDefinition against a GameObjectFilter's card predicates.
     * Only evaluates card predicates (type, subtype, color, mana value, etc.)
     * since state and controller predicates don't apply to spells being cast.
     *
     * @param sourceEntityId The entity providing the cost reduction (for source-relative predicates)
     * @param state The current game state (for source-relative predicates)
     * @param projectedState The projected state (for source-relative predicates using projected types)
     */
    private fun matchesCardDefinition(
        cardDef: CardDefinition,
        filter: GameObjectFilter,
        sourceEntityId: EntityId? = null,
        state: GameState? = null,
        projectedState: com.wingedsheep.engine.mechanics.layers.ProjectedState? = null
    ): Boolean {
        if (filter.cardPredicates.isEmpty()) return true
        return if (filter.matchAll) {
            filter.cardPredicates.all { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }
        } else {
            filter.cardPredicates.any { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }
        }
    }

    private fun matchesCardPredicate(
        cardDef: CardDefinition,
        predicate: CardPredicate,
        sourceEntityId: EntityId? = null,
        state: GameState? = null,
        projectedState: com.wingedsheep.engine.mechanics.layers.ProjectedState? = null
    ): Boolean {
        val typeLine = cardDef.typeLine
        return when (predicate) {
            CardPredicate.IsCreature -> typeLine.isCreature
            CardPredicate.IsLand -> typeLine.isLand
            CardPredicate.IsArtifact -> typeLine.isArtifact
            CardPredicate.IsEnchantment -> typeLine.isEnchantment
            CardPredicate.IsPlaneswalker -> CardType.PLANESWALKER in typeLine.cardTypes
            CardPredicate.IsInstant -> typeLine.isInstant
            CardPredicate.IsSorcery -> typeLine.isSorcery
            CardPredicate.IsBasicLand -> typeLine.isBasicLand
            CardPredicate.IsPermanent -> typeLine.isPermanent
            CardPredicate.IsNonland -> !typeLine.isLand
            CardPredicate.IsNoncreature -> !typeLine.isCreature
            CardPredicate.IsToken -> false
            CardPredicate.IsNontoken -> true

            is CardPredicate.HasColor -> predicate.color in cardDef.colors
            is CardPredicate.NotColor -> predicate.color !in cardDef.colors
            CardPredicate.IsColorless -> cardDef.colors.isEmpty()
            CardPredicate.IsMulticolored -> cardDef.colors.size > 1
            CardPredicate.IsMonocolored -> cardDef.colors.size == 1

            is CardPredicate.HasSubtype -> typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.NotSubtype -> !typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.HasBasicLandType -> typeLine.hasSubtype(Subtype(predicate.landType))
            is CardPredicate.NameEquals -> cardDef.name == predicate.name

            is CardPredicate.HasKeyword -> predicate.keyword in cardDef.keywords
            is CardPredicate.NotKeyword -> predicate.keyword !in cardDef.keywords

            is CardPredicate.ManaValueEquals -> cardDef.manaCost.cmc == predicate.value
            is CardPredicate.ManaValueAtMost -> cardDef.manaCost.cmc <= predicate.max
            is CardPredicate.ManaValueAtLeast -> cardDef.manaCost.cmc >= predicate.min

            is CardPredicate.PowerEquals -> cardDef.creatureStats?.basePower == predicate.value
            is CardPredicate.PowerAtMost -> (cardDef.creatureStats?.basePower ?: 0) <= predicate.max
            is CardPredicate.PowerAtLeast -> (cardDef.creatureStats?.basePower ?: 0) >= predicate.min
            is CardPredicate.ToughnessEquals -> cardDef.creatureStats?.baseToughness == predicate.value
            is CardPredicate.ToughnessAtMost -> (cardDef.creatureStats?.baseToughness ?: 0) <= predicate.max
            is CardPredicate.ToughnessAtLeast -> (cardDef.creatureStats?.baseToughness ?: 0) >= predicate.min

            CardPredicate.NotOfSourceChosenType -> true

            CardPredicate.SharesCreatureTypeWithSource -> {
                if (sourceEntityId == null) return true
                val spellSubtypes = cardDef.typeLine.subtypes
                if (spellSubtypes.isEmpty()) return false
                // Use projected subtypes (accounts for BecomeCreatureType effects)
                val sourceSubtypes = if (projectedState != null) {
                    projectedState.getSubtypes(sourceEntityId)
                } else {
                    val card = state?.getEntity(sourceEntityId)?.get<CardComponent>()
                    card?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
                }
                spellSubtypes.any { spellSubtype ->
                    sourceSubtypes.any { it.equals(spellSubtype.value, ignoreCase = true) }
                }
            }

            CardPredicate.SharesCreatureTypeWithTriggeringEntity -> true // Not applicable in cost calculation

            is CardPredicate.And -> predicate.predicates.all { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }
            is CardPredicate.Or -> predicate.predicates.any { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }
            is CardPredicate.Not -> !matchesCardPredicate(cardDef, predicate.predicate, sourceEntityId, state, projectedState)

            // Not applicable in cost calculation — abilities aren't cards
            CardPredicate.IsActivatedOrTriggeredAbility -> false
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

    /**
     * Calculate the total morph cost increase from all IncreaseMorphCost abilities on the battlefield.
     * Scans ALL permanents on the battlefield (not just those controlled by a specific player)
     * since IncreaseMorphCost affects all players globally.
     *
     * @param state The current game state
     * @return The total generic mana increase to apply to morph (turn face-up) costs
     */
    fun calculateMorphCostIncrease(state: GameState): Int {
        var totalIncrease = 0

        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry?.getCard(card.cardDefinitionId) ?: continue

                for (ability in cardDef.script.staticAbilities) {
                    if (ability is IncreaseMorphCost) {
                        totalIncrease += ability.amount
                    }
                }
            }
        }

        return totalIncrease
    }

    /**
     * Apply a generic mana increase to an existing ManaCost.
     * Used to increase morph costs by adding generic mana.
     */
    fun increaseGenericCost(cost: ManaCost, increase: Int): ManaCost {
        if (increase <= 0) return cost

        val coloredSymbols = cost.symbols.filter { it !is ManaSymbol.Generic }
        val currentGeneric = cost.genericAmount
        val newGeneric = currentGeneric + increase

        val newSymbols = if (newGeneric > 0) {
            listOf(ManaSymbol.Generic(newGeneric)) + coloredSymbols
        } else {
            coloredSymbols
        }

        return ManaCost(newSymbols)
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
