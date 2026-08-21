package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastTargetFromExileEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantFreeCastTargetFromExileEffect.
 *
 * Registers a [MayPlayPermission] for a single target in exile and stamps
 * PlayWithoutPayingCostComponent on it, granting the controller permission to
 * cast it from exile without paying its mana cost. Optionally adds
 * AfterResolveDestinationComponent so the spell goes to exile instead of graveyard
 * after resolution.
 */
class GrantFreeCastTargetFromExileExecutor : EffectExecutor<GrantFreeCastTargetFromExileEffect> {

    override val effectType: KClass<GrantFreeCastTargetFromExileEffect> =
        GrantFreeCastTargetFromExileEffect::class

    override fun execute(
        state: GameState,
        effect: GrantFreeCastTargetFromExileEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val targetId = context.resolveTarget(effect.target) ?: return EffectResult.success(state)

        var newState = state.updateEntity(targetId) { container ->
            var updated = container.with(PlayWithoutPayingCostComponent(controllerId = controllerId))
            if (effect.exileAfterResolve) {
                updated = updated.with(AfterResolveDestinationComponent())
            }
            updated
        }

        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(targetId),
                controllerId = controllerId,
                sourceId = context.sourceId,
                timestamp = state.timestamp,
            )
        )

        return EffectResult.success(newState)
    }
}
