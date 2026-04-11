package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn

/**
 * Primitive single-card draw.
 *
 * Takes a [GameState] + player, moves the top card of that player's library to
 * their hand, increments [CardsDrawnThisTurnComponent], and emits
 * [CardRevealedFromDrawEvent] if this is the first draw of the turn and the
 * player controls a permanent with [RevealFirstDrawEachTurn].
 *
 * Handles empty-library loss (Rule 704.5c) respecting
 * [GrantsCantLoseGameComponent] (Platinum Angel).
 *
 * Does **not** emit [com.wingedsheep.engine.core.CardsDrawnEvent] — the driver
 * aggregates drawn cards across multiple calls and emits a single
 * `CardsDrawnEvent` for the whole batch.
 *
 * No loops, no replacement logic, no prompts. This is the shared primitive
 * that both spell/ability draws ([DrawCardsExecutor]) and the draw-step
 * draw ([com.wingedsheep.engine.core.DrawPhaseManager]) call into.
 */
class DrawCardPrimitive(
    private val cardRegistry: CardRegistry
) {
    /**
     * Result of a single [drawOne] call.
     *
     * @property state updated game state
     * @property events per-card side events (currently only [CardRevealedFromDrawEvent]
     *     or [DrawFailedEvent]); does **not** include [com.wingedsheep.engine.core.CardsDrawnEvent]
     * @property drawnCardId the id of the drawn card, or `null` if the draw failed
     * @property failed true if the library was empty and the draw failed
     */
    data class Result(
        val state: GameState,
        val events: List<GameEvent>,
        val drawnCardId: EntityId?,
        val failed: Boolean
    )

    /**
     * Draw one card from the top of [playerId]'s library into their hand.
     *
     * @param emptyLibraryReason string included in the [DrawFailedEvent] when
     *     the library is empty. Historically the spell/ability path used
     *     `"Empty library"` and the draw-step path used `"Library is empty"`;
     *     callers should pass the same value to preserve existing test
     *     assertions.
     */
    fun drawOne(
        state: GameState,
        playerId: EntityId,
        emptyLibraryReason: String = "Empty library"
    ): Result {
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val handZone = ZoneKey(playerId, Zone.HAND)
        val library = state.getZone(libraryZone)

        if (library.isEmpty()) {
            // Rule 704.5c: failed to draw from an empty library → lose the game,
            // unless a controlled permanent grants "can't lose the game" (Platinum Angel).
            val cantLose = state.getBattlefield().any { entityId ->
                val c = state.getEntity(entityId) ?: return@any false
                c.has<GrantsCantLoseGameComponent>() &&
                    c.get<ControllerComponent>()?.playerId == playerId
            }
            val lostState = if (cantLose) {
                state
            } else {
                state.updateEntity(playerId) { container ->
                    container.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY))
                }
            }
            return Result(
                state = lostState,
                events = listOf(DrawFailedEvent(playerId, emptyLibraryReason)),
                drawnCardId = null,
                failed = true
            )
        }

        // Move top card from library → hand.
        val cardId = library.first()
        var newState = state.removeFromZone(libraryZone, cardId)
        newState = newState.addToZone(handZone, cardId)

        // Track cards-drawn-this-turn and emit reveal on the first draw of the turn.
        val drawCountBefore = newState.getEntity(playerId)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0
        newState = newState.updateEntity(playerId) { container ->
            container.with(CardsDrawnThisTurnComponent(count = drawCountBefore + 1))
        }

        val events = mutableListOf<GameEvent>()
        if (drawCountBefore == 0) {
            val revealEvent = checkRevealFirstDraw(newState, playerId, cardId)
            if (revealEvent != null) events.add(revealEvent)
        }

        return Result(
            state = newState,
            events = events,
            drawnCardId = cardId,
            failed = false
        )
    }

    /**
     * If [playerId] controls a permanent with [RevealFirstDrawEachTurn], emit a
     * [CardRevealedFromDrawEvent] for [drawnCardId]. Only called on the first
     * draw of the turn.
     */
    private fun checkRevealFirstDraw(
        state: GameState,
        playerId: EntityId,
        drawnCardId: EntityId
    ): CardRevealedFromDrawEvent? {
        val projected = state.projectedState
        val hasRevealAbility = projected.getBattlefieldControlledBy(playerId).any { permanentId ->
            val card = state.getEntity(permanentId)?.get<CardComponent>() ?: return@any false
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return@any false
            cardDef.script.staticAbilities.any { it is RevealFirstDrawEachTurn }
        }
        if (!hasRevealAbility) return null

        val drawnCard = state.getEntity(drawnCardId)?.get<CardComponent>() ?: return null
        return CardRevealedFromDrawEvent(
            playerId = playerId,
            cardEntityId = drawnCardId,
            cardName = drawnCard.name,
            isCreature = drawnCard.typeLine.isCreature
        )
    }
}
