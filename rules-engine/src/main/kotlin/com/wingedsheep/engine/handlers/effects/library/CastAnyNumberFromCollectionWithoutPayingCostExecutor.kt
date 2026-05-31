package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CastAnyNumberFromCollectionContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.SearchCardInfo
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [CastAnyNumberFromCollectionWithoutPayingCostEffect].
 *
 * Drives the "cast any number of spells from among them for free" loop **during resolution**.
 * One invocation = one loop iteration: it presents the controller a 0..1 [SelectCardsDecision]
 * over the cards in the collection that are still in exile, then pauses. The actual cast and
 * the next iteration are run by `resumeCastAnyNumberFromCollection`
 * ([com.wingedsheep.engine.handlers.continuations.LibraryAndZoneContinuationResumer]), which
 * re-enters this effect over the remaining cards.
 *
 * Empty / all-gone collection → no-op (the loop ends). The collection must already be the
 * eligible set — callers filter it (nonland, mana value ≤ X, …) upstream; this effect casts
 * whatever it's handed and leaves uncast cards in place. Timing restrictions based on card type
 * are ignored because the synthesized casts go through `CastSpellHandler.execute` directly
 * (the player-action `validate()` timing gate never runs), exactly like Cascade.
 */
class CastAnyNumberFromCollectionWithoutPayingCostExecutor :
    EffectExecutor<CastAnyNumberFromCollectionWithoutPayingCostEffect> {

    override val effectType: KClass<CastAnyNumberFromCollectionWithoutPayingCostEffect> =
        CastAnyNumberFromCollectionWithoutPayingCostEffect::class

    override fun execute(
        state: GameState,
        effect: CastAnyNumberFromCollectionWithoutPayingCostEffect,
        context: EffectContext,
    ): EffectResult {
        val candidates = stillCastable(state, context.pipeline.storedCollections[effect.from].orEmpty())
        if (candidates.isEmpty()) return EffectResult.success(state)

        val controllerId = context.controllerId
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val decisionId = UUID.randomUUID().toString()

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

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a spell to cast for free, or select none to stop",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING,
            ),
            options = candidates,
            minSelections = 0,
            maxSelections = 1,
            cardInfo = cardInfo,
        )

        // Normalize the collection to the still-castable set so the resumer's bookkeeping
        // (remaining = collection − chosen) matches exactly what was offered.
        val normalizedContext = context.copy(
            pipeline = context.pipeline.copy(
                storedCollections = context.pipeline.storedCollections + (effect.from to candidates)
            )
        )

        val continuation = CastAnyNumberFromCollectionContinuation(
            decisionId = decisionId,
            from = effect.from,
            effectContext = normalizedContext,
        )

        val pausedState = state
            .pushContinuation(continuation)
            .withPendingDecision(decision)
            .withPriority(controllerId)

        return EffectResult.paused(
            pausedState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt,
                )
            ),
        )
    }

    /** Cards from the collection that are still in their owner's exile (castable from there). */
    private fun stillCastable(state: GameState, ids: List<EntityId>): List<EntityId> =
        ids.filter { id ->
            val ownerId = state.getEntity(id)?.get<OwnerComponent>()?.playerId ?: return@filter false
            id in state.getZone(ZoneKey(ownerId, Zone.EXILE))
        }
}
