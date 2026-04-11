package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.state.GameState
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

    override fun execute(
        state: GameState,
        effect: GrantActivatedAbilityToGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val filter = effect.filter
        val excludeSelfId = if (filter.excludeSelf) context.sourceId else null
        val matched = BattlefieldFilterUtils.findMatchingOnBattlefield(state, filter.baseFilter, context, excludeSelfId)

        if (matched.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val grants = matched.map { entityId ->
            GrantedActivatedAbility(
                entityId = entityId,
                ability = effect.ability,
                duration = effect.duration
            )
        }

        val newState = state.copy(
            grantedActivatedAbilities = state.grantedActivatedAbilities + grants
        )

        return ExecutionResult.success(newState)
    }
}
