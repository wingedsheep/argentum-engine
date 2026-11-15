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
 * nothing about which card went in where, and the pile's cards are dealt back into the zone slots
 * they occupy in that shuffled order, so their positions say nothing either. The old ids are also
 * rewritten in every other stored collection of the same pipeline block, so a later step of that
 * block reading one of the source collections still finds its cards.
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

        // Renaming keeps each card in its slot, so a pile left where it is (say, in exile) would still
        // line up old ids with new ones by position: deal the shuffled pile back into those slots.
        for ((zone, members) in result.groupBy { current.logicalZone(it) }) {
            if (zone == null || zone.zoneType in UNSHUFFLED_ZONES || members.size < 2) continue
            val contents = current.getZone(zone)
            val slots = contents.indices.filter { contents[it] in members }
            if (slots.size != members.size) continue
            val reordered = contents.toMutableList()
            slots.forEachIndexed { i, slot -> reordered[slot] = members[i] }
            current = current.reorderZone(zone, reordered)
        }

        val rewritten = if (renamed.isEmpty()) emptyMap() else stored
            .filterValues { ids -> ids.any { it in renamed } }
            .mapValues { (_, ids) -> ids.map { renamed[it] ?: it } }
        return EffectResult(state = current, updatedCollections = rewritten + (effect.storeAs to result))
    }

    private companion object {
        /** A library keeps its own order; permanents and spells keep their places (and their ids). */
        val UNSHUFFLED_ZONES = setOf(Zone.LIBRARY, Zone.BATTLEFIELD, Zone.STACK)
    }

    private fun isHiddenFaceDown(state: GameState, cardId: EntityId): Boolean {
        val zone = state.logicalZone(cardId)?.zoneType ?: return false
        return zone != Zone.LIBRARY && zone != Zone.BATTLEFIELD && zone != Zone.STACK &&
            state.getEntity(cardId)?.has<FaceDownComponent>() == true
    }
}
