package com.wingedsheep.engine.handlers.effects.regeneration

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import kotlin.reflect.KClass

/**
 * Executor for RegenerateEffect.
 * "Regenerate [permanent]" creates a one-shot regeneration shield that expires at end of turn.
 * The next time the permanent would be destroyed this turn, instead:
 * tap it, remove all damage, and remove it from combat.
 */
class RegenerateExecutor : EffectExecutor<RegenerateEffect> {

    override val effectType: KClass<RegenerateEffect> = RegenerateEffect::class

    override fun execute(
        state: GameState,
        effect: RegenerateEffect,
        context: EffectContext
    ): EffectResult {
        // State-aware resolution: "regenerate enchanted creature" (Thrull Retainer) names its host
        // through the attachment link, and only the [GameState] overload can follow one — the
        // context-only overload returns null for it, so such an ability produced no shield at all.
        // Once the Aura has sacrificed itself to pay for this very ability, the same overload reads
        // the host frozen into its battlefield-exit snapshot (CR 608.2h).
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "Could not resolve target for RegenerateEffect")

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RegenerationShield,
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
