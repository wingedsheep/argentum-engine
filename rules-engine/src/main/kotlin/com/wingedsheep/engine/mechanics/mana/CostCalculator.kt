package com.wingedsheep.engine.mechanics.mana
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.engine.state.components.battlefield.chosenColor

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantAlternativeCastingCost
import com.wingedsheep.sdk.scripting.MayCastWithoutPayingManaCost
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Calculates effective spell costs after applying cost reductions.
 *
 * All cost modifiers go through the unified [ModifySpellCost] static ability:
 * generic/colored reductions, dynamic reductions sourced from game state, global
 * tax effects, face-down (morph) cast cost adjustments, and morph (turn face-up)
 * activation cost adjustments.
 *
 * Cost reductions only reduce generic mana costs by default; colored modifications
 * are explicit (`CostModification.ReduceColored*`).
 * A spell's cost can never be reduced below its colored mana requirements.
 */
class CostCalculator(
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator(),
    private val conditionEvaluator: ConditionEvaluator = ConditionEvaluator()
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
        chosenTargets: List<EntityId> = emptyList(),
        fromZone: Zone? = null,
    ): ManaCost {
        var totalReduction = 0
        var totalIncrease = 0
        val coloredReductionSymbols = mutableListOf<ManaSymbol>()
        val coloredReductionWithOverflow = mutableListOf<ManaSymbol>()
        val coloredIncreaseSymbols = mutableListOf<ManaSymbol>()

        // Self-reductions read from the spell card's own static abilities.
        for (ability in cardDef.script.staticAbilities) {
            if (ability !is ModifySpellCost) continue
            if (ability.target != SpellCostTarget.SelfCast) continue
            if (!gatingApplies(state, casterId, cardDef, ability)) continue
            applyToSpellCast(
                state, cardDef, casterId, ability.modification, chosenTargets,
                addGenericReduction = { totalReduction += it },
                addGenericIncrease = { totalIncrease += it },
                addColoredReduction = { coloredReductionSymbols += it },
                addColoredReductionWithOverflow = { coloredReductionWithOverflow += it },
                addColoredIncrease = { coloredIncreaseSymbols += it },
            )
        }

        // Affinity keyword abilities (handled separately from ModifySpellCost).
        for (keywordAbility in cardDef.keywordAbilities) {
            if (keywordAbility is KeywordAbility.Affinity) {
                totalReduction += countPermanentsOfType(state, casterId, keywordAbility.forType)
            }
            if (keywordAbility is KeywordAbility.AffinityForSubtype) {
                totalReduction += countPermanentsWithSubtype(state, casterId, keywordAbility.forSubtype)
            }
        }

        // Battlefield-sourced ModifySpellCost abilities.
        for ((sourceId, ability) in scanBattlefieldModifySpellCost(state)) {
            if (!targetMatchesSpell(ability.target, cardDef, casterId, sourceId, state, chosenTargets, fromZone)) continue
            if (!gatingApplies(state, casterId, cardDef, ability)) continue
            applyToSpellCast(
                state, cardDef, casterId, ability.modification, chosenTargets,
                addGenericReduction = { totalReduction += it },
                addGenericIncrease = { totalIncrease += it },
                addColoredReduction = { coloredReductionSymbols += it },
                addColoredReductionWithOverflow = { coloredReductionWithOverflow += it },
                addColoredIncrease = { coloredIncreaseSymbols += it },
            )
        }

        // Commander tax (CR 903.8).
        totalIncrease += calculateCommanderTax(state, cardDef, casterId, fromZone)

        // CR 601.2f: the total cost is the base cost plus all cost increases, then minus all cost
        // reductions; the mana component is floored at {0} (it can't be reduced below {0}). Apply
        // increases first so a reduction that overshoots {0} doesn't leave a stale increase behind
        // (e.g. {U} +{1} −{2} → {U}, not {1}{U}).
        var effectiveCost = increaseGenericCost(cardDef.manaCost, totalIncrease)
        if (coloredIncreaseSymbols.isNotEmpty()) {
            effectiveCost = increaseColoredCost(effectiveCost, coloredIncreaseSymbols)
        }
        effectiveCost = reduceGenericCost(effectiveCost, totalReduction)
        if (coloredReductionSymbols.isNotEmpty()) {
            effectiveCost = reduceColoredCost(effectiveCost, coloredReductionSymbols)
        }
        if (coloredReductionWithOverflow.isNotEmpty()) {
            effectiveCost = reduceColoredCostWithOverflow(effectiveCost, coloredReductionWithOverflow)
        }
        return effectiveCost
    }

    /**
     * Compute commander tax for [cardDef] when cast from [fromZone].
     *
     * Returns 0 unless [fromZone] is `Zone.COMMAND` and the corresponding entity carries a
     * `CommanderComponent`. The tax is `2 * castsFromCommandZone` generic mana, applied alongside
     * other generic-cost increases. Locating the entity uses the deck-time `cardDefinitionId`
     * suffix (`Name#Set-CN`) plus the bare name, mirroring how `GameInitializer` builds the id.
     */
    private fun calculateCommanderTax(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId,
        fromZone: Zone?,
    ): Int {
        if (fromZone != Zone.COMMAND) return 0
        val commandZone = state.zones[
            com.wingedsheep.engine.state.ZoneKey(casterId, Zone.COMMAND),
        ] ?: return 0
        val match = commandZone.firstNotNullOfOrNull { entityId ->
            val container = state.getEntity(entityId) ?: return@firstNotNullOfOrNull null
            val card = container.get<CardComponent>() ?: return@firstNotNullOfOrNull null
            val commander = container.get<CommanderComponent>() ?: return@firstNotNullOfOrNull null
            if (commander.ownerId != casterId) return@firstNotNullOfOrNull null
            if (card.name != cardDef.name) return@firstNotNullOfOrNull null
            commander
        } ?: return 0
        return 2 * match.castsFromCommandZone
    }

    /**
     * Scan the battlefield for [ModifySpellCost] static abilities, returning each
     * (sourceEntityId, ability) pair. Class-level filtering via [ClassLevelComponent]
     * is honored.
     */
    private fun scanBattlefieldModifySpellCost(state: GameState): List<Pair<EntityId, ModifySpellCost>> {
        val results = mutableListOf<Pair<EntityId, ModifySpellCost>>()
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val container = state.getEntity(entityId) ?: continue
                val card = container.get<CardComponent>() ?: continue
                val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val classLevel = container.get<ClassLevelComponent>()?.currentLevel
                for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
                    if (ability is ModifySpellCost) {
                        results += entityId to ability
                    }
                }
            }
        }
        return results
    }

    /**
     * Whether [cardDef] (cast by [casterId]) is a target of [target] for the given
     * [sourceId] permanent on the battlefield (or null for self-cast).
     */
    private fun targetMatchesSpell(
        target: SpellCostTarget,
        cardDef: CardDefinition,
        casterId: EntityId,
        sourceId: EntityId,
        state: GameState,
        chosenTargets: List<EntityId> = emptyList(),
        fromZone: Zone? = null,
    ): Boolean {
        return when (target) {
            SpellCostTarget.SelfCast -> false
            is SpellCostTarget.YouCast -> {
                val controller = state.projectedState.getController(sourceId)
                controller == casterId &&
                    matchesCardDefinition(cardDef, target.filter, sourceId, state, state.projectedState)
            }
            is SpellCostTarget.AnyCaster -> matchesCardDefinition(cardDef, target.filter, sourceId, state, state.projectedState)
            is SpellCostTarget.OpponentsCastTargeting ->
                opponentsCastTargetingMatches(state, casterId, sourceId, target.targetFilter, chosenTargets)
            is SpellCostTarget.OpponentsCastFromZones -> {
                // Source must be controlled by an opponent of the caster, the spell must be cast
                // from one of the named zones, and the card must match the filter.
                if (fromZone == null || fromZone !in target.zones) return false
                val sourceController = state.projectedState.getController(sourceId) ?: return false
                if (sourceController == casterId) return false
                matchesCardDefinition(cardDef, target.filter, sourceId, state, state.projectedState)
            }
            is SpellCostTarget.YouCastFromZones -> {
                // Source must be controlled by the caster, the spell must be cast from one of the
                // named zones, and the card must match the filter (Doc Aurlock).
                if (fromZone == null || fromZone !in target.zones) return false
                val sourceController = state.projectedState.getController(sourceId) ?: return false
                if (sourceController != casterId) return false
                matchesCardDefinition(cardDef, target.filter, sourceId, state, state.projectedState)
            }
            // FaceDown / Morph targets are spell-cast modifiers only when applied to a face-down cast.
            // calculateEffectiveCost is only called for face-up casts; face-down handling is in
            // calculateFaceDownCost / calculateMorphCostIncrease.
            SpellCostTarget.FaceDownYouCast -> false
            SpellCostTarget.MorphActivation -> false
        }
    }

    /**
     * True iff the source is controlled by an opponent of the caster, and at least
     * one of the caster's chosen targets matches [targetFilter] relative to the
     * source. Implements the matching half of [SpellCostTarget.OpponentsCastTargeting]
     * (e.g. Terror of the Peaks' "this creature", Kopala's "Merfolk you control").
     */
    private fun opponentsCastTargetingMatches(
        state: GameState,
        casterId: EntityId,
        sourceId: EntityId,
        targetFilter: GroupFilter,
        chosenTargets: List<EntityId>,
    ): Boolean {
        if (chosenTargets.isEmpty()) return false
        val sourceController = state.projectedState.getController(sourceId) ?: return false
        if (sourceController == casterId) return false
        val context = PredicateContext(controllerId = sourceController, sourceId = sourceId)
        val projected = state.projectedState
        return chosenTargets.any { targetId ->
            when (val scope = targetFilter.scope) {
                is Scope.Self -> targetId == sourceId
                is Scope.Specific -> targetId == scope.entityId
                is Scope.AttachedTo -> {
                    val attached = state.getEntity(sourceId)
                        ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                        ?.targetId
                    attached != null && targetId == attached
                }
                is Scope.Battlefield ->
                    predicateEvaluator.matches(state, projected, targetId, targetFilter.baseFilter, context)
            }
        }
    }

    /**
     * Whether the [ability]'s gating allows it to apply to a cast of [cardDef] by [casterId].
     */
    private fun gatingApplies(
        state: GameState,
        casterId: EntityId,
        cardDef: CardDefinition,
        ability: ModifySpellCost,
    ): Boolean {
        return when (val gating = ability.gating) {
            CostGating.None -> true
            is CostGating.NthOfTypePerTurn -> {
                val filter = filterForGating(ability.target) ?: return false
                val records = state.spellsCastThisTurnByPlayer[casterId] ?: emptyList()
                val priorMatching = records.count { predicateEvaluator.matchesFilter(it, filter) }
                priorMatching == gating.n - 1
            }
            is CostGating.OnlyIf -> {
                // Player-scoped conditions ("during your turn", "you've cast another spell", ...)
                // evaluate against the caster. The cost modifier's source is a non-spell permanent
                // (or the spell card itself for SelfCast), neither of which the condition needs.
                val ctx = EffectContext(sourceId = null, controllerId = casterId)
                conditionEvaluator.evaluate(state, gating.condition, ctx)
            }
        }
    }

    private fun filterForGating(target: SpellCostTarget) = when (target) {
        is SpellCostTarget.YouCast -> target.filter
        is SpellCostTarget.AnyCaster -> target.filter
        else -> null  // Gating requires a filter to know what "of type" means.
    }

    /**
     * Apply a single cost modification to the running totals for a spell cast.
     */
    private fun applyToSpellCast(
        state: GameState,
        cardDef: CardDefinition,
        casterId: EntityId,
        modification: CostModification,
        chosenTargets: List<EntityId>,
        addGenericReduction: (Int) -> Unit,
        addGenericIncrease: (Int) -> Unit,
        addColoredReduction: (ManaSymbol) -> Unit,
        addColoredReductionWithOverflow: (ManaSymbol) -> Unit,
        addColoredIncrease: (ManaSymbol) -> Unit,
    ) {
        when (modification) {
            is CostModification.ReduceGeneric -> addGenericReduction(modification.amount)
            is CostModification.ReduceGenericBy ->
                addGenericReduction(evaluateReduction(state, modification.source, casterId, chosenTargets))
            is CostModification.ReduceColored -> {
                ManaCost.parse(modification.symbols).symbols
                    .filterIsInstance<ManaSymbol.Colored>()
                    .forEach(addColoredReduction)
            }
            is CostModification.ReduceColoredPerUnit -> {
                val units = evaluateReduction(state, modification.countSource, casterId, chosenTargets)
                val coloredSymbols = ManaCost.parse(modification.symbols).symbols
                    .filterIsInstance<ManaSymbol.Colored>()
                repeat(units) { coloredSymbols.forEach(addColoredReductionWithOverflow) }
            }
            is CostModification.ReduceColoredIfAnyTargetMatches -> {
                if (chosenTargets.isNotEmpty() &&
                    anyTargetMatchesFilter(state, casterId, chosenTargets, modification.filter)
                ) {
                    ManaCost.parse(modification.symbols).symbols
                        .filterIsInstance<ManaSymbol.Colored>()
                        .forEach(addColoredReduction)
                }
            }
            is CostModification.IncreaseGeneric -> addGenericIncrease(modification.amount)
            is CostModification.IncreaseColored -> {
                ManaCost.parse(modification.symbols).symbols
                    .filterIsInstance<ManaSymbol.Colored>()
                    .forEach(addColoredIncrease)
            }
            is CostModification.IncreaseGenericPerOtherSpellThisTurn -> {
                val spellsCast = state.playerSpellsCastThisTurn[casterId] ?: 0
                addGenericIncrease(spellsCast * modification.amountPerSpell)
            }
            is CostModification.IncreaseGenericIfAnyTargetMatches -> {
                if (chosenTargets.isNotEmpty() &&
                    anyTargetMatchesFilter(state, casterId, chosenTargets, modification.filter)
                ) {
                    addGenericIncrease(modification.amount)
                }
            }
            is CostModification.IncreaseLife -> {
                // Life is not part of the mana cost — collected separately via
                // [calculateAdditionalLifeCost] and paid alongside mana in CastSpellHandler.
            }
        }
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
            is CostReductionSource.GreatestManaValueAmongPermanentsYouControl -> {
                greatestManaValueAmongMatching(state, playerId, source.filter)
            }
            is CostReductionSource.FixedIfVoid -> {
                if (state.nonlandPermanentLeftBattlefieldThisTurn || state.spellWarpedThisTurn)
                    source.amount
                else 0
            }
            is CostReductionSource.PermanentsYouControlMatching ->
                countControlledPermanentsMatching(state, playerId, source.filter)
            is CostReductionSource.DifferentlyNamedPermanentsYouControl ->
                countDifferentlyNamedPermanents(state, playerId, source.filter)
            is CostReductionSource.PermanentsOnBattlefieldMatching ->
                countBattlefieldPermanentsMatching(state, playerId, source.filter)
        }
    }

    /**
     * Count permanents the player controls matching the filter. Iterates the projected-control
     * view (`controlledBattlefield`) so control-changing effects are honored, and evaluates the
     * filter through [PredicateEvaluator] against projected state so type/power/subtype checks
     * see buffs, counters, and lords (CR 613). The "you control" analogue of
     * [countBattlefieldPermanentsMatching].
     */
    private fun countControlledPermanentsMatching(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): Int {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        return state.controlledBattlefield(playerId).count { entityId ->
            predicateEvaluator.matches(state, projected, entityId, filter, context)
        }
    }

    /**
     * Count permanents on the battlefield (across all players) matching the filter.
     * Uses projected state for type/subtype checks to honor continuous effects.
     */
    private fun countBattlefieldPermanentsMatching(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): Int {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        return state.getBattlefield().count { entityId ->
            predicateEvaluator.matches(state, projected, entityId, filter, context)
        }
    }

    /**
     * Find the greatest mana value among permanents the player controls matching a filter.
     * Returns 0 if none match. Mana value is read from CardDefinition.manaCost.cmc;
     * X-costs contribute X = 0 per Rule 202.3b for permanents on the battlefield.
     */
    private fun greatestManaValueAmongMatching(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): Int {
        val projectedState = state.projectedState
        var maxMv = 0
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val matches = filter.cardPredicates.all { predicate ->
                matchesBattlefieldPredicate(entityId, cardDef, predicate, projectedState)
            }
            if (matches) {
                val mv = cardDef.manaCost.cmc
                if (mv > maxMv) maxMv = mv
            }
        }
        return maxMv
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
            if (ability !is ModifySpellCost) continue
            if (ability.target != SpellCostTarget.SelfCast) continue
            val src = sourceFromModification(ability.modification)
            if (src is CostReductionSource.FixedIfAnyTargetMatches) {
                val match = findAnyBattlefieldMatch(state, casterId, src.filter)
                if (match != null) optimisticTargets += match
            }
        }
        return calculateEffectiveCost(state, cardDef, casterId, optimisticTargets)
    }

    private fun sourceFromModification(modification: CostModification): CostReductionSource? = when (modification) {
        is CostModification.ReduceGenericBy -> modification.source
        is CostModification.ReduceColoredPerUnit -> modification.countSource
        else -> null
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
            predicateEvaluator.matches(state, projected, entityId, filter, context)
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
            if (predicateEvaluator.matches(state, projected, entityId, filter, context)) {
                return entityId
            }
        }
        return null
    }

    /**
     * Count creatures controlled by a player.
     */
    private fun countCreatures(state: GameState, playerId: EntityId): Int {
        val projected = state.projectedState
        return state.getBattlefield(playerId).count { entityId -> projected.isCreature(entityId) }
    }

    /**
     * Sum the power of all creatures controlled by a player.
     * Uses projected state if available to account for continuous effects.
     */
    private fun sumPower(state: GameState, playerId: EntityId): Int {
        val projectedState = state.projectedState

        var totalPower = 0
        for (entityId in state.getBattlefield(playerId)) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue

            if (!projectedState.isCreature(entityId)) continue

            val basePower: Int = when (val p = card.baseStats?.power) {
                is CharacteristicValue.Fixed -> p.value
                is CharacteristicValue.Dynamic -> 0
                is CharacteristicValue.DynamicWithOffset -> p.offset
                null -> 0
            }
            val projectedPower: Int? = projectedState.getPower(entityId)
            val power: Int = (projectedPower ?: basePower).coerceAtLeast(0)

            totalPower += power
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
            is CardPredicate.IsPermanent -> cardDef.typeLine.isPermanent
            is CardPredicate.HasSubtype -> projectedState?.hasSubtype(entityId, predicate.subtype.value) ?: (predicate.subtype in cardDef.typeLine.subtypes)
            else -> false
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
     *
     * Uses projected state for both controller and color so that:
     * - Control-changing effects (Mind Control, Threaten) are honored — the rule reads
     *   "permanents you control", which is the projected controller (CR 613.1c).
     * - Color-changing effects (Foraging Wickermaw's "becomes that color until end of turn",
     *   Painter's Servant, Moonlace, etc.) contribute their granted colors, since the rule
     *   reads "colors among permanents".
     */
    private fun countColors(state: GameState, playerId: EntityId): Int {
        val projected = state.projectedState
        val colors = mutableSetOf<String>()

        state.controlledBattlefield(playerId).forEach { entityId ->
            colors.addAll(projected.getColors(entityId))
        }

        return colors.size
    }

    private fun countDifferentlyNamedPermanents(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): Int {
        val projectedState = state.projectedState
        val names = mutableSetOf<String>()
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val matches = filter.cardPredicates.all { predicate ->
                matchesBattlefieldPredicate(entityId, cardDef, predicate, projectedState)
            }
            if (matches) names.add(card.name)
        }
        return names.size
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
     * Add specific colored mana symbols to a mana cost (colored tax effect).
     * Each symbol in [symbolsToAdd] appends one matching colored pip. Does not affect generic mana.
     */
    private fun increaseColoredCost(cost: ManaCost, symbolsToAdd: List<ManaSymbol>): ManaCost {
        if (symbolsToAdd.isEmpty()) return cost
        return ManaCost(cost.symbols + symbolsToAdd)
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
        if (filter.cardPredicates.isEmpty() && filter.anyOf.isEmpty()) return true
        // Conjunction over card predicates; OR lives inside a CardPredicate.Or.
        if (!filter.cardPredicates.all { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }) return false
        // Recursive union (`or` infix): match if any branch matches.
        if (filter.anyOf.isNotEmpty()) {
            return filter.anyOf.any { matchesCardDefinition(cardDef, it, sourceEntityId, state, projectedState) }
        }
        return true
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
            CardPredicate.IsNonartifact -> !typeLine.isArtifact
            CardPredicate.IsToken -> false
            CardPredicate.IsNontoken -> true
            CardPredicate.IsLegendary -> typeLine.isLegendary
            CardPredicate.IsNonlegendary -> !typeLine.isLegendary
            CardPredicate.HasNonManaActivatedAbility -> cardDef.hasNonManaActivatedAbility

            is CardPredicate.HasColor -> predicate.color in cardDef.colors
            is CardPredicate.NotColor -> predicate.color !in cardDef.colors
            // CostCalculator has no resolution-time chosen color; no static answer here.
            CardPredicate.HasChosenColor -> false
            CardPredicate.IsColorless -> cardDef.colors.isEmpty()
            CardPredicate.IsColored -> cardDef.colors.isNotEmpty()
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
            // CostCalculator has no chosen-number context; predicate has no static answer here.
            CardPredicate.ManaValueAtMostX -> false
            CardPredicate.ManaValueEqualsX -> false
            is CardPredicate.ManaValueAtLeast -> cardDef.manaCost.cmc >= predicate.min
            // CostCalculator has no entity context; predicate has no static answer here.
            is CardPredicate.ManaValueAtMostEntity -> false
            is CardPredicate.ManaValueAtMostEntityManaSpent -> false
            is CardPredicate.ManaValueAtMostColorsSpent -> false
            is CardPredicate.PowerGreaterThanEntity -> false
            is CardPredicate.PowerAtMostEntity -> false
            CardPredicate.ManaValueIsEven -> cardDef.manaCost.cmc % 2 == 0
            CardPredicate.ManaValueIsOdd -> cardDef.manaCost.cmc % 2 != 0

            is CardPredicate.PowerEquals -> cardDef.creatureStats?.basePower == predicate.value
            is CardPredicate.PowerAtMost -> (cardDef.creatureStats?.basePower ?: 0) <= predicate.max
            is CardPredicate.PowerAtLeast -> (cardDef.creatureStats?.basePower ?: 0) >= predicate.min
            is CardPredicate.ToughnessEquals -> cardDef.creatureStats?.baseToughness == predicate.value
            is CardPredicate.ToughnessAtMost -> (cardDef.creatureStats?.baseToughness ?: 0) <= predicate.max
            // CostCalculator has no X context; predicate has no static answer here.
            CardPredicate.ToughnessAtMostX -> false
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
            CardPredicate.ToughnessGreaterThanPower -> {
                val power = cardDef.creatureStats?.basePower ?: 0
                val toughness = cardDef.creatureStats?.baseToughness ?: 0
                toughness > power
            }

            CardPredicate.NotOfSourceChosenType -> true

            CardPredicate.SharesCreatureTypeWithSource -> {
                if (sourceEntityId == null) return true
                val spellSubtypes = cardDef.typeLine.subtypes
                if (spellSubtypes.isEmpty()) return false
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

            CardPredicate.SharesCreatureTypeWithTriggeringEntity -> true
            CardPredicate.HasChosenSubtype -> {
                if (sourceEntityId == null || state == null) return false
                val chosenType = state.getEntity(sourceEntityId)
                    ?.chosenCreatureType()
                    ?: return false
                val spellSubtypes = cardDef.typeLine.subtypes.map { it.value }
                spellSubtypes.any { it.equals(chosenType, ignoreCase = true) } ||
                    (com.wingedsheep.sdk.core.Keyword.CHANGELING in cardDef.keywords &&
                        chosenType in Subtype.ALL_CREATURE_TYPES)
            }
            is CardPredicate.SharesCreatureTypeWith -> true
            is CardPredicate.SharesColorWith -> true
            CardPredicate.SharesColorWithRecipient -> false
            CardPredicate.SharesChosenColorWithSource -> {
                if (sourceEntityId == null || state == null) return false
                val chosenColor = state.getEntity(sourceEntityId)
                    ?.chosenColor()
                    ?: return false
                cardDef.colors.contains(chosenColor)
            }

            is CardPredicate.HasSubtypeFromVariable -> true
            is CardPredicate.HasSubtypeInStoredList -> true
            is CardPredicate.HasSubtypeInEachStoredGroup -> true
            is CardPredicate.NameEqualsChosen -> true

            is CardPredicate.And -> predicate.predicates.all { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }
            is CardPredicate.Or -> predicate.predicates.any { matchesCardPredicate(cardDef, it, sourceEntityId, state, projectedState) }
            is CardPredicate.Not -> !matchesCardPredicate(cardDef, predicate.predicate, sourceEntityId, state, projectedState)

            CardPredicate.IsActivatedOrTriggeredAbility -> false
            CardPredicate.IsTriggeredAbility -> false
            CardPredicate.IsActivatedAbility -> false
            is CardPredicate.TargetsMatching -> false
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
     * Calculate the effective cost of casting a face-down creature spell (morph).
     * The base cost is {3}, reduced by any [ModifySpellCost] with target
     * [SpellCostTarget.FaceDownYouCast] on permanents the caster controls.
     */
    fun calculateFaceDownCost(state: GameState, casterId: EntityId): ManaCost {
        val baseMorphCost = ManaCost.parse("{3}")
        var totalReduction = 0

        for ((sourceId, ability) in scanBattlefieldModifySpellCost(state)) {
            if (ability.target != SpellCostTarget.FaceDownYouCast) continue
            if (state.projectedState.getController(sourceId) != casterId) continue
            when (val mod = ability.modification) {
                is CostModification.ReduceGeneric -> totalReduction += mod.amount
                is CostModification.ReduceGenericBy ->
                    totalReduction += evaluateReduction(state, mod.source, casterId)
                else -> { /* face-down only supports generic reduction */ }
            }
        }

        return reduceGenericCost(baseMorphCost, totalReduction)
    }

    /**
     * Calculate the total morph cost increase from all [ModifySpellCost] abilities
     * on the battlefield with target [SpellCostTarget.MorphActivation]. This affects
     * all players globally (turn-face-up activated cost).
     */
    fun calculateMorphCostIncrease(state: GameState): Int {
        var totalIncrease = 0
        for ((_, ability) in scanBattlefieldModifySpellCost(state)) {
            if (ability.target != SpellCostTarget.MorphActivation) continue
            when (val mod = ability.modification) {
                is CostModification.IncreaseGeneric -> totalIncrease += mod.amount
                else -> { /* morph-activation only supports generic increase */ }
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
     * Whether any battlefield permanent is currently granting [casterId] a
     * [MayCastWithoutPayingManaCost] permission whose gates are all open
     * (e.g. Weftwalking's `firstSpellOfTurnOnly = true`).
     *
     * Gates honored per source:
     *  - `controllerOnly` — when true, the source's controller (read from projected state) must
     *    be [casterId].
     *  - `firstSpellOfTurnOnly` — when true, [casterId] must be the active player and must have
     *    cast no spells yet this turn.
     *
     * Scans every battlefield zone (not just the caster's) so that an opponent's source can
     * still grant the caster their free cast when the source's wording doesn't require
     * controller-only.
     */
    fun hasFreeCastPermission(
        state: GameState,
        casterId: EntityId,
        spellCardDef: CardDefinition? = null
    ): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val permanentDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in permanentDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability !is MayCastWithoutPayingManaCost) continue
                if (ability.controllerOnly) {
                    val controllerId = state.projectedState.getController(entityId) ?: continue
                    if (controllerId != casterId) continue
                }
                if (ability.firstSpellOfTurnOnly) {
                    if (state.activePlayerId != casterId) continue
                    if ((state.playerSpellsCastThisTurn[casterId] ?: 0) > 0) continue
                }
                // The permission may be scoped to a spell type (e.g. Dracogenesis — Dragons only).
                // When the caller knows the spell being cast, enforce the filter; with no spell in
                // hand (a generic "does any free-cast source exist?" probe) a filtered source still
                // counts, since some castable spell may match.
                if (spellCardDef != null && ability.spellFilter != GameObjectFilter.Any &&
                    !matchesCardDefinition(spellCardDef, ability.spellFilter, entityId, state, state.projectedState)
                ) continue
                return true
            }
        }
        return false
    }

    /**
     * Calculate the effective cost of casting a spell using an alternative base cost.
     * Applies cost increases (tax effects) to the alternative cost.
     * Per Rule 118.9a, cost reductions and increases apply to alternative costs.
     *
     * Note: Self-reduction (`SpellCostTarget.SelfCast`) and Affinity are NOT applied to
     * alternative costs, since those modify the card's own mana cost. Only
     * battlefield-sourced AnyCaster increases apply.
     */
    fun calculateEffectiveCostWithAlternativeBase(
        state: GameState,
        cardDef: CardDefinition,
        alternativeCost: ManaCost,
        casterId: EntityId? = null
    ): ManaCost {
        var totalIncrease = 0
        for ((sourceId, ability) in scanBattlefieldModifySpellCost(state)) {
            val target = ability.target
            if (target !is SpellCostTarget.AnyCaster) continue
            if (!matchesCardDefinition(cardDef, target.filter, sourceId, state, state.projectedState)) continue
            when (val mod = ability.modification) {
                is CostModification.IncreaseGeneric -> totalIncrease += mod.amount
                is CostModification.IncreaseGenericPerOtherSpellThisTurn -> {
                    if (casterId != null) {
                        val spellsCast = state.playerSpellsCastThisTurn[casterId] ?: 0
                        totalIncrease += spellsCast * mod.amountPerSpell
                    }
                }
                else -> { /* AnyCaster reductions don't apply to alternative casting costs. */ }
            }
        }
        return increaseGenericCost(alternativeCost, totalIncrease)
    }

    /**
     * Calculate the additional life [casterId] must pay as part of casting a spell with
     * the given [targets], summed across every battlefield [ModifySpellCost] ability
     * matching [SpellCostTarget.OpponentsCastTargeting] + [CostModification.IncreaseLife]
     * (e.g. Terror of the Peaks).
     *
     * Called from [CastSpellHandler] to validate affordability and collect payment.
     */
    fun calculateAdditionalLifeCost(
        state: GameState,
        casterId: EntityId,
        targets: List<ChosenTarget>
    ): Int {
        if (targets.isEmpty()) return 0
        val targetEntityIds = targets.mapNotNull { (it as? ChosenTarget.Permanent)?.entityId }
        if (targetEntityIds.isEmpty()) return 0
        var total = 0
        for ((sourceId, ability) in scanBattlefieldModifySpellCost(state)) {
            val target = ability.target as? SpellCostTarget.OpponentsCastTargeting ?: continue
            val modification = ability.modification as? CostModification.IncreaseLife ?: continue
            if (!opponentsCastTargetingMatches(state, casterId, sourceId, target.targetFilter, targetEntityIds)) continue
            total += modification.amount
        }
        return total
    }

    companion object {
        /**
         * Check if a card has any cost reduction abilities.
         */
        fun hasCostReduction(cardDef: CardDefinition): Boolean {
            val hasSelfReduction = cardDef.script.staticAbilities.any { ability ->
                ability is ModifySpellCost && ability.target == SpellCostTarget.SelfCast
            }
            val hasAffinity = cardDef.keywordAbilities.any {
                it is KeywordAbility.Affinity || it is KeywordAbility.AffinityForSubtype
            }
            return hasSelfReduction || hasAffinity
        }
    }
}
