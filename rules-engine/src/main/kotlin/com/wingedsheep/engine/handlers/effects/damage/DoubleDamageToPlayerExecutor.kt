package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.DoubleDamageToPlayerEffect
import kotlin.reflect.KClass

/**
 * Executor for [DoubleDamageToPlayerEffect].
 *
 * Resolves the affected player once from [DoubleDamageToPlayerEffect.target] (a player reference such
 * as `Player.TriggeringPlayer` — the player just dealt combat damage) and installs a
 * [SerializableModification.DoubleDamageToPlayer] floating effect scoped to that player for the
 * effect's [DoubleDamageToPlayerEffect.duration]. `DamageUtils.applyStaticDamageAmplification` reads
 * it for every damage instance (combat and noncombat) whose recipient is that player or a permanent
 * that player controls, doubling the amount. The floating effect's controller is the resolver, so an
 * `UntilYourNextTurn` duration expires it after the resolver's next untap step; it is independent of
 * the source that installed it (which may leave the battlefield).
 *
 * Lightning, Army of One — "Stagger — Whenever Lightning deals combat damage to a player, until your
 * next turn, if a source would deal damage to that player or a permanent that player controls, it
 * deals double that damage instead."
 */
class DoubleDamageToPlayerExecutor : EffectExecutor<DoubleDamageToPlayerEffect> {

    override val effectType: KClass<DoubleDamageToPlayerEffect> = DoubleDamageToPlayerEffect::class

    override fun execute(
        state: GameState,
        effect: DoubleDamageToPlayerEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = TargetResolutionUtils.resolvePlayerTarget(effect.target, context)
            ?: return EffectResult.success(state)

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.DoubleDamageToPlayer(playerId),
            // affectedEntities carries the scoped player (also the cleanup key); the read site
            // matches it against the damage recipient and its controller.
            affectedEntities = setOf(playerId),
            duration = effect.duration,
            context = context
        )
        return EffectResult.success(newState)
    }
}
