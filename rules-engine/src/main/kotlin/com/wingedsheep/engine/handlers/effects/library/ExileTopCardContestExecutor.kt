package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ExileTopCardContestEffect
import kotlin.reflect.KClass

/**
 * Executor for [ExileTopCardContestEffect].
 *
 * Runs the contest to completion — there are no player choices in it, so it never pauses. Each
 * round every remaining contender exiles the top card of their library face up (the cards stay in
 * exile); the greatest mana value wins. A tie narrows the field to the tied players and the round
 * repeats, which is what Timesifter's "the tied players repeat this process until the tie is
 * broken" asks for.
 *
 * Two ways the contest ends without a winner, both leaving [ExileTopCardContestEffect.storeWinnerAs]
 * empty rather than picking arbitrarily: nobody is in the contest at all, and no contender could
 * exile a card (every remaining library is empty). A player with an empty library exiles nothing
 * and therefore can't be the player who exiled the greatest mana value — so a round in which only
 * one contender can still exile resolves in that player's favour.
 *
 * Termination: a round either exiles at least one card — shrinking a library — or ends the contest,
 * so the loop is bounded by the total number of cards in the contenders' libraries.
 */
class ExileTopCardContestExecutor : EffectExecutor<ExileTopCardContestEffect> {

    override val effectType: KClass<ExileTopCardContestEffect> = ExileTopCardContestEffect::class

    override fun execute(
        state: GameState,
        effect: ExileTopCardContestEffect,
        context: EffectContext
    ): EffectResult {
        var currentState = state
        val events = mutableListOf<EngineGameEvent>()
        val allExiled = mutableListOf<EntityId>()
        var winner: EntityId? = null

        var contenders = context.resolvePlayerTargets(
            com.wingedsheep.sdk.scripting.targets.EffectTarget.PlayerRef(effect.players),
            currentState
        )

        while (contenders.isNotEmpty()) {
            // Mana value of the card each contender exiled this round; a contender whose library is
            // empty exiles nothing and simply doesn't appear.
            val exiledThisRound = mutableMapOf<EntityId, Int>()

            for (playerId in contenders) {
                val topCardId = currentState.getZone(ZoneKey(playerId, Zone.LIBRARY)).firstOrNull() ?: continue
                val card = currentState.getEntity(topCardId)?.get<CardComponent>()
                val moveResult = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                    .moveCardToZone(currentState, topCardId, Zone.EXILE)
                if (!moveResult.isSuccess) continue
                currentState = moveResult.state
                events.addAll(moveResult.events)
                allExiled.add(topCardId)
                exiledThisRound[playerId] = card?.manaValue ?: 0
                events.add(
                    CardsRevealedEvent(
                        revealingPlayerId = playerId,
                        cardIds = listOf(topCardId),
                        cardNames = listOf(card?.name ?: "Unknown"),
                        imageUris = listOf(card?.imageUri),
                        source = context.sourceId?.let { currentState.getEntity(it)?.get<CardComponent>()?.name }
                    )
                )
            }

            if (exiledThisRound.isEmpty()) break

            val greatest = exiledThisRound.values.max()
            val tied = exiledThisRound.filterValues { it == greatest }.keys.toList()
            if (tied.size == 1) {
                winner = tied.single()
                break
            }
            contenders = tied
        }

        return EffectResult.success(currentState, events).copy(
            updatedCollections = mapOf(
                effect.storeWinnerAs to listOfNotNull(winner),
                effect.storeExiledAs to allExiled.toList()
            )
        )
    }
}
