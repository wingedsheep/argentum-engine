package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RevealTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for [RevealTargetEffect].
 *
 * Emits a [CardsRevealedEvent] for a single targeted entity. Does not move or
 * modify the card. The event's `source` is populated from the ability source's
 * card name so the reveal overlay labels the reveal with "— <SourceCardName>".
 *
 * If the target cannot be resolved (illegal target / fizzle), no event is emitted.
 */
class RevealTargetExecutor : EffectExecutor<RevealTargetEffect> {

    override val effectType: KClass<RevealTargetEffect> = RevealTargetEffect::class

    override fun execute(
        state: GameState,
        effect: RevealTargetEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        val cardComponent = state.getEntity(targetId)?.get<CardComponent>()
            ?: return EffectResult.success(state)

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val event = CardsRevealedEvent(
            revealingPlayerId = context.controllerId,
            cardIds = listOf(targetId),
            cardNames = listOf(cardComponent.name),
            imageUris = listOf(cardComponent.imageUri),
            source = sourceName
        )

        return EffectResult.success(state, listOf(event))
    }
}
