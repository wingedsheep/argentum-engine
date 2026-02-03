package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreatureGroupFilter
import com.wingedsheep.sdk.scripting.GrantCantBeBlockedExceptByColorEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantCantBeBlockedExceptByColorEffect.
 * "Black creatures you control can't be blocked this turn except by black creatures."
 *
 * This creates floating effects for each creature matching the filter, marking them
 * as only blockable by creatures of the specified color. The CombatManager checks
 * for this restriction during declare blockers validation.
 */
class GrantCantBeBlockedExceptByColorExecutor : EffectExecutor<GrantCantBeBlockedExceptByColorEffect> {

    override val effectType: KClass<GrantCantBeBlockedExceptByColorEffect> = GrantCantBeBlockedExceptByColorEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: GrantCantBeBlockedExceptByColorEffect,
        context: EffectContext
    ): ExecutionResult {
        val affectedEntities = mutableSetOf<EntityId>()

        // Use unified filter if present, otherwise fall back to legacy filter
        val unifiedFilter = effect.unifiedFilter
        if (unifiedFilter != null) {
            // Use PredicateEvaluator for unified filter
            val predicateContext = PredicateContext.fromEffectContext(context)
            for (entityId in state.getBattlefield()) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                if (!cardComponent.typeLine.isCreature) continue

                val excludeSelf = unifiedFilter.excludeSelf && entityId == context.sourceId
                if (excludeSelf) continue

                if (!predicateEvaluator.matches(state, entityId, unifiedFilter.baseFilter, predicateContext)) continue

                affectedEntities.add(entityId)
            }
        } else {
            // Legacy filter support
            for (entityId in state.getBattlefield()) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                if (!cardComponent.typeLine.isCreature) continue
                @Suppress("DEPRECATION")
                if (!matchesFilter(state, entityId, cardComponent, effect.filter, context)) continue

                affectedEntities.add(entityId)
            }
        }

        if (affectedEntities.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Create a floating effect marking these creatures as only blockable by the specified color
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,  // Layer doesn't matter for this effect
                sublayer = null,
                modification = SerializableModification.CantBeBlockedExceptByColor(
                    color = effect.canOnlyBeBlockedByColor.name
                ),
                affectedEntities = affectedEntities
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }

    private fun matchesFilter(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        filter: CreatureGroupFilter,
        context: EffectContext
    ): Boolean {
        val controllerId = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

        return when (filter) {
            is CreatureGroupFilter.All -> true
            is CreatureGroupFilter.AllOther -> entityId != context.sourceId
            is CreatureGroupFilter.AllYouControl -> controllerId == context.controllerId
            is CreatureGroupFilter.AllOpponentsControl -> controllerId != context.controllerId
            is CreatureGroupFilter.NonWhite -> !cardComponent.colors.contains(com.wingedsheep.sdk.core.Color.WHITE)
            is CreatureGroupFilter.NotColor -> !cardComponent.colors.contains(filter.excludedColor)
            is CreatureGroupFilter.ColorYouControl ->
                controllerId == context.controllerId && cardComponent.colors.contains(filter.color)
            is CreatureGroupFilter.WithKeywordYouControl ->
                controllerId == context.controllerId && cardComponent.baseKeywords.contains(filter.keyword)
            is CreatureGroupFilter.OtherTappedYouControl -> {
                val isTapped = state.getEntity(entityId)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() ?: false
                entityId != context.sourceId && controllerId == context.controllerId && isTapped
            }
        }
    }
}
