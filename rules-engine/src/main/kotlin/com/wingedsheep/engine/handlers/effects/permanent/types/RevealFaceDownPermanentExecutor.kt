package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.scripting.effects.RevealFaceDownPermanentEffect
import kotlin.reflect.KClass

/**
 * Executor for [RevealFaceDownPermanentEffect].
 *
 * Reveals the card hidden under a face-down permanent — makes its identity public (CR 708.2: a
 * face-down permanent's true characteristics are normally hidden; "reveal" shows them to all
 * players). This is informational only: it does not turn the permanent face up and does not change
 * any game state. It emits a [CardsRevealedEvent] so the UI shows what the permanent is.
 *
 * If the target is no longer face down by the time this resolves (already turned up, or left the
 * battlefield), the reveal is a harmless no-op.
 */
class RevealFaceDownPermanentExecutor : EffectExecutor<RevealFaceDownPermanentEffect> {

    override val effectType: KClass<RevealFaceDownPermanentEffect> =
        RevealFaceDownPermanentEffect::class

    override fun execute(
        state: GameState,
        effect: RevealFaceDownPermanentEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        val container = state.getEntity(targetId)
            ?: return EffectResult.success(state)

        // Only meaningful while the permanent is still face down.
        if (!container.has<FaceDownComponent>()) {
            return EffectResult.success(state)
        }

        val card = container.get<CardComponent>()
            ?: return EffectResult.success(state)
        val ownerId = container.get<OwnerComponent>()?.playerId ?: context.controllerId

        return EffectResult.success(
            state,
            listOf(
                CardsRevealedEvent(
                    revealingPlayerId = ownerId,
                    cardIds = listOf(targetId),
                    cardNames = listOf(card.name),
                    imageUris = listOf(card.imageUri),
                    source = "Reveal"
                )
            )
        )
    }
}
