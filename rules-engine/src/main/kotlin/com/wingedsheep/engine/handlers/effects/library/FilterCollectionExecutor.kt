package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import kotlin.reflect.KClass

/**
 * Executor for FilterCollectionEffect.
 *
 * Splits a named collection into matching and non-matching subsets based on
 * a [CollectionFilter]. This is a purely automatic filter with no player choice.
 */
class FilterCollectionExecutor : EffectExecutor<FilterCollectionEffect> {

    override val effectType: KClass<FilterCollectionEffect> = FilterCollectionEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val amountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: FilterCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val cards = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val filter = effect.filter
        val projected = state.projectedState
        val (matching, nonMatching) = when (filter) {
            is CollectionFilter.ExcludeSubtypesFromStored -> {
                val excludedSubtypes = context.pipeline.storedStringLists[filter.storedKey]
                    ?.map { Subtype(it) }?.toSet() ?: emptySet()

                cards.partition { cardId ->
                    val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
                    val subtypes = cardComponent?.typeLine?.subtypes ?: emptyList()
                    // "Matching" = does NOT have any excluded subtype (passes through the filter)
                    subtypes.none { it in excludedSubtypes }
                }
            }

            is CollectionFilter.SharesSubtypeWithSacrificed -> {
                val sacrificedId = context.sacrificedPermanents.firstOrNull()
                if (sacrificedId == null) {
                    emptyList<EntityId>() to cards
                } else {
                    val sacrificedSubtypes = context.sacrificedPermanentSubtypes[sacrificedId]
                        ?: state.getEntity(sacrificedId)?.get<CardComponent>()
                            ?.typeLine?.subtypes?.map { it.value }?.toSet()
                        ?: emptySet()

                    cards.partition { cardId ->
                        val creatureSubtypes = projected.getSubtypes(cardId)
                        creatureSubtypes.intersect(sacrificedSubtypes).isNotEmpty()
                    }
                }
            }

            is CollectionFilter.MatchesFilter -> {
                val predicateContext = PredicateContext.fromEffectContext(context)
                cards.partition { cardId ->
                    predicateEvaluator.matchesWithProjection(state, projected, cardId, filter.filter, predicateContext)
                }
            }

            is CollectionFilter.GreatestPower -> {
                val maxPower = cards.maxOfOrNull { projected.getPower(it) ?: Int.MIN_VALUE }
                if (maxPower == null || maxPower == Int.MIN_VALUE) {
                    emptyList<EntityId>() to cards
                } else {
                    cards.partition { (projected.getPower(it) ?: Int.MIN_VALUE) == maxPower }
                }
            }

            is CollectionFilter.ManaValueAtMost -> {
                val maxManaValue = amountEvaluator.evaluate(state, filter.max, context)
                cards.partition { cardId ->
                    val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
                    val manaValue = cardComponent?.manaValue ?: 0
                    manaValue <= maxManaValue
                }
            }

            is CollectionFilter.ManaValueEquals -> {
                val exactManaValue = amountEvaluator.evaluate(state, filter.value, context)
                cards.partition { cardId ->
                    val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
                    val manaValue = cardComponent?.manaValue ?: 0
                    manaValue == exactManaValue
                }
            }

            is CollectionFilter.ExcludeEntity -> {
                val excludedId = resolveEntityReference(filter.entity, context)
                cards.partition { it != excludedId }
            }
        }

        val updatedCollections = mutableMapOf(effect.storeMatching to matching)
        val storeNonMatching = effect.storeNonMatching
        if (storeNonMatching != null) {
            updatedCollections[storeNonMatching] = nonMatching
        }

        return EffectResult.success(state).copy(updatedCollections = updatedCollections)
    }

    private fun resolveEntityReference(ref: EntityReference, context: EffectContext): EntityId? =
        when (ref) {
            is EntityReference.Source -> context.sourceId
            is EntityReference.Target -> {
                val target = context.targets.getOrNull(ref.index)
                when (target) {
                    is ChosenTarget.Permanent -> target.entityId
                    is ChosenTarget.Spell -> target.spellEntityId
                    is ChosenTarget.Player -> target.playerId
                    else -> null
                }
            }
            is EntityReference.Sacrificed -> context.sacrificedPermanents.getOrNull(ref.index)
            is EntityReference.TappedAsCost -> context.tappedPermanents.getOrNull(ref.index)
            is EntityReference.Triggering -> context.triggeringEntityId
            is EntityReference.AffectedEntity -> context.affectedEntityId
        }
}
