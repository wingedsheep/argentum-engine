package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import kotlin.reflect.KClass

/**
 * Executor for MarkExileOnDeathEffect.
 * Adds an ExileOnDeath floating effect so that if the target creature would die
 * within the effect's duration, it goes to exile instead of the graveyard. A
 * [Duration.Permanent] mark ends when the creature leaves the battlefield — zone changes
 * prune floating effects that target the departing object.
 *
 * If the target is a player (not a creature), this effect does nothing.
 */
class MarkExileOnDeathExecutor : EffectExecutor<MarkExileOnDeathEffect> {

    override val effectType: KClass<MarkExileOnDeathEffect> = MarkExileOnDeathEffect::class

    override fun execute(
        state: GameState,
        effect: MarkExileOnDeathEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for exile-on-death marker")

        // Only applies to creatures, not players
        val isPlayer = state.getEntity(targetId)?.get<LifeTotalComponent>() != null
        if (isPlayer) {
            return EffectResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            // "It gains 'If this creature would die, exile it instead'" is an ability the creature
            // has, which lose-all-abilities effects strip; a "this turn" rider is the spell's own
            // replacement effect and isn't.
            modification = SerializableModification.ExileOnDeath(
                grantedAbility = effect.duration == Duration.Permanent
            ),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
