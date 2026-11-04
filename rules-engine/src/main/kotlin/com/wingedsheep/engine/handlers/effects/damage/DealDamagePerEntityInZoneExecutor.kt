package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils.dealDamageToTarget
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
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
class DealDamagePerEntityInZoneExecutor(private val zones: ZoneTransitionService) : EffectExecutor<DealDamagePerEntityInZoneEffect> {

    override val effectType: KClass<DealDamagePerEntityInZoneEffect> = DealDamagePerEntityInZoneEffect::class

    override fun execute(
        state: GameState,
        effect: DealDamagePerEntityInZoneEffect,
        context: EffectContext
    ): EffectResult {
        // Count how many of the tracked entities are still in the specified zone
        val stillInZone = effect.entityIds.count { entityId ->
            state.zones.any { (zoneKey, entities) ->
                zoneKey.zoneType == effect.zone && entityId in entities
            }
        }

        if (stillInZone == 0) {
            return EffectResult.success(state)
        }

        val totalDamage = stillInZone * effect.damagePerEntity

        val damageSourceTarget = effect.damageSource
        val sourceId = if (damageSourceTarget != null) {
            context.resolveTarget(damageSourceTarget, state)
        } else {
            context.sourceId
        }

        val damageSourceRef = if (damageSourceTarget == null) context.objectReferences.origin
            ?: sourceId?.let(state::objectRef) else sourceId?.let(state::objectRef)

        val targets = if (effect.target is EffectTarget.PlayerRef) context.resolvePlayerTargets(effect.target, state)
            else listOfNotNull(context.resolveTarget(effect.target, state))
        return DamageReplacementChain.deal(zones, state,
            targets.map { DamageReplacementChain.Instance(sourceId, it, totalDamage, sourceRef = damageSourceRef) }, effect, context)
    }
}
