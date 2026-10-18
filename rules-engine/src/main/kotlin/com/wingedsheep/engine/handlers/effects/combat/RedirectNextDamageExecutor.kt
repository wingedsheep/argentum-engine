package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RedirectNextDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for RedirectNextDamageEffect.
 * "The next time damage would be dealt to [protected targets] this turn,
 *  that damage is dealt to [redirectTo] instead."
 *
 * Creates a floating effect with a RedirectNextDamage shield that is consumed
 * when damage is first dealt to any protected entity. The shield expires at end of turn.
 */
class RedirectNextDamageExecutor : EffectExecutor<RedirectNextDamageEffect> {

    override val effectType: KClass<RedirectNextDamageEffect> = RedirectNextDamageEffect::class

    override fun execute(
        state: GameState,
        effect: RedirectNextDamageEffect,
        context: EffectContext
    ): EffectResult {
        val redirectToId = context.resolveTarget(effect.redirectTo, state)
            ?: return EffectResult.error(state, "Could not resolve redirect target for RedirectNextDamageEffect")

        val protectedIds = effect.protectedTargets.mapNotNull { target ->
            context.resolveTarget(target, state)
        }.toSet()

        // "By target attacking creature" / "by target sorcery spell": the shield is keyed to that
        // one object, through the same chosen-source path RedirectDamageFromChosenSource uses. A
        // source that is already gone (a fizzled target) leaves nothing to redirect.
        val fromSource = effect.fromSource?.let { sourceTarget ->
            val sourceId = context.resolveTarget(sourceTarget, state) ?: return EffectResult.success(state)
            val ref = state.objectRef(sourceId) ?: return EffectResult.success(state)
            ChosenDamageSource(ref, state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Source")
        }

        // A creatures-only shield protects a *class*, not a list, so it carries no protected ids
        // and must not be rejected for having none (Blood of the Martyr). A source-keyed shield
        // with no protected targets covers every recipient (Reverberation).
        val coversAnyRecipient = effect.fromSource != null && effect.protectedTargets.isEmpty()
        if (protectedIds.isEmpty() && !effect.creaturesOnly && !coversAnyRecipient) {
            return EffectResult.error(state, "Could not resolve any protected targets for RedirectNextDamageEffect")
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RedirectNextDamage(
                redirectToId, effect.amount, effect.scope, effect.creaturesOnly, effect.optional,
                chosenSource = fromSource,
                // Keyed to objects, as RedirectDamageFromChosenSource's shield is: a protected or
                // redirect-to permanent that leaves the battlefield ends the shield (CR 400.7).
                protectedRef = if (fromSource != null) protectedIds.singleOrNull()?.let(state::objectRef) else null,
                redirectToRef = if (fromSource != null) state.objectRef(redirectToId) else null
            ),
            affectedEntities = protectedIds,
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
