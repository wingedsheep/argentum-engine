package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.DealDamagePerEntityInZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for DealDamagePerEntityInZoneEffect.
 *
 * Counts how many of the tracked entities are still in the specified zone,
 * then deals (count * damagePerEntity) damage to the target.
 *
 * Used for Dragonhawk, Fate's Tempest: "deal 2 damage to each opponent
 * for each of those cards that are still exiled."
 */
class DealDamagePerEntityInZoneExecutor : EffectExecutor<DealDamagePerEntityInZoneEffect> {

    override val effectType: KClass<DealDamagePerEntityInZoneEffect> = DealDamagePerEntityInZoneEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamagePerEntityInZoneEffect,
        context: EffectContext
    ): ExecutionResult {
        // Count how many of the tracked entities are still in the specified zone
        val stillInZone = effect.entityIds.count { entityId ->
            state.zones.any { (zoneKey, entities) ->
                zoneKey.zoneType == effect.zone && entityId in entities
            }
        }

        if (stillInZone == 0) {
            return ExecutionResult.success(state)
        }

        val totalDamage = stillInZone * effect.damagePerEntity

        val damageSourceTarget = effect.damageSource
        val sourceId = if (damageSourceTarget != null) {
            context.resolveTarget(damageSourceTarget, state)
        } else {
            context.sourceId
        }

        // For PlayerRef targets, resolve to potentially multiple players
        if (effect.target is EffectTarget.PlayerRef) {
            val playerIds = context.resolvePlayerTargets(effect.target, state)
            if (playerIds.isEmpty()) {
                return ExecutionResult.success(state)
            }

            var newState = state
            val events = mutableListOf<EngineGameEvent>()
            for (playerId in playerIds) {
                val result = dealDamageToTarget(newState, playerId, totalDamage, sourceId, cantBePrevented = false)
                newState = result.newState
                events.addAll(result.events)
            }
            return ExecutionResult.success(newState, events)
        }

        // Single target resolution
        val targetId = context.resolveTarget(effect.target, state)
            ?: return ExecutionResult.error(state, "No valid target for damage")

        return dealDamageToTarget(state, targetId, totalDamage, sourceId, cantBePrevented = false)
    }
}
