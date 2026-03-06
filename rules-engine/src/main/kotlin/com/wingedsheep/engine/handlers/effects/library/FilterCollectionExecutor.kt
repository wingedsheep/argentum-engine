package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import kotlin.reflect.KClass

/**
 * Executor for FilterCollectionEffect.
 *
 * Splits a named collection into matching and non-matching subsets based on
 * a [CollectionFilter]. This is a purely automatic filter with no player choice.
 */
class FilterCollectionExecutor : EffectExecutor<FilterCollectionEffect> {

    override val effectType: KClass<FilterCollectionEffect> = FilterCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: FilterCollectionEffect,
        context: EffectContext
    ): ExecutionResult {
        val cards = context.storedCollections[effect.from]
            ?: return ExecutionResult.error(state, "No collection named '${effect.from}' in storedCollections")

        val filter = effect.filter
        val (matching, nonMatching) = when (filter) {
            is CollectionFilter.ExcludeSubtypesFromStored -> {
                val excludedSubtypes = context.storedStringLists[filter.storedKey]
                    ?.map { Subtype(it) }?.toSet() ?: emptySet()

                cards.partition { cardId ->
                    val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
                    val subtypes = cardComponent?.typeLine?.subtypes ?: emptyList()
                    // "Matching" = does NOT have any excluded subtype (passes through the filter)
                    subtypes.none { it in excludedSubtypes }
                }
            }
        }

        val updatedCollections = mutableMapOf(effect.storeMatching to matching)
        val storeNonMatching = effect.storeNonMatching
        if (storeNonMatching != null) {
            updatedCollections[storeNonMatching] = nonMatching
        }

        return ExecutionResult.success(state).copy(updatedCollections = updatedCollections)
    }
}
