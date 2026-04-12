package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import kotlin.reflect.KClass

/**
 * Executor for [RevealCollectionEffect].
 *
 * Emits a [CardsRevealedEvent] for all cards in the named stored collection.
 * Does not move or modify any cards — purely an event/information step.
 *
 * If the collection is empty, no event is emitted.
 */
class RevealCollectionExecutor : EffectExecutor<RevealCollectionEffect> {

    override val effectType: KClass<RevealCollectionEffect> = RevealCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: RevealCollectionEffect,
        context: EffectContext
    ): EffectResult {
        val cards = context.pipeline.storedCollections[effect.from]
            ?: return EffectResult.error(state, "No collection named '${effect.from}' in storedCollections")

        if (cards.isEmpty()) {
            return EffectResult.success(state)
        }

        val cardNames = cards.map { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
        }
        val imageUris = cards.map { cardId ->
            state.getEntity(cardId)?.get<CardComponent>()?.imageUri
        }
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val event = CardsRevealedEvent(
            revealingPlayerId = context.controllerId,
            cardIds = cards,
            cardNames = cardNames,
            imageUris = imageUris,
            source = sourceName
        )

        return EffectResult.success(state, listOf(event))
    }
}
