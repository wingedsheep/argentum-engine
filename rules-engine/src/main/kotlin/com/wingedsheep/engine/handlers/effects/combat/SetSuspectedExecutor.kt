package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.SetSuspectedEffect
import kotlin.reflect.KClass

/**
 * Atomic executor for the named "suspected" status (CR 701.60).
 *
 * The full Suspect mechanic (status + menace + can't-block) is composed in
 * `Effects.Suspect` as a [com.wingedsheep.sdk.scripting.effects.CompositeEffect]
 * over [SetSuspectedEffect], `GrantKeywordEffect(MENACE)`, and `CantBlockEffect`.
 * This executor only applies the named-status modification.
 *
 * CR 701.60d ("A suspected permanent can't become suspected again") is enforced
 * here: a redundant SetSuspected is a no-op so the named-status floating effect
 * list stays clean and any future "becomes suspected" trigger fires exactly
 * once per suspect.
 */
class SetSuspectedExecutor : EffectExecutor<SetSuspectedEffect> {

    override val effectType: KClass<SetSuspectedEffect> = SetSuspectedEffect::class

    override fun execute(
        state: GameState,
        effect: SetSuspectedEffect,
        context: EffectContext
    ): EffectResult {
        val entityId = TargetResolutionUtils.resolveTarget(effect.target, context)
            ?: return EffectResult.success(state)
        state.getEntity(entityId)?.get<CardComponent>()
            ?: return EffectResult.success(state)

        val alreadySuspected = state.floatingEffects.any { fx ->
            fx.effect.modification is SerializableModification.SetSuspected
                && entityId in fx.effect.affectedEntities
        }
        if (alreadySuspected) {
            return EffectResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetSuspected,
            affectedEntities = setOf(entityId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
