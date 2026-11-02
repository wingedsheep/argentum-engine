package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CantGetCountersComponent
import com.wingedsheep.engine.state.components.player.PlayerCounterLock
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.LockPlayerCountersEffect
import kotlin.reflect.KClass

/**
 * Resolves [LockPlayerCountersEffect] — adds a [PlayerCounterLock] to each resolved player target's
 * [CantGetCountersComponent], so `ProjectedState.canReceiveCounters` refuses them counters.
 *
 * A source-keyed lock ([Duration.WhileSourceOnBattlefield]) records the source *object*. If that
 * object has already left the battlefield as the effect resolves, the lock would end the moment it
 * began, so none is added (CR 611.2b) — the same holds when the source left and came back as a new
 * object. Non-player targets are skipped. Locks whose source has since left are pruned as a new
 * lock lands, so the component never accumulates dead entries.
 */
class LockPlayerCountersExecutor : EffectExecutor<LockPlayerCountersEffect> {

    override val effectType: KClass<LockPlayerCountersEffect> = LockPlayerCountersEffect::class

    override fun execute(
        state: GameState,
        effect: LockPlayerCountersEffect,
        context: EffectContext
    ): EffectResult {
        val lock = when (effect.duration) {
            is Duration.WhileSourceOnBattlefield -> {
                val sourceId = context.sourceId ?: return EffectResult.success(state)
                if (!context.objectReferences.isCurrent(context.objectReferences.source, state) ||
                    state.logicalZone(sourceId)?.zoneType != Zone.BATTLEFIELD
                ) return EffectResult.success(state)
                val ref = state.objectRef(sourceId) ?: return EffectResult.success(state)
                PlayerCounterLock(PlayerEffectRemoval.Permanent, whileSource = ref)
            }
            Duration.EndOfTurn -> PlayerCounterLock(PlayerEffectRemoval.EndOfTurn)
            else -> PlayerCounterLock(PlayerEffectRemoval.Permanent)
        }

        var newState = state
        for (playerId in context.resolvePlayerTargets(effect.target, state)) {
            if (playerId !in newState.turnOrder) continue
            newState = newState.updateEntity(playerId) { container ->
                val live = container.get<CantGetCountersComponent>()?.locks.orEmpty().filter { it.holds(state) }
                container.with(CantGetCountersComponent(live + lock))
            }
        }
        return EffectResult.success(newState)
    }
}
