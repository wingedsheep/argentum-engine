package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityToGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantActivatedAbilityToGroupEffect.
 * "Each creature you control gains '{cost}: {effect}' until end of turn" and similar group ability grants.
 *
 * Adds GrantedActivatedAbility entries for each matching creature.
 */
class GrantActivatedAbilityToGroupExecutor : EffectExecutor<GrantActivatedAbilityToGroupEffect> {

    override val effectType: KClass<GrantActivatedAbilityToGroupEffect> = GrantActivatedAbilityToGroupEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: GrantActivatedAbilityToGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val grants = mutableListOf<GrantedActivatedAbility>()

        val filter = effect.filter
        val predicateContext = PredicateContext.fromEffectContext(context)
        val projected = stateProjector.project(state)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue

            // Check excludeSelf
            if (filter.excludeSelf && entityId == context.sourceId) continue

            // Apply unified filter
            if (!predicateEvaluator.matchesWithProjection(state, projected, entityId, filter.baseFilter, predicateContext)) {
                continue
            }

            grants.add(
                GrantedActivatedAbility(
                    entityId = entityId,
                    ability = effect.ability,
                    duration = effect.duration
                )
            )
        }

        if (grants.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val newState = state.copy(
            grantedActivatedAbilities = state.grantedActivatedAbilities + grants
        )

        return ExecutionResult.success(newState)
    }
}
