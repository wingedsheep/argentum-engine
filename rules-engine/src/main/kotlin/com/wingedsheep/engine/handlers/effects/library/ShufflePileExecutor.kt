package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ShufflePileEffect
import kotlin.reflect.KClass

/**
 * Executor for [ShufflePileEffect]: joins the named collections into one pile (each card once),
 * shuffles it with the game's random generator, and stores it under [ShufflePileEffect.storeAs].
 *
 * Every card of the pile that is face down outside the library is re-identified
 * ([GameState.reidentify]) in the shuffled order, so the fresh ids — minted in sequence — say
 * nothing about which card went in where. The old ids are also rewritten in every other stored
 * collection, so a later step reading one of the source collections still finds its cards.
 */
class ShufflePileExecutor : EffectExecutor<ShufflePileEffect> {

    override val effectType: KClass<ShufflePileEffect> = ShufflePileEffect::class

    override fun execute(
        state: GameState,
        effect: ShufflePileEffect,
        context: EffectContext
    ): EffectResult {
        val stored = context.pipeline.storedCollections
        val pile = effect.from.flatMap { stored[it].orEmpty() }.distinct().filter { state.getEntity(it) != null }
        if (pile.isEmpty()) return EffectResult(state = state, updatedCollections = mapOf(effect.storeAs to emptyList()))

        val (shuffled, afterShuffle) = state.nextRandom { shuffle(pile) }
        var current = afterShuffle
        val renamed = mutableMapOf<EntityId, EntityId>()
        val result = shuffled.map { cardId ->
            if (!isHiddenFaceDown(current, cardId)) return@map cardId
            val (newId, next) = current.reidentify(cardId)
            current = next
            if (newId != cardId) renamed[cardId] = newId
            newId
        }

        val rewritten = if (renamed.isEmpty()) emptyMap() else stored
            .filterValues { ids -> ids.any { it in renamed } }
            .mapValues { (_, ids) -> ids.map { renamed[it] ?: it } }
        return EffectResult(state = current, updatedCollections = rewritten + (effect.storeAs to result))
    }

    private fun isHiddenFaceDown(state: GameState, cardId: EntityId): Boolean {
        val zone = state.logicalZone(cardId)?.zoneType ?: return false
        return zone != Zone.LIBRARY && zone != Zone.BATTLEFIELD && zone != Zone.STACK &&
            state.getEntity(cardId)?.has<FaceDownComponent>() == true
    }
}
