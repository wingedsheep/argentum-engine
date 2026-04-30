package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ExileAndGrantOwnerPlayPermissionEffect
import kotlin.reflect.KClass

/**
 * Exiles a target and grants its owner permission to play it from exile for as long
 * as it remains exiled. Optionally adds a generic tax when the owner is an opponent
 * of the effect controller.
 */
class ExileAndGrantOwnerPlayPermissionExecutor : EffectExecutor<ExileAndGrantOwnerPlayPermissionEffect> {

    override val effectType: KClass<ExileAndGrantOwnerPlayPermissionEffect> =
        ExileAndGrantOwnerPlayPermissionEffect::class

    override fun execute(
        state: GameState,
        effect: ExileAndGrantOwnerPlayPermissionEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = TargetResolutionUtils.resolveTarget(effect.target, context, state)
            ?: return EffectResult.error(state, "Could not resolve target to exile")
        val ownerId = state.getEntity(targetId)?.get<CardComponent>()?.ownerId
            ?: return EffectResult.error(state, "Could not resolve owner of exiled target")

        val transition = ZoneTransitionService.moveToZone(state, targetId, Zone.EXILE)

        var newState = transition.state.updateEntity(targetId) { container ->
            container.with(
                MayPlayFromExileComponent(
                    controllerId = ownerId,
                    permanent = true
                )
            )
        }

        if (effect.opponentCostIncrease > 0 && ownerId != context.controllerId) {
            newState = newState.updateEntity(targetId) { container ->
                container.with(
                    PlayWithCostIncreaseComponent(
                        controllerId = ownerId,
                        amount = effect.opponentCostIncrease
                    )
                )
            }
        }

        return EffectResult.success(newState, transition.events)
    }
}
