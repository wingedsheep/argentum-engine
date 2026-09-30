package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CantGainLifeComponent
import com.wingedsheep.engine.state.components.player.CantLoseLifeComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.LockLifeGainEffect
import com.wingedsheep.sdk.scripting.effects.LockLifeLossEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Resolves [LockLifeGainEffect] — tags each resolved player target with a
 * [CantGainLifeComponent] so their life gain is locked for the effect's duration.
 *
 * Only players (entities with a [LifeTotalComponent]) are affected; a non-player target — e.g. a
 * creature or planeswalker hit by a "deal damage to any target" rider — is silently skipped, so
 * the effect composes safely after such damage. The lock is idempotent at the most-durable level:
 * a Permanent lock is never downgraded by a later turn-scoped lock.
 */
class LockLifeGainExecutor : EffectExecutor<LockLifeGainEffect> {

    override val effectType: KClass<LockLifeGainEffect> = LockLifeGainEffect::class

    override fun execute(
        state: GameState,
        effect: LockLifeGainEffect,
        context: EffectContext
    ): EffectResult = EffectResult.success(
        lockPlayers(state, context, effect.target, effect.duration, { it.get<CantGainLifeComponent>()?.removeOn }) {
            CantGainLifeComponent(removeOn = it)
        }
    )
}

/**
 * Resolves [LockLifeLossEffect] — the [CantLoseLifeComponent] sibling of [LockLifeGainExecutor],
 * with the same player-only, never-downgrade semantics.
 */
class LockLifeLossExecutor : EffectExecutor<LockLifeLossEffect> {

    override val effectType: KClass<LockLifeLossEffect> = LockLifeLossEffect::class

    override fun execute(
        state: GameState,
        effect: LockLifeLossEffect,
        context: EffectContext
    ): EffectResult = EffectResult.success(
        lockPlayers(state, context, effect.target, effect.duration, { it.get<CantLoseLifeComponent>()?.removeOn }) {
            CantLoseLifeComponent(removeOn = it)
        }
    )
}

// Reified: `ComponentContainer.with` keys a component by its static type, so the lock must reach
// it as its concrete class, not as `Component`.
private inline fun <reified T : Component> lockPlayers(
    state: GameState,
    context: EffectContext,
    target: EffectTarget,
    duration: Duration,
    existingRemoval: (ComponentContainer) -> PlayerEffectRemoval?,
    crossinline lock: (PlayerEffectRemoval) -> T
): GameState {
    val removeOn = when (duration) {
        Duration.EndOfTurn -> PlayerEffectRemoval.EndOfTurn
        Duration.UntilYourNextTurn -> PlayerEffectRemoval.UntilYourNextTurn
        else -> PlayerEffectRemoval.Permanent
    }

    val targetIds = context.resolvePlayerTargets(target, state)
        .filter { state.turnOrder.contains(it) }

    var newState = state
    for (playerId in targetIds) {
        val container = newState.getEntity(playerId) ?: continue
        // Only players have a life total to lock.
        if (container.get<LifeTotalComponent>() == null) continue
        // Never downgrade a Permanent lock to a shorter duration.
        if (existingRemoval(container) == PlayerEffectRemoval.Permanent) continue
        newState = newState.updateEntity(playerId) { c -> c.with(lock(removeOn)) }
    }
    return newState
}
