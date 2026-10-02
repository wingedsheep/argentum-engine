package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlin.reflect.KClass

/**
 * Executor for ConditionalOnCollectionEffect.
 *
 * Measures a named collection (raw size, distinct card type count, or filtered size)
 * against the configured minimum and delegates to the appropriate sub-effect.
 */
class ConditionalOnCollectionExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<ConditionalOnCollectionEffect> {

    override val effectType: KClass<ConditionalOnCollectionEffect> = ConditionalOnCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: ConditionalOnCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val collection = context.pipeline.storedCollections[effect.collection] ?: emptyList()

        val knownCards = collection.filter { it !in context.pipeline.storedCollections[com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations.UNDEFINED + ":" + effect.collection].orEmpty() }
        val measuredSize = when {
            effect.countDistinctCardTypes -> knownCards.flatMap { entityId ->
                state.getEntity(entityId)?.get<CardComponent>()?.typeLine?.cardTypes ?: emptySet()
            }.toSet().size

            effect.filter != GameObjectFilter.Any -> {
                val predicateContext = PredicateContext.fromEffectContext(context)
                val unknown = context.pipeline.storedCollections[
                    com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations.UNDEFINED + ":" + effect.collection
                ].orEmpty().toSet()
                val undefinedFilter = com.wingedsheep.engine.handlers.effects.EffectDiscardDestinations
                    .filterForUndefinedCharacteristics(effect.filter)
                collection.count { entityId ->
                    val filter = if (entityId in unknown) undefinedFilter else effect.filter
                    filter != null && predicateEvaluator.matches(state, state.projectedState, entityId, filter, predicateContext)
                }
            }

            else -> collection.size
        }

        val elseEffect = effect.ifEmpty
        return if (measuredSize >= effect.minSize) {
            effectExecutor(state, effect.ifNotEmpty, context)
        } else if (elseEffect != null) {
            effectExecutor(state, elseEffect, context)
        } else {
            EffectResult.success(state)
        }
    }
}
