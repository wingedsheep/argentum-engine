package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import kotlin.reflect.KClass

/**
 * Executor for FilterCollectionEffect.
 *
 * Splits a named collection into matching and non-matching subsets: the cards matching the
 * effect's [GameObjectFilter], narrowed by its collection-relative [CollectionFilter] when it has
 * one. A purely automatic filter with no player choice.
 */
class FilterCollectionExecutor(
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<FilterCollectionEffect> {
    private val amountEvaluator = predicateEvaluator.amounts

    override val effectType: KClass<FilterCollectionEffect> = FilterCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: FilterCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val cards = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val projected = state.projectedState
        val predicateContext = PredicateContext.fromEffectContext(context)
        val unknown = context.pipeline.storedCollections[EffectDiscardDestinations.UNDEFINED + ":" + effect.from].orEmpty().toSet()
        val readsCharacteristics = when (effect.collectionFilter) {
            is CollectionFilter.SharesSubtypeWithSacrificed, is CollectionFilter.GreatestPower,
            is CollectionFilter.LeastToughness, is CollectionFilter.GreatestManaValue -> true
            else -> false
        }
        val eligible = if (readsCharacteristics) cards.filterNot { it in unknown } else cards
        val undefinedFilter = EffectDiscardDestinations.filterForUndefinedCharacteristics(effect.filter)
        val passing = eligible.filter { id ->
            val filter = if (id in unknown) undefinedFilter else effect.filter
            filter != null && predicateEvaluator.matches(state, projected, id, filter, predicateContext)
        }
        val kept = (effect.collectionFilter?.let { keep(state, it, passing, context) } ?: passing).toSet()
        val (matching, nonMatching) = cards.partition { it in kept }

        val updatedCollections = mutableMapOf(effect.storeMatching to matching)
        val storeNonMatching = effect.storeNonMatching
        if (storeNonMatching != null) {
            updatedCollections[storeNonMatching] = nonMatching
        }

        return EffectResult.success(state).copy(updatedCollections = updatedCollections)
    }

    /** The members of [cards] a collection-relative [filter] keeps, in their original order. */
    private fun keep(
        state: GameState,
        filter: CollectionFilter,
        cards: List<EntityId>,
        context: EffectContext
    ): List<EntityId> {
        val projected = state.projectedState
        return when (filter) {
            is CollectionFilter.SharesSubtypeWithSacrificed -> {
                val sacrificed = context.sacrificedPermanents.firstOrNull() ?: return emptyList()
                val sacrificedSubtypes = sacrificed.subtypes.takeIf { it.isNotEmpty() }
                    ?: state.getEntity(sacrificed.entityId)?.get<CardComponent>()
                        ?.typeLine?.subtypes?.map { it.value }?.toSet()
                    ?: emptySet()
                cards.filter { projected.getSubtypes(it).intersect(sacrificedSubtypes).isNotEmpty() }
            }

            is CollectionFilter.GreatestPower -> {
                val maxPower = cards.maxOfOrNull { projected.getPower(it) ?: Int.MIN_VALUE }
                if (maxPower == null || maxPower == Int.MIN_VALUE) emptyList()
                else cards.filter { (projected.getPower(it) ?: Int.MIN_VALUE) == maxPower }
            }

            is CollectionFilter.LeastToughness -> {
                val minToughness = cards.minOfOrNull { projected.getToughness(it) ?: Int.MAX_VALUE }
                if (minToughness == null || minToughness == Int.MAX_VALUE) emptyList()
                else cards.filter { (projected.getToughness(it) ?: Int.MAX_VALUE) == minToughness }
            }

            is CollectionFilter.GreatestManaValue -> {
                // A face-down permanent has no mana cost (CR 708.2a), so its mana value is 0
                // (CR 202.3a): a gathered battlefield collection (Break Under Pressure) never
                // ranks a morph by its hidden cost.
                fun manaValueOf(cardId: EntityId): Int =
                    if (projected.getProjectedValues(cardId)?.isFaceDown == true) 0
                    else state.getEntity(cardId)?.get<CardComponent>()?.manaValue ?: Int.MIN_VALUE
                val maxManaValue = cards.maxOfOrNull { manaValueOf(it) }
                if (maxManaValue == null || maxManaValue == Int.MIN_VALUE) emptyList()
                else cards.filter { manaValueOf(it) == maxManaValue }
            }

            is CollectionFilter.ExcludeEntity -> {
                val excludedId = TargetResolutionUtils.resolveEntity(filter.entity, context, state)
                cards.filter { it != excludedId }
            }

            is CollectionFilter.ExcludeOtherCollection -> {
                val excluded = context.pipeline.storedCollections[filter.otherCollectionName]
                    ?.toSet() ?: emptySet()
                cards.filter { it !in excluded }
            }
        }
    }
}
