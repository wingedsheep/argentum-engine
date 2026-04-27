package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
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
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.FaceDownSpellCostReduction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAlternativeCastingCost
import com.wingedsheep.sdk.scripting.IncreaseMorphCost
import com.wingedsheep.sdk.scripting.IncreaseSpellCostByFilter
import com.wingedsheep.sdk.scripting.IncreaseSpellCostByPlayerSpellsCast
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ReduceFirstSpellOfTypeColoredCost
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.ReduceSpellColoredCostBySubtype
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype
import com.wingedsheep.sdk.scripting.SpellCostReduction
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
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
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
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
        casterId: EntityId,
        chosenTargets: List<EntityId> = emptyList()
    ): ManaCost {
        var totalReduction = 0

        // Evaluate SpellCostReduction static abilities
        for (ability in cardDef.script.staticAbilities) {
            if (ability is SpellCostReduction) {
                totalReduction += evaluateReduction(state, ability.reductionSource, casterId, chosenTargets)
            }
        }

        // Evaluate Affinity keyword abilities
        for (keywordAbility in cardDef.keywordAbilities) {
            if (keywordAbility is KeywordAbility.Affinity) {
                totalReduction += countPermanentsOfType(state, casterId, keywordAbility.forType)
            }
            if (keywordAbility is KeywordAbility.AffinityForSubtype) {
                totalReduction += countPermanentsWithSubtype(state, casterId, keywordAbility.forSubtype)
            }
        }

        // Evaluate ReduceSpellCostBySubtype from battlefield permanents controlled by the caster
        totalReduction += calculateSubtypeCostReduction(state, cardDef, casterId)

        // Evaluate ReduceSpellCostByFilter from battlefield permanents controlled by the caster
        totalReduction += calculateFilterCostReduction(state, cardDef, casterId)

        // Calculate cost increases from global tax effects (e.g., Glowrider, Damping Sphere)
        val totalIncrease = calculateFilterCostIncrease(state, cardDef, casterId)

        // First apply generic cost reduction
        var effectiveCost = reduceGenericCost(cardDef.manaCost, totalReduction)

        // Then apply colored cost reductions
        effectiveCost = applyColoredCostReductions(state, cardDef, casterId, effectiveCost)

        // Apply cost increases after reductions
        effectiveCost = increaseGenericCost(effectiveCost, totalIncrease)

        return effectiveCost
    }

    /**
     * Evaluate the reduction amount from a CostReductionSource.
     */
    private fun evaluateReduction(
        state: GameState,
        source: CostReductionSource,
        playerId: EntityId,
        chosenTargets: List<EntityId> = emptyList()
    ): Int {
        return when (source) {
            is CostReductionSource.Fixed -> source.amount
            is CostReductionSource.CreaturesYouControl -> countCreatures(state, playerId)
            is CostReductionSource.TotalPowerYouControl -> sumPower(state, playerId)
            is CostReductionSource.ArtifactsYouControl -> countArtifacts(state, playerId)
            is CostReductionSource.ColorsAmongPermanentsYouControl -> countColors(state, playerId)
            is CostReductionSource.FixedIfControlFilter -> {
                val controls = controlsMatchingPermanent(state, playerId, source.filter)
                if (controls) source.amount else 0
            }
            is CostReductionSource.CardsInGraveyardMatchingFilter -> {
                countGraveyardCardsMatchingFilter(state, playerId, source.filter) * source.amountPerCard
            }
            is CostReductionSource.CardsInGraveyardAndExileMatchingFilter -> {
                val graveyardCount = countGraveyardCardsMatchingFilter(state, playerId, source.filter)
                val exileCount = countExileCardsMatchingFilter(state, playerId, source.filter)
                (graveyardCount + exileCount) * source.amountPerCard
            }
            is CostReductionSource.PermanentsWithCounterYouControl -> {
                countPermanentsWithCounter(state, playerId, source.filter, source.counterType)
            }
            is CostReductionSource.FixedIfAnyTargetMatches -> {
                if (chosenTargets.isEmpty()) 0
                else if (anyTargetMatchesFilter(state, playerId, chosenTargets, source.filter)) source.amount
                else 0
            }
            is CostReductionSource.FixedIfCreatureAttackingYou -> {
                if (isAnyCreatureAttacking(state, playerId)) source.amount else 0
            }
        }
    }

    /**
     * Returns true if any creature on the battlefield is currently attacking [playerId]
     * or a planeswalker they control. Reads [AttackingComponent.defenderId] against the
     * caster and their controlled planeswalkers.
     */
    private fun isAnyCreatureAttacking(state: GameState, playerId: EntityId): Boolean {
        val projected = state.projectedState
        val planeswalkersControlled = state.getBattlefield()
            .filter { id ->
                projected.isPlaneswalker(id) && projected.getController(id) == playerId
            }.toSet()
        for (entityId in state.getBattlefield()) {
            val attacking = state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
                ?: continue
            if (attacking.defenderId == playerId || attacking.defenderId in planeswalkersControlled) {
                return true
            }
        }
        return false
    }

    /**
     * Calculate the minimum possible cost for affordability gating during legal-action
     * enumeration. For target-conditional reductions, this assumes the reduction WILL apply
     * iff at least one legal target matching the filter currently exists on the battlefield.
     *
     * Used by CastSpellEnumerator so spells like Dire Downdraft show as castable when a
     * discounted target exists, even though the actual cost is locked from chosen targets
     * at cast time.
     */
    fun calculateMinPossibleCost(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId
    ): ManaCost {
        val optimisticTargets = mutableListOf<EntityId>()
        for (ability in cardDef.script.staticAbilities) {
            if (ability is SpellCostReduction) {
                val src = ability.reductionSource
                if (src is CostReductionSource.FixedIfAnyTargetMatches) {
                    val match = findAnyBattlefieldMatch(state, casterId, src.filter)
                    if (match != null) optimisticTargets += match
                }
            }
        }
        return calculateEffectiveCost(state, cardDef, casterId, optimisticTargets)
    }

    /**
     * Check whether any of the given targets (which may be in any zone, but typically
     * battlefield permanents) satisfies the filter. Uses projected state.
     */
    private fun anyTargetMatchesFilter(
        state: GameState,
        playerId: EntityId,
        targets: List<EntityId>,
        filter: GameObjectFilter
    ): Boolean {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        return targets.any { entityId ->
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, context)
        }
    }

    /**
     * Find any battlefield permanent matching the filter (both players' battlefields).
     * Used by calculateMinPossibleCost to check if a legal discounted target exists.
     */
    private fun findAnyBattlefieldMatch(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): EntityId? {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        for (entityId in state.getBattlefield()) {
            if (predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, context)) {
                return entityId
            }
        }
        return null
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
        val projectedState = state.projectedState

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
            val projectedPower: Int? = projectedState.getPower(entityId)
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
     * Check if a player controls any permanent matching a GameObjectFilter.
     * Uses projected state for type/subtype matching to account for continuous effects.
     */
    private fun controlsMatchingPermanent(state: GameState, playerId: EntityId, filter: GameObjectFilter): Boolean {
        val projectedState = state.projectedState
        return state.getBattlefield(playerId).any { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@any false
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return@any false
            filter.cardPredicates.all { predicate ->
                matchesBattlefieldPredicate(entityId, cardDef, predicate, projectedState)
            }
        }
    }

    /**
     * Count cards in a player's graveyard that match a filter.
     * Graveyard cards use base state (no continuous effects apply in graveyard).
     */
    private fun countGraveyardCardsMatchingFilter(state: GameState, playerId: EntityId, filter: GameObjectFilter): Int {
        return state.getGraveyard(playerId).count { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@count false
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return@count false
            filter.cardPredicates.all { predicate ->
                matchesGraveyardPredicate(cardDef, predicate)
            }
        }
    }

    /**
     * Count cards in a player's exile that match a filter.
     * Exile cards use base state (no continuous effects apply in exile).
     */
    private fun countExileCardsMatchingFilter(state: GameState, playerId: EntityId, filter: GameObjectFilter): Int {
        return state.getExile(playerId).count { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@count false
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return@count false
            filter.cardPredicates.all { predicate ->
                matchesGraveyardPredicate(cardDef, predicate)
            }
        }
    }

    /**
     * Match a graveyard card against a card predicate using base state.
     */
    private fun matchesGraveyardPredicate(
        cardDef: CardDefinition,
        predicate: CardPredicate
    ): Boolean {
        return when (predicate) {
            is CardPredicate.IsCreature -> cardDef.typeLine.isCreature
            is CardPredicate.IsArtifact -> cardDef.typeLine.isArtifact
            is CardPredicate.IsEnchantment -> cardDef.typeLine.isEnchantment
            is CardPredicate.IsLand -> cardDef.typeLine.isLand
            is CardPredicate.IsInstant -> cardDef.typeLine.isInstant
            is CardPredicate.IsSorcery -> cardDef.typeLine.isSorcery
            is CardPredicate.HasSubtype -> predicate.subtype in cardDef.typeLine.subtypes
            is CardPredicate.Or -> predicate.predicates.any { matchesGraveyardPredicate(cardDef, it) }
            is CardPredicate.And -> predicate.predicates.all { matchesGraveyardPredicate(cardDef, it) }
            is CardPredicate.Not -> !matchesGraveyardPredicate(cardDef, predicate.predicate)
            else -> false
        }
    }

    /**
     * Match a battlefield permanent against a card predicate.
     * Uses projected state when available for type/subtype checks.
     */
    private fun matchesBattlefieldPredicate(
        entityId: EntityId,
        cardDef: CardDefinition,
        predicate: CardPredicate,
        projectedState: com.wingedsheep.engine.mechanics.layers.ProjectedState?
    ): Boolean {
        return when (predicate) {
            is CardPredicate.IsCreature -> projectedState?.isCreature(entityId) ?: cardDef.typeLine.isCreature
            is CardPredicate.IsArtifact -> projectedState?.hasType(entityId, "ARTIFACT") ?: cardDef.typeLine.isArtifact
            is CardPredicate.IsEnchantment -> projectedState?.hasType(entityId, "ENCHANTMENT") ?: cardDef.typeLine.isEnchantment
            is CardPredicate.IsLand -> projectedState?.hasType(entityId, "LAND") ?: cardDef.typeLine.isLand
            is CardPredicate.HasSubtype -> projectedState?.hasSubtype(entityId, predicate.subtype.value) ?: (predicate.subtype in cardDef.typeLine.subtypes)
            else -> false // Only common predicates supported for cost reduction checks
        }
    }

    /**
     * Count permanents matching a filter that have a specific counter type.
     * Uses projected state for type/subtype checks.
     */
    private fun countPermanentsWithCounter(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        counterType: String
    ): Int {
        val projectedState = state.projectedState
        val ct = CounterType.entries.find { it.name.equals(counterType, ignoreCase = true) }
            ?: return 0
        return state.getBattlefield(playerId).count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            val card = container.get<CardComponent>() ?: return@count false
            val counters = container.get<CountersComponent>()
            if ((counters?.getCount(ct) ?: 0) <= 0) return@count false
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return@count false
            filter.cardPredicates.all { predicate ->
                matchesBattlefieldPredicate(entityId, cardDef, predicate, projectedState)
            }
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
     * Uses projected state for both controller and type (respects control-changing and type-changing effects).
     */
    private fun countPermanentsOfType(state: GameState, playerId: EntityId, cardType: CardType): Int {
        val projected = state.projectedState
        return state.controlledBattlefield(playerId).count { entityId ->
            projected.hasType(entityId, cardType.name)
        }
    }

    /**
     * Count permanents with a specific subtype controlled by a player.
     * Used for Affinity for subtypes (e.g., "Affinity for Lizards").
     * Uses projected state for both controller and subtype (respects control-changing and type-changing effects).
     */
    private fun countPermanentsWithSubtype(state: GameState, playerId: EntityId, subtype: Subtype): Int {
        val projected = state.projectedState
        return state.controlledBattlefield(playerId).count { entityId ->
            projected.hasSubtype(entityId, subtype.value)
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
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel

            for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
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
        val projectedState = state.projectedState

        var reduction = 0
        for (entityId in state.getBattlefield(casterId)) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel

            for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
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
     * Apply colored cost reductions from battlefield permanents.
     * Handles ReduceSpellColoredCostBySubtype and ReduceFirstSpellOfTypeColoredCost.
     */
    private fun applyColoredCostReductions(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId,
        currentCost: ManaCost
    ): ManaCost {
        val spellSubtypes = cardDef.typeLine.subtypes
        var cost = currentCost

        // Phase 1: ReduceSpellColoredCostBySubtype — no overflow (excess is dropped)
        if (spellSubtypes.isNotEmpty()) {
            val symbolsToRemove = mutableListOf<ManaSymbol>()
            for (entityId in state.getBattlefield(casterId)) {
                val container = state.getEntity(entityId) ?: continue
                val card = container.get<CardComponent>() ?: continue
                val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val classLevel = container.get<ClassLevelComponent>()?.currentLevel

                for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
                    if (ability is ReduceSpellColoredCostBySubtype &&
                        Subtype.of(ability.subtype) in spellSubtypes
                    ) {
                        val reductionCost = ManaCost.parse(ability.manaReduction)
                        symbolsToRemove.addAll(reductionCost.symbols.filterIsInstance<ManaSymbol.Colored>())
                    }
                }
            }
            if (symbolsToRemove.isNotEmpty()) {
                cost = reduceColoredCost(cost, symbolsToRemove)
            }
        }

        // Phase 2: ReduceFirstSpellOfTypeColoredCost — with overflow to generic
        val overflowSymbols = mutableListOf<ManaSymbol>()
        for (entityId in state.getBattlefield(casterId)) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel

            for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability is ReduceFirstSpellOfTypeColoredCost &&
                    matchesCardDefinition(cardDef, ability.spellFilter)
                ) {
                    val records = state.spellsCastThisTurnByPlayer[casterId] ?: emptyList()
                    val castCount = records.count { predicateEvaluator.matchesFilter(it, ability.spellFilter) }
                    if (castCount == 0) {
                        val units = evaluateReduction(state, ability.countSource, casterId)
                        val reductionSymbol = ManaCost.parse(ability.manaReductionPerUnit)
                        val coloredSymbols = reductionSymbol.symbols.filterIsInstance<ManaSymbol.Colored>()
                        repeat(units) {
                            overflowSymbols.addAll(coloredSymbols)
                        }
                    }
                }
            }
        }
        if (overflowSymbols.isNotEmpty()) {
            cost = reduceColoredCostWithOverflow(cost, overflowSymbols)
        }

        return cost
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
     * Remove colored mana symbols from a cost, with overflow to generic reduction.
     * First removes matching colored symbols; excess that can't match reduces generic mana.
     * Used for Eluge: "{U} less for each flood-counter land" — if more {U} reductions
     * than blue pips, excess reduces generic cost.
     */
    private fun reduceColoredCostWithOverflow(cost: ManaCost, symbolsToRemove: List<ManaSymbol>): ManaCost {
        val remainingSymbols = cost.symbols.toMutableList()
        var overflowReduction = 0

        for (symbolToRemove in symbolsToRemove) {
            val index = remainingSymbols.indexOfFirst { it == symbolToRemove }
            if (index >= 0) {
                remainingSymbols.removeAt(index)
            } else {
                // Colored symbol couldn't be removed — overflow to generic reduction
                overflowReduction++
            }
        }

        val result = ManaCost(remainingSymbols)
        return if (overflowReduction > 0) reduceGenericCost(result, overflowReduction) else result
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
            CardPredicate.IsNonenchantment -> !typeLine.isEnchantment
            CardPredicate.IsToken -> false
            CardPredicate.IsNontoken -> true
            CardPredicate.IsLegendary -> typeLine.isLegendary
            CardPredicate.IsNonlegendary -> !typeLine.isLegendary

            is CardPredicate.HasColor -> predicate.color in cardDef.colors
            is CardPredicate.NotColor -> predicate.color !in cardDef.colors
            CardPredicate.IsColorless -> cardDef.colors.isEmpty()
            CardPredicate.IsMulticolored -> cardDef.colors.size > 1
            CardPredicate.IsMonocolored -> cardDef.colors.size == 1

            is CardPredicate.HasSubtype -> typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.NotSubtype -> !typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.HasAnyOfSubtypes -> predicate.subtypes.any { typeLine.hasSubtype(it) }
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
            is CardPredicate.PowerOrToughnessAtLeast -> {
                val power = cardDef.creatureStats?.basePower ?: 0
                val toughness = cardDef.creatureStats?.baseToughness ?: 0
                power >= predicate.min || toughness >= predicate.min
            }
            is CardPredicate.TotalPowerAndToughnessAtMost -> {
                val power = cardDef.creatureStats?.basePower ?: 0
                val toughness = cardDef.creatureStats?.baseToughness ?: 0
                (power + toughness) <= predicate.max
            }

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
            CardPredicate.HasChosenSubtype -> true // Not applicable in cost calculation
            is CardPredicate.SharesCreatureTypeWith -> true // Not applicable in cost calculation

            // Context-relative predicates — not applicable in cost calculation (no pipeline context)
            is CardPredicate.HasSubtypeFromVariable -> true
            is CardPredicate.HasSubtypeInStoredList -> true
            is CardPredicate.HasSubtypeInEachStoredGroup -> true

            is CardPredicate.And -> predicate.predicates.all { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }
            is CardPredicate.Or -> predicate.predicates.any { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }
            is CardPredicate.Not -> !matchesCardPredicate(cardDef, predicate.predicate, sourceEntityId, state, projectedState)

            // Not applicable in cost calculation — abilities aren't cards
            CardPredicate.IsActivatedOrTriggeredAbility -> false
            CardPredicate.IsTriggeredAbility -> false
        }
    }

    /**
     * Reduce the generic mana cost by the specified amount.
     * Never reduces below 0 generic mana. Colored costs are preserved.
     */
    private fun reduceGenericCost(cost: ManaCost, reduction: Int): ManaCost {
        return cost.reduceGeneric(reduction)
    }

    /**
     * Calculate cost increase from global tax effects (IncreaseSpellCostByFilter,
     * IncreaseSpellCostByPlayerSpellsCast).
     * Scans ALL permanents on the battlefield since these are global effects
     * (e.g., Glowrider's "Noncreature spells cost {1} more to cast" affects all players).
     */
    private fun calculateFilterCostIncrease(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId? = null
    ): Int {
        var increase = 0
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val container = state.getEntity(entityId) ?: continue
                val card = container.get<CardComponent>() ?: continue
                val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val classLevel = container.get<ClassLevelComponent>()?.currentLevel

                for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
                    if (ability is IncreaseSpellCostByFilter &&
                        matchesCardDefinition(cardDef, ability.filter)
                    ) {
                        increase += ability.amount
                    }
                    if (ability is IncreaseSpellCostByPlayerSpellsCast && casterId != null) {
                        val spellsCast = state.playerSpellsCastThisTurn[casterId] ?: 0
                        increase += spellsCast * ability.amountPerSpell
                    }
                }
            }
        }
        return increase
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
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

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
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

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

    /**
     * Find alternative casting costs available to the caster from battlefield permanents.
     * Scans permanents controlled by the caster for GrantAlternativeCastingCost abilities.
     *
     * @return List of alternative ManaCosts available (may be empty)
     */
    fun findAlternativeCastingCosts(state: GameState, casterId: EntityId): List<ManaCost> {
        val costs = mutableListOf<ManaCost>()
        for (entityId in state.getBattlefield(casterId)) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel

            for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability is GrantAlternativeCastingCost) {
                    costs.add(ManaCost.parse(ability.cost))
                }
            }
        }
        return costs
    }

    /**
     * Calculate the effective cost of casting a spell using an alternative base cost.
     * Applies cost increases (tax effects) to the alternative cost.
     * Per Rule 118.9a, cost reductions and increases apply to alternative costs.
     *
     * Note: SpellCostReduction (self-reduction on the card) and Affinity are NOT applied
     * to alternative costs, since those modify the card's own mana cost. Only global
     * tax effects (IncreaseSpellCostByFilter) apply.
     */
    fun calculateEffectiveCostWithAlternativeBase(
        state: GameState,
        cardDef: CardDefinition,
        alternativeCost: ManaCost,
        casterId: EntityId? = null
    ): ManaCost {
        val totalIncrease = calculateFilterCostIncrease(state, cardDef, casterId)
        return increaseGenericCost(alternativeCost, totalIncrease)
    }

    companion object {
        /**
         * Check if a card has any cost reduction abilities.
         */
        fun hasCostReduction(cardDef: CardDefinition): Boolean {
            val hasSpellCostReduction = cardDef.script.staticAbilities.any { it is SpellCostReduction }
            val hasAffinity = cardDef.keywordAbilities.any {
                it is KeywordAbility.Affinity || it is KeywordAbility.AffinityForSubtype
            }
            return hasSpellCostReduction || hasAffinity
        }
    }
}
