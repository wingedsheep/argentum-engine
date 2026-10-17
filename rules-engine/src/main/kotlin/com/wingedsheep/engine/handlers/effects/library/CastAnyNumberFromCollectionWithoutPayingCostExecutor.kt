package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.CastAnyNumberFromCollectionContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.land.PlayLandHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect
import kotlin.reflect.KClass

/**
 * Executor for [CastAnyNumberFromCollectionWithoutPayingCostEffect].
 *
 * Drives the "cast any number of spells from among them for free" loop **during resolution**.
 * One invocation = one loop iteration: it presents the controller a 0..1 [SelectCardsDecision]
 * over the cards in the collection that are still castable from where they were gathered — their
 * owner's exile, hand or graveyard — then pauses. The actual cast and
 * the next iteration are run by `resumeCastAnyNumberFromCollection`
 * ([com.wingedsheep.engine.handlers.continuations.LibraryAndZoneContinuationResumer]), which
 * re-enters this effect over the remaining cards.
 *
 * Empty / all-gone collection → no-op (the loop ends). The collection must already be the
 * eligible set — callers filter it (nonland, mana value ≤ X, …) upstream; this effect casts
 * whatever it's handed and leaves uncast cards in place. Timing restrictions based on card type
 * are ignored because the synthesized casts go through `CastSpellHandler.execute` directly
 * (the player-action `validate()` timing gate never runs), exactly like Cascade.
 *
 * A non-null `maxCasts` is the remaining budget for the "cast **up to N** spells from among
 * them" wording: an exhausted budget (`<= 0`) ends the loop before the decision is offered, and
 * the budget rides on the continuation so the resumer can re-enter with one fewer once a cast
 * actually initiates (a pick that can't be cast for want of a legal target doesn't spend one).
 *
 * A non-null `maxTotalManaValue` is the remaining budget for the "spells with total mana value N
 * or less" wording: only cards whose mana value fits are offered, and the resumer re-enters with
 * the cast card's mana value spent (again only once the cast actually initiates).
 *
 * With `playLands` ("play lands and cast spells from among them" — Gix, Yawgmoth Praetor) land
 * cards are offered too, but only while the controller could play one right now (their turn, a
 * land play left, no lock — asked of the land-play pipeline itself). Without it, land cards are
 * offered exactly as before: callers filter them out upstream.
 */
class CastAnyNumberFromCollectionWithoutPayingCostExecutor(
    private val playLandHandlerProvider: () -> PlayLandHandler,
) : EffectExecutor<CastAnyNumberFromCollectionWithoutPayingCostEffect> {

    override val effectType: KClass<CastAnyNumberFromCollectionWithoutPayingCostEffect> =
        CastAnyNumberFromCollectionWithoutPayingCostEffect::class

    override fun execute(
        state: GameState,
        effect: CastAnyNumberFromCollectionWithoutPayingCostEffect,
        context: EffectContext,
    ): EffectResult {
        val remainingCasts = effect.maxCasts
        if (remainingCasts != null && remainingCasts <= 0) return EffectResult.success(state)

        val remainingManaValue = effect.maxTotalManaValue
        if (remainingManaValue != null && remainingManaValue < 0) return EffectResult.success(state)

        // A "total mana value N or less" budget offers only the cards that still fit in it; a card
        // that doesn't fit now never will (the budget only shrinks), so it leaves the pool.
        val candidates = stillCastable(state, context.pipeline.storedCollections[effect.from].orEmpty())
            .filter { id -> remainingManaValue == null || manaValueOf(state, id) <= remainingManaValue }
            .filter { id ->
                // A land is never cast; under "play lands" it is offered only while it could be
                // played now. A land the controller can't play stays out of the pool.
                !effect.playLands || !isLand(state, id) ||
                    PlayFromCollectionWithoutPayingCostExecutor.canPlayLandNow(
                        state, playLandHandlerProvider(), context.controllerId, context.sourceId, id
                    )
            }
        if (candidates.isEmpty()) return EffectResult.success(state)

        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val cardInfo = candidates.associateWith { cardId ->
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null,
                colors = cardComponent?.colors?.map { it.name } ?: emptyList(),
            )
        }

        val decision = { decisionId: String -> SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = buildString {
                append(
                    when {
                        effect.playLands -> "Choose a land to play or a spell to cast for free"
                        effect.payManaCost -> "Choose a spell to cast"
                        else -> "Choose a spell to cast for free"
                    }
                )
                // "$n remaining", not "$n more": the card being chosen right now is one of them.
                if (remainingCasts != null) append(" ($remainingCasts remaining)")
                if (remainingManaValue != null) append(" (total mana value $remainingManaValue remaining)")
                append(", or select none to stop")
            },
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING,
            ),
            options = candidates,
            minSelections = 0,
            maxSelections = 1,
            cardInfo = cardInfo,
        ) }

        // Normalize the collection to the still-castable set so the resumer's bookkeeping
        // (remaining = collection − chosen) matches exactly what was offered.
        val normalizedContext = context.copy(
            pipeline = context.pipeline.copy(
                storedCollections = context.pipeline.storedCollections + (effect.from to candidates)
            )
        )

        val continuation = CastAnyNumberFromCollectionContinuation(
            from = effect.from,
            effectContext = normalizedContext,
            payManaCost = effect.payManaCost,
            maxCasts = effect.maxCasts,
            maxTotalManaValue = effect.maxTotalManaValue,
            playLands = effect.playLands,
            insteadOfGraveyard = effect.insteadOfGraveyard,
        )

        return EffectResult.from(state.withPriority(controllerId).suspendForDecision(decision, continuation, emptyList()))
    }

    companion object {
        private val CASTABLE_ZONES = listOf(Zone.EXILE, Zone.HAND, Zone.GRAVEYARD)

        /** A card's mana value off the stack — X counts as 0, which a free cast also forces (CR 107.3b). */
        fun manaValueOf(state: GameState, id: EntityId): Int =
            state.getEntity(id)?.get<CardComponent>()?.manaValue ?: 0

        fun isLand(state: GameState, id: EntityId): Boolean =
            state.getEntity(id)?.get<CardComponent>()?.typeLine?.isLand == true
    }

    /**
     * Cards from the collection that are still in their owner's exile, hand or graveyard — the
     * zones a synthesized cast takes a card from. A card cast earlier in the loop (now on the stack)
     * or moved anywhere else since it was gathered drops out.
     */
    private fun stillCastable(state: GameState, ids: List<EntityId>): List<EntityId> =
        ids.filter { id ->
            val ownerId = state.getEntity(id)?.get<OwnerComponent>()?.playerId ?: return@filter false
            CASTABLE_ZONES.any { zone -> id in state.getZone(ZoneKey(ownerId, zone)) }
        }
}
