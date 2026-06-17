package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.MakePreparedEffect
import kotlin.reflect.KClass

/**
 * Executor for [MakePreparedEffect] (Prepare — Secrets of Strixhaven).
 *
 * Resolves the target permanent, looks up its card definition (whose `cardFaces[0]` is the prepare
 * spell), and delegates to [PreparedService.makePrepared] — the same machinery that runs when a
 * `CardLayout.PREPARE` permanent enters prepared. The controller who may cast the exiled copy is the
 * effect controller ([EffectContext.controllerId]).
 *
 * No-op when: the target can't be resolved, isn't a real card on the battlefield, has no prepare
 * face, or is already prepared (the service guards the last case so a creature doesn't become
 * prepared twice).
 */
class MakePreparedEffectExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<MakePreparedEffect> {

    override val effectType: KClass<MakePreparedEffect> = MakePreparedEffect::class

    override fun execute(
        state: GameState,
        effect: MakePreparedEffect,
        context: EffectContext
    ): EffectResult {
        val permanentId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)
        val cardComponent = state.getEntity(permanentId)?.get<CardComponent>()
            ?: return EffectResult.success(state)
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            ?: return EffectResult.success(state)

        val newState = PreparedService.makePrepared(state, permanentId, cardDef, context.controllerId)
        return EffectResult.success(newState)
    }
}
