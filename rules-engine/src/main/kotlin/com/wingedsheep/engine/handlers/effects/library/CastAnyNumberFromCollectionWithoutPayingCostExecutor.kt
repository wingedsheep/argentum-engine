package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.CastAnyNumberFromCollectionContinuation
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.land.PlayLandHandler
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
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
 *
 * With `storeCastTo` the loop keeps a running record of what it cast. Each iteration's single
 * cast publishes its card under a private per-iteration key ([lastCastKey]) only when the cast
 * initiates; the next iteration folds that into the record so far ([castSoFarKey]) and clears it,
 * and every exit of the loop — including the very first, when nothing is offered — publishes the
 * record under `storeCastTo`, so the step after the loop sees exactly the spells actually cast.
 * The record travels under its own key rather than `storeCastTo` because a composite hands on only
 * the keys it added: a key already in the context a resumed iteration starts from would be dropped.
 */
class CastAnyNumberFromCollectionWithoutPayingCostExecutor(
    private val playLandHandlerProvider: () -> PlayLandHandler,
    private val castSpellHandlerProvider: () -> CastSpellHandler,
) : EffectExecutor<CastAnyNumberFromCollectionWithoutPayingCostEffect> {

    override val effectType: KClass<CastAnyNumberFromCollectionWithoutPayingCostEffect> =
        CastAnyNumberFromCollectionWithoutPayingCostEffect::class

    override fun execute(
        state: GameState,
        effect: CastAnyNumberFromCollectionWithoutPayingCostEffect,
        context: EffectContext,
    ): EffectResult {
        // The running "cast this way" record: what earlier iterations cast, plus the card the
        // previous iteration's single cast just published (absent when that cast never initiated).
        val castSoFar = effect.storeCastTo?.let { key ->
            val collections = context.pipeline.storedCollections
            collections[castSoFarKey(key)].orEmpty() + collections[lastCastKey(key)].orEmpty()
        }
        val recorded = effect.storeCastTo?.let { key -> mapOf(key to castSoFar.orEmpty()) }.orEmpty()
        val done = EffectResult.success(state).copy(updatedCollections = recorded)

        val remainingCasts = effect.maxCasts
        if (remainingCasts != null && remainingCasts <= 0) return done

        val remainingManaValue = effect.maxTotalManaValue
        if (remainingManaValue != null && remainingManaValue < 0) return done

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
            .filter { id ->
                // A spell a cast prohibition forbids (Drannith Magistrate) isn't offered at all.
                isLand(state, id) ||
                    castSpellHandlerProvider().resolutionCastProhibition(state, CastSpell(context.controllerId, id)) == null
            }
        if (candidates.isEmpty()) return done

        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val cardInfo = candidates.associateWith { cardId ->
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null,
                colors = if (cardComponent == null) emptyList() else state.projectedState.colorsOf(cardId).toList(),
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
        // The cast record rides along too, folded and with the per-iteration key cleared, so a
        // decline publishes it and the next cast's publication is the only thing under that key.
        val normalizedContext = context.copy(
            pipeline = context.pipeline.copy(
                storedCollections = context.pipeline.storedCollections + (effect.from to candidates) +
                    effect.storeCastTo?.let { key -> mapOf(castSoFarKey(key) to castSoFar.orEmpty()) }.orEmpty() -
                    listOfNotNull(effect.storeCastTo?.let(::lastCastKey)).toSet()
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
            storeCastTo = effect.storeCastTo,
        )

        return EffectResult.from(state.withPriority(controllerId).suspendForDecision(decision, continuation, emptyList()))
    }

    companion object {
        private val CASTABLE_ZONES = listOf(Zone.EXILE, Zone.HAND, Zone.GRAVEYARD)

        /** A card's mana value off the stack — X counts as 0, which a free cast also forces (CR 107.3b). */
        fun manaValueOf(state: GameState, id: EntityId): Int =
            state.getEntity(id)?.get<CardComponent>()?.manaValue ?: 0

        /** Where one iteration's single cast publishes its card, for the next iteration to fold in. */
        fun lastCastKey(storeCastTo: String): String = "$storeCastTo\$lastCast"

        /** Where the loop carries what earlier iterations cast, between a pause and its resume. */
        fun castSoFarKey(storeCastTo: String): String = "$storeCastTo\$castSoFar"

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
