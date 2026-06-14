package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.ConvokeCreatureData
import com.wingedsheep.engine.legalactions.CounterRemovalCreatureData
import com.wingedsheep.engine.legalactions.DelveCardData
import com.wingedsheep.engine.legalactions.HarmonizeCreatureData
import com.wingedsheep.engine.legalactions.WaterbendPermanentData
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSource
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Extracted cost-checking helpers from LegalActionsCalculator.
 * These methods check cost payability, find sacrifice/tap/exile targets, etc.
 */
class CostEnumerationUtils(
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val predicateEvaluator: PredicateEvaluator,
    private val cardRegistry: CardRegistry
) {
    // --- Sacrifice targets ---

    fun findSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        cost: CostAtom.Sacrifice
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matches(state, projected, entityId, cost.filter, predicateContext)
        }
    }

    fun findVariableSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter, predicateContext)
        }
    }

    fun findAbilitySacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId? = null
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter, predicateContext)
        }
    }

    // --- Tap targets ---

    fun findAbilityTapTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            if (container.has<TappedComponent>()) return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter, predicateContext)
        }
    }

    // --- Bounce targets ---

    fun findAbilityBounceTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter, predicateContext)
        }
    }

    // --- Exile targets ---

    fun findExileTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        zone: Zone
    ): List<EntityId> {
        val zoneKey = ZoneKey(playerId, zone)
        val predicateContext = PredicateContext(controllerId = playerId)
        return state.getZone(zoneKey).filter { entityId ->
            predicateEvaluator.matches(state, state.projectedState, entityId, filter, predicateContext)
        }
    }

    /**
     * Find cards in the player's hand matching the filter — used for discard costs on
     * activated abilities (e.g., "Discard a land card: ...").
     */
    fun findDiscardTargets(state: GameState, playerId: EntityId, filter: GameObjectFilter): List<EntityId> {
        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)
        val predicateContext = PredicateContext(controllerId = playerId)
        return hand.filter { predicateEvaluator.matches(state, state.projectedState, it, filter, predicateContext) }
    }

    // --- Morph cost targets ---

    fun findMorphDiscardTargets(state: GameState, playerId: EntityId, filter: GameObjectFilter): List<EntityId> =
        findDiscardTargets(state, playerId, filter)

    fun findMorphRevealTargets(state: GameState, playerId: EntityId, filter: GameObjectFilter): List<EntityId> {
        return findMorphDiscardTargets(state, playerId, filter) // Same logic
    }

    fun findMorphSacrificeTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter, predicateContext)
        }
    }

    fun findMorphExileTargets(state: GameState, playerId: EntityId, filter: GameObjectFilter, zone: Zone): List<EntityId> {
        val zoneKey = ZoneKey(playerId, zone)
        val cards = state.getZone(zoneKey)
        val predicateContext = PredicateContext(controllerId = playerId)
        return cards.filter { predicateEvaluator.matches(state, state.projectedState, it, filter, predicateContext) }
    }

    fun findReturnToHandTargets(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter,
        excludeEntityId: EntityId
    ): List<EntityId> {
        val predicateContext = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            if (entityId == excludeEntityId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            predicateEvaluator.matches(state, projected, entityId, filter, predicateContext)
        }
    }

    // --- Convoke ---

    fun findConvokeCreatures(state: GameState, playerId: EntityId): List<ConvokeCreatureData> {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null
            if (!projected.isCreature(entityId)) return@mapNotNull null
            if (container.has<TappedComponent>()) return@mapNotNull null
            ConvokeCreatureData(entityId, cardComponent.name, cardComponent.colors)
        }
    }

    fun canAffordWithConvoke(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        convokeCreatures: List<ConvokeCreatureData>,
        precomputedSources: List<ManaSource>? = null
    ): Boolean {
        // Convoke creatures with their own mana abilities (e.g. Llanowar Elves) appear in
        // both lists — but tapping the creature can pay either a convoke pip or a mana
        // ability, never both. Exclude them from the mana-source count so the totals
        // don't double-up.
        val convokeIds = convokeCreatures.mapTo(mutableSetOf()) { it.entityId }
        val sourcesForMana = (precomputedSources ?: manaSolver.findAvailableManaSources(state, playerId))
            .filter { it.entityId !in convokeIds }
        val availableMana = manaSolver.getAvailableManaCount(state, playerId, sourcesForMana)
        val totalResources = availableMana + convokeCreatures.size
        if (totalResources < manaCost.cmc) return false

        val coloredRequirements = manaCost.colorCount
        val creatureColors = mutableMapOf<Color, Int>()
        for (creature in convokeCreatures) {
            for (color in creature.colors) {
                creatureColors[color] = (creatureColors[color] ?: 0) + 1
            }
        }
        var creaturesUsedForColors = 0
        for ((color, needed) in coloredRequirements) {
            val creaturesOfColor = creatureColors[color] ?: 0
            creaturesUsedForColors += minOf(needed, creaturesOfColor)
        }
        val genericRequired = manaCost.genericAmount
        val creaturesForGeneric = convokeCreatures.size - creaturesUsedForColors
        val resourcesForGeneric = availableMana + creaturesForGeneric
        return resourcesForGeneric >= genericRequired
    }

    // --- Waterbend ---

    /**
     * Untapped artifacts/creatures the player controls that may be tapped for a Waterbend cost.
     * Projected types are used so animated lands / type-changed permanents are honored.
     */
    fun findWaterbendPermanents(state: GameState, playerId: EntityId): List<WaterbendPermanentData> {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null
            val isCreature = projected.isCreature(entityId)
            val isArtifact = projected.hasType(entityId, "ARTIFACT")
            if (!isCreature && !isArtifact) return@mapNotNull null
            if (container.has<TappedComponent>()) return@mapNotNull null
            WaterbendPermanentData(entityId, cardComponent.name, isCreature)
        }
    }

    /**
     * Whether [manaCost] is payable with the help of waterbend taps. Each tapped permanent pays
     * exactly {1} generic, so colored pips must come from mana sources; the generic portion may
     * be covered by mana sources and/or waterbend permanents. A permanent tapped for waterbend
     * can't also be a mana source, so any that double as mana sources are excluded from the mana
     * count.
     */
    fun canAffordWithWaterbend(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        waterbendPermanents: List<WaterbendPermanentData>,
        precomputedSources: List<ManaSource>? = null
    ): Boolean {
        val waterbendIds = waterbendPermanents.mapTo(mutableSetOf()) { it.entityId }
        val sourcesForMana = (precomputedSources ?: manaSolver.findAvailableManaSources(state, playerId))
            .filter { it.entityId !in waterbendIds }
        val availableMana = manaSolver.getAvailableManaCount(state, playerId, sourcesForMana)

        // Colored pips can only be paid by mana — waterbend is generic-only.
        val coloredRequired = manaCost.colorCount.values.sum()
        if (availableMana < coloredRequired) return false

        val genericRequired = manaCost.genericAmount
        val manaLeftForGeneric = availableMana - coloredRequired
        val resourcesForGeneric = manaLeftForGeneric + waterbendPermanents.size
        return resourcesForGeneric >= genericRequired
    }

    // --- Harmonize ---

    /**
     * Untapped creatures the player controls that may be tapped for Harmonize. [power] is
     * the projected power (the generic-mana reduction tapping the creature would grant).
     */
    fun findHarmonizeCreatures(state: GameState, playerId: EntityId): List<HarmonizeCreatureData> {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null
            if (!projected.isCreature(entityId)) return@mapNotNull null
            if (container.has<TappedComponent>()) return@mapNotNull null
            val power = (projected.getPower(entityId) ?: 0).coerceAtLeast(0)
            HarmonizeCreatureData(entityId, cardComponent.name, power)
        }
    }

    /**
     * Can the player pay [manaCost] either as-is or after tapping a single Harmonize creature
     * (reducing the generic portion by its power)? A creature tapped for Harmonize can't also
     * tap for mana, so it is excluded from the mana sources when evaluating its reduction.
     */
    fun canAffordWithHarmonize(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        harmonizeCreatures: List<HarmonizeCreatureData>,
        precomputedSources: List<ManaSource>? = null
    ): Boolean {
        if (manaSolver.canPay(state, playerId, manaCost, precomputedSources = precomputedSources)) return true
        val generic = manaCost.genericAmount
        for (creature in harmonizeCreatures) {
            val reduction = minOf(creature.power, generic)
            if (reduction <= 0) continue
            val reducedCost = manaCost.reduceGeneric(reduction)
            val sourcesForMana = (precomputedSources ?: manaSolver.findAvailableManaSources(state, playerId))
                .filter { it.entityId != creature.entityId }
            if (manaSolver.canPay(state, playerId, reducedCost, precomputedSources = sourcesForMana)) return true
        }
        return false
    }

    /**
     * Optimistic maximum X for an X-cost Harmonize spell, considering the best single
     * creature the player could tap. Tapping reduces generic mana by the creature's power,
     * and {X} is generic (TDM release notes), so a tap raises the affordable X. The best of
     * {no tap, each creature} is returned — consistent with [canAffordWithHarmonize]'s
     * affordability (which also assumes the tap). Count-based like the other `maxAffordableX`
     * computations: it doesn't verify color feasibility, so it may slightly over-estimate;
     * the harmonize-creature phase, mana selection, and server validation gate the real cast.
     */
    fun maxAffordableXWithHarmonize(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        harmonizeCreatures: List<HarmonizeCreatureData>,
        precomputedSources: List<ManaSource>? = null
    ): Int {
        if (!manaCost.hasX) return 0
        val xCount = manaCost.xCount.coerceAtLeast(1)
        val fixedCost = manaCost.cmc // colored + printed generic; {X} contributes 0
        val sources = precomputedSources ?: manaSolver.findAvailableManaSources(state, playerId)

        fun maxXFor(reduction: Int, excludeId: EntityId?): Int {
            val usable = if (excludeId == null) sources else sources.filter { it.entityId != excludeId }
            val available = manaSolver.getAvailableManaCount(state, playerId, usable)
            return ((available - fixedCost + reduction) / xCount).coerceAtLeast(0)
        }

        var best = maxXFor(0, null)
        for (creature in harmonizeCreatures) {
            if (creature.power <= 0) continue
            best = maxOf(best, maxXFor(creature.power, creature.entityId))
        }
        return best
    }

    // --- Delve ---

    fun findDelveCards(state: GameState, playerId: EntityId): List<DelveCardData> {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        return state.getZone(graveyardZone).mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            DelveCardData(entityId, cardComponent.name, cardDef?.metadata?.imageUri)
        }
    }

    fun canAffordWithDelve(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        delveCards: List<DelveCardData>,
        precomputedSources: List<ManaSource>? = null
    ): Boolean {
        val maxDelve = minOf(delveCards.size, manaCost.genericAmount)
        val reducedCost = manaCost.reduceGeneric(maxDelve)
        return manaSolver.canPay(state, playerId, reducedCost, precomputedSources = precomputedSources)
    }

    fun calculateMinDelveNeeded(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        delveCards: List<DelveCardData>,
        precomputedSources: List<ManaSource>? = null
    ): Int {
        if (manaSolver.canPay(state, playerId, manaCost, precomputedSources = precomputedSources)) return 0
        val maxDelve = minOf(delveCards.size, manaCost.genericAmount)
        for (delveCount in 1..maxDelve) {
            if (manaSolver.canPay(state, playerId, manaCost.reduceGeneric(delveCount), precomputedSources = precomputedSources)) {
                return delveCount
            }
        }
        return maxDelve
    }

    // --- Ability cost checking ---

    /**
     * Check if a tap cost can be paid on the given entity.
     * Returns true if payable, false if not (entity is tapped or has summoning sickness).
     */
    fun canPayTapCost(state: GameState, entityId: EntityId): Boolean {
        val container = state.getEntity(entityId) ?: return false
        if (container.has<TappedComponent>()) return false
        val cardComponent = container.get<CardComponent>() ?: return false
        // Read creature-ness / haste from projected state so a Vehicle or animated permanent
        // that is currently a creature is gated. Lands keep the carve-out (basic-land mana
        // abilities are not restricted by summoning sickness).
        if (!cardComponent.typeLine.isLand && state.projectedState.isCreature(entityId)) {
            val hasSummoningSickness = container.has<SummoningSicknessComponent>()
            val hasHaste = state.projectedState.hasKeyword(entityId, Keyword.HASTE)
            if (hasSummoningSickness && !hasHaste) return false
        }
        return true
    }

    /**
     * Check if a TapAttachedCreature cost can be paid on the given entity.
     */
    fun canPayTapAttachedCreatureCost(state: GameState, entityId: EntityId): Boolean {
        val container = state.getEntity(entityId) ?: return false
        val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
            ?: return false
        val attachedEntity = state.getEntity(attachedId) ?: return false
        if (attachedEntity.has<TappedComponent>()) return false
        // Read creature-ness / haste from projected state so a Vehicle or animated permanent
        // currently being a creature is gated.
        if (state.projectedState.isCreature(attachedId)) {
            val hasSummoningSickness = attachedEntity.has<SummoningSicknessComponent>()
            val hasHaste = state.projectedState.hasKeyword(attachedId, Keyword.HASTE)
            if (hasSummoningSickness && !hasHaste) return false
        }
        return true
    }

    /**
     * Build counter removal creature info for abilities with RemoveXPlusOnePlusOneCounters cost.
     */
    fun buildCounterRemovalCreatures(state: GameState, playerId: EntityId): List<CounterRemovalCreatureData> {
        return state.entities.mapNotNull { (eid, c) ->
            if (c.get<ControllerComponent>()?.playerId == playerId &&
                c.get<CardComponent>()?.typeLine?.isCreature == true) {
                val counters = c.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                if (counters > 0) {
                    val card = c.get<CardComponent>()!!
                    CounterRemovalCreatureData(
                        entityId = eid,
                        name = card.name,
                        availableCounters = counters,
                        availableCountersByType = mapOf(com.wingedsheep.sdk.core.Counters.PLUS_ONE_PLUS_ONE to counters),
                        imageUri = card.imageUri
                    )
                } else null
            } else null
        }
    }

    /**
     * Build counter removal info for the filtered-fixed-count
     * [AbilityCost.RemovePlusOnePlusOneCounters] variant. Surfaces every permanent the
     * player controls that matches [filter] and has at least one +1/+1 counter,
     * using projected state so type-changing effects (e.g., a Vehicle that became
     * an artifact creature) are honored.
     */
    fun buildCounterRemovalPermanents(
        state: GameState,
        playerId: EntityId,
        filter: GameObjectFilter
    ): List<CounterRemovalCreatureData> {
        val context = PredicateContext(controllerId = playerId)
        val projected = state.projectedState
        return state.entities.mapNotNull { (eid, c) ->
            if (c.get<ControllerComponent>()?.playerId != playerId) return@mapNotNull null
            if (!predicateEvaluator.matches(state, projected, eid, filter, context)) {
                return@mapNotNull null
            }
            val counters = c.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            if (counters <= 0) return@mapNotNull null
            val card = c.get<CardComponent>() ?: return@mapNotNull null
            CounterRemovalCreatureData(
                entityId = eid,
                name = card.name,
                availableCounters = counters,
                availableCountersByType = mapOf(com.wingedsheep.sdk.core.Counters.PLUS_ONE_PLUS_ONE to counters),
                imageUri = card.imageUri
            )
        }
    }

    /**
     * Calculate max affordable X for activated abilities, considering various X cost types.
     */
    fun calculateMaxAffordableX(
        state: GameState,
        playerId: EntityId,
        abilityCost: AbilityCost,
        manaCost: ManaCost?,
        precomputedSources: List<ManaSource>? = null
    ): Int {
        var maxX = if (manaCost != null && manaCost.hasX) {
            val availableSources = manaSolver.getAvailableManaCount(state, playerId, precomputedSources)
            val fixedCost = manaCost.cmc
            // Each X symbol is charged once, so a cost like {X}{X}{X} (Momir) consumes 3 mana per
            // point of X — divide the spare mana by the number of X symbols, not just subtract the
            // fixed part. xCount is 1 for the common single-{X} case, so this is a no-op there.
            val xSymbols = manaCost.xCount.coerceAtLeast(1)
            ((availableSources - fixedCost).coerceAtLeast(0)) / xSymbols
        } else {
            Int.MAX_VALUE
        }

        // Cap by graveyard size if ExileXFromGraveyard
        val hasExileXCost = when (abilityCost) {
            is AbilityCost.Composite -> abilityCost.costs.any { it is AbilityCost.ExileXFromGraveyard }
            else -> false
        }
        if (hasExileXCost) {
            val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
            maxX = minOf(maxX, state.getZone(graveyardZone).size)
        }

        // Cap by total +1/+1 counters if RemoveXPlusOnePlusOneCounters
        val hasRemoveCounters = when (abilityCost) {
            is AbilityCost.RemoveXPlusOnePlusOneCounters -> true
            is AbilityCost.Composite -> abilityCost.costs.any { it is AbilityCost.RemoveXPlusOnePlusOneCounters }
            else -> false
        }
        if (hasRemoveCounters) {
            var totalCounters = 0
            for ((_, container) in state.entities) {
                if (container.get<ControllerComponent>()?.playerId == playerId &&
                    container.get<CardComponent>()?.typeLine?.isCreature == true) {
                    totalCounters += container.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                }
            }
            maxX = minOf(maxX, totalCounters)
        }

        // Cap by untapped matching permanents if TapXPermanents
        val tapXCost = when (abilityCost) {
            is AbilityCost.TapXPermanents -> abilityCost
            is AbilityCost.Composite -> abilityCost.costs.filterIsInstance<AbilityCost.TapXPermanents>().firstOrNull()
            else -> null
        }
        if (tapXCost != null) {
            maxX = minOf(maxX, findAbilityTapTargets(state, playerId, tapXCost.filter).size)
        }

        // Cap by life total if PayXLife. Per CR 119.4 a player may pay an amount of life greater
        // than 0 only if their life total is at least that amount — so X can be as large as the
        // player's current life, paying down to exactly 0 (they then lose to a state-based action
        // per CR 104.3b/704.5, but the payment itself is legal).
        val hasPayXLife = when (abilityCost) {
            is AbilityCost.PayXLife -> true
            is AbilityCost.Composite -> abilityCost.costs.any { it is AbilityCost.PayXLife }
            else -> false
        }
        if (hasPayXLife) {
            val life = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
            maxX = minOf(maxX, life.coerceAtLeast(0))
        }

        return maxX
    }
}
