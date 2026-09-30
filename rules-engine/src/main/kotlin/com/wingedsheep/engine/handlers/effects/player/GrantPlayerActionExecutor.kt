package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PlayerActionPermissionsChangedEvent
import com.wingedsheep.engine.event.PlayerActionPermission
import com.wingedsheep.engine.event.entityId
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.GrantPlayerActionEffect
import kotlin.reflect.KClass

class GrantPlayerActionExecutor : EffectExecutor<GrantPlayerActionEffect> {
    override val effectType: KClass<GrantPlayerActionEffect> = GrantPlayerActionEffect::class
    override fun execute(state: GameState, effect: GrantPlayerActionEffect, context: EffectContext): EffectResult {
        val player = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)
        if (player !in state.turnOrder) return EffectResult.error(state, "Player action recipient must be a player")
        val targets = context.targets + context.pipeline.namedTargets.values + context.alignedTargets.filterNotNull()
        val (id, allocated) = state.newRoutingId()
        val permission = PlayerActionPermission(
            id = id, playerId = player, action = effect,
            context = context.copy(controllerId = player),
            references = targets.associate { it.entityId() to state.objectRef(it.entityId()) },
        )
        return EffectResult.success(
            allocated.copy(playerActionPermissions = allocated.playerActionPermissions + permission),
            listOf(PlayerActionPermissionsChangedEvent(player)),
        )
    }
}
