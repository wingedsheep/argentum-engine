package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RemoveKeywordEffect
import kotlin.reflect.KClass

/**
 * Executor for RemoveKeywordEffect.
 * "All other creatures lose flying until end of turn."
 *
 * Mirrors [com.wingedsheep.engine.handlers.effects.permanent.abilities.GrantKeywordExecutor]'s
 * target requirement: any battlefield permanent, not specifically a creature. Removal is meaningful
 * on a noncreature permanent — Spectacular Pileup's "All creatures **and Vehicles** lose
 * indestructible until end of turn" has to strip the keyword from an *uncrewed* Vehicle, which is
 * an artifact and not a creature at that moment. A creature-only guard here silently exempted
 * exactly the permanents such a card names. Removing a keyword the permanent doesn't have is a
 * harmless no-op in projection, so the permissive guard costs nothing.
 */
class RemoveKeywordExecutor : EffectExecutor<RemoveKeywordEffect> {

    override val effectType: KClass<RemoveKeywordEffect> = RemoveKeywordEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveKeywordEffect,
        context: EffectContext
    ): EffectResult {
        // The two-arg overload is required: the one-arg form can't resolve attachment-relative
        // targets (EffectTarget.AttachedTo and friends).
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for keyword removal")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target permanent no longer exists")
        targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        if (targetId !in state.getBattlefield()) {
            return EffectResult.error(state, "Target is no longer on the battlefield")
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RemoveKeyword(effect.keyword),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
