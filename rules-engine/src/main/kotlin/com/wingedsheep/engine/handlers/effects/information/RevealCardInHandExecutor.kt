package com.wingedsheep.engine.handlers.effects.information

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.HandObjectRestrictions
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.HandObjectRestrictionKind
import com.wingedsheep.sdk.scripting.effects.RevealCardInHandEffect
import kotlin.reflect.KClass

/**
 * Executor for [RevealCardInHandEffect] — reveals one card in a hand to every player and pins that
 * knowledge to the hand object for the effect's duration (see [HandObjectRestrictions]).
 */
class RevealCardInHandExecutor(private val cardRegistry: CardRegistry) : EffectExecutor<RevealCardInHandEffect> {

    override val effectType: KClass<RevealCardInHandEffect> = RevealCardInHandEffect::class

    override fun execute(
        state: GameState,
        effect: RevealCardInHandEffect,
        context: EffectContext
    ): EffectResult {
        val cardId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        val (newState, events) = HandObjectRestrictions.apply(
            state, cardId, HandObjectRestrictionKind.REVEALED, effect.duration,
            effectControllerId = context.effectControllerId ?: context.controllerId,
            cardRegistry = cardRegistry
        ) ?: return EffectResult.error(state, "RevealCardInHand: unsupported duration ${effect.duration}")
        return EffectResult.success(newState, events)
    }
}
