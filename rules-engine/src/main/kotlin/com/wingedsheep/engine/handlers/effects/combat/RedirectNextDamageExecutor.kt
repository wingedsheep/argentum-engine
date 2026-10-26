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
 *  that damage is dealt to [redirectTo] instead" — or "all damage … until your next turn".
 *
 * Creates a floating effect with a RedirectNextDamage shield. When it is used up depends on the
 * effect's amount and [com.wingedsheep.sdk.scripting.effects.RedirectScope]; it expires with the
 * effect's duration ("this turn" by default), or as soon as its recipient leaves the battlefield.
 */
class RedirectNextDamageExecutor : EffectExecutor<RedirectNextDamageEffect> {

    override val effectType: KClass<RedirectNextDamageEffect> = RedirectNextDamageEffect::class

    override fun execute(
        state: GameState,
        effect: RedirectNextDamageEffect,
        context: EffectContext
    ): EffectResult {
        // No recipient — an illegal target, or nothing to choose ("choose a creature you control"
        // with none) — leaves nothing to redirect to, so the shield is never created.
        val redirectToId = context.resolveTarget(effect.redirectTo, state)
            ?: return EffectResult.success(state)

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

        // A filter shield protects a *class*, not a list, so it carries no protected ids and must
        // not be rejected for having none (Blood of the Martyr). A source-keyed shield
        // with no protected targets covers every recipient (Reverberation).
        val coversAnyRecipient = effect.fromSource != null && effect.protectedTargets.isEmpty()
        if (protectedIds.isEmpty() && effect.protectedFilter == null && !coversAnyRecipient) {
            return EffectResult.error(state, "Could not resolve any protected targets for RedirectNextDamageEffect")
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RedirectNextDamage(
                redirectToId, effect.amount, effect.scope, effect.protectedFilter, effect.optional,
                chosenSource = fromSource,
                // Keyed to objects, as RedirectDamageFromChosenSource's shield is: a protected or
                // redirect-to permanent that leaves the battlefield ends the shield (CR 400.7) — a
                // flickered "that creature" comes back as a new object and soaks nothing.
                protectedRef = if (fromSource != null) protectedIds.singleOrNull()?.let(state::objectRef) else null,
                redirectToRef = state.objectRef(redirectToId)
            ),
            affectedEntities = protectedIds,
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
