package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.sdk.scripting.effects.MarkSpellExileOnResolveEffect
import kotlin.reflect.KClass

/**
 * Executor for [MarkSpellExileOnResolveEffect].
 *
 * Tags the target spell on the stack with [AfterResolveDestinationComponent] carrying the effect's
 * [com.wingedsheep.sdk.scripting.effects.ExiledSpellRider] and the `onlyIfResolved` flag. The
 * marking ability's controller is the card's owner (checked below), so the owner controls the
 * delayed return. When the spell actually resolves,
 * [com.wingedsheep.engine.mechanics.stack.NonPermanentSpellResolver] sees the component, sends the
 * card to exile instead of the graveyard, and applies the rider (plot it, or schedule its return to
 * hand). If the spell is countered or fizzles, the component does not redirect it — it goes to its
 * owner's graveyard normally ("If you do").
 */
class MarkSpellExileOnResolveExecutor : EffectExecutor<MarkSpellExileOnResolveEffect> {

    override val effectType: KClass<MarkSpellExileOnResolveEffect> =
        MarkSpellExileOnResolveEffect::class

    override fun execute(
        state: GameState,
        effect: MarkSpellExileOnResolveEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target) ?: return EffectResult.success(state)
        // "…instead of putting it into *your* graveyard": a spell you cast but don't own goes to its
        // owner's graveyard, so there is nothing for this to replace (Feather / Lilah rulings).
        val ownerId = state.getEntity(targetId)?.get<CardComponent>()?.ownerId
        if (ownerId != context.controllerId) return EffectResult.success(state)
        val newState = state.updateEntity(targetId) { container ->
            val existing = container.get<AfterResolveDestinationComponent>()
            // An existing exile rider (a cast-this-way "exile it instead", Dreadhorde Arcanist) keeps
            // its own on-counter / on-fizzle reach; the follow-up below only ever runs on resolution.
            val merged = existing?.copy(
                exiledRider = effect.then
            ) ?: AfterResolveDestinationComponent(
                onlyIfResolved = true,
                exiledRider = effect.then
            )
            container.with(merged)
        }
        return EffectResult.success(newState)
    }
}
