package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.AttemptedDrawFromEmptyLibraryComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.RevealFirstDrawEachTurn

/**
 * Primitive single-card draw.
 *
 * Takes a [GameState] + player, moves the top card of that player's library to
 * their hand, increments [CardsDrawnThisTurnComponent], emits the exact object transition,
 * and emits [CardRevealedFromDrawEvent] if this is the first draw of the turn and the
 * player controls a permanent with [RevealFirstDrawEachTurn].
 *
 * Handles empty-library loss (Rule 704.5b) respecting [playerCantLoseGame] (Platinum Angel).
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
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) {
    /**
     * Result of a single [drawOne] call.
     *
     * @property state updated game state
     * @property events the zone transition and per-card reveal/failure events;
     *     does **not** include [com.wingedsheep.engine.core.CardsDrawnEvent]
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
            // CR 121.4: a player who attempts to draw from an empty library loses the game the
            // next time a player would receive priority — a state-based action (CR 704.5b), not
            // part of the draw. Record the attempt; EmptyLibraryDrawLossCheck turns it into the
            // loss (and honours "can't lose the game" grants as they stand at that moment). The
            // rest of the resolving effect still runs first, so "draw two cards. If your library
            // has no cards in it, you win the game" (Fblthp, Impossibly Lost) wins.
            val attemptedState = state.updateEntity(playerId) { container ->
                container.with(AttemptedDrawFromEmptyLibraryComponent)
            }
            return Result(
                state = attemptedState,
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

        val events = mutableListOf<GameEvent>(
            com.wingedsheep.engine.core.ZoneChangeEvent(
                entityId = cardId,
                entityName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown",
                fromZone = Zone.LIBRARY,
                toZone = Zone.HAND,
                ownerId = playerId,
                oldObject = state.objectRef(cardId),
                newObject = newState.objectRef(cardId)
            )
        )
        if (drawCountBefore == 0) {
            val revealEvent = checkRevealFirstDraw(newState, playerId, cardId)
            if (revealEvent != null) events.add(revealEvent)

            // Miracle (CR 702.94): if the first card drawn this turn has miracle (printed or granted
            // in hand), open its miracle window so its controller may cast it for the miracle cost
            // this turn, and reveal it (CR 702.94b). The window component is cleared at end of turn.
            val cardDef = newState.getEntity(cardId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            val miracle = com.wingedsheep.engine.mechanics.MiracleGrants.effectiveMiracle(
                newState, cardId, cardDef, playerId, cardRegistry, predicateEvaluator
            )
            if (miracle != null) {
                newState = newState.updateEntity(cardId) { c ->
                    c.with(
                        com.wingedsheep.engine.state.components.identity.MiracleWindowComponent(
                            controllerId = playerId,
                            turnOpened = newState.turnNumber
                        )
                    )
                }
                if (revealEvent == null) {
                    val drawn = newState.getEntity(cardId)?.get<CardComponent>()
                    events.add(
                        CardRevealedFromDrawEvent(
                            playerId = playerId,
                            cardEntityId = cardId,
                            cardName = drawn?.name ?: "card",
                            isCreature = drawn?.typeLine?.isCreature ?: false
                        )
                    )
                }
            }
        }

        return Result(
            state = newState,
            events = events,
            drawnCardId = cardId,
            failed = false
        )
    }

    /**
     * True when a [com.wingedsheep.sdk.scripting.RestrictCardsDrawnPerTurn] cap forbids
     * [playerId]'s next draw this turn (CR 121.2b) — see [DrawRestrictions].
     */
    fun drawProhibited(state: GameState, playerId: EntityId): Boolean =
        DrawRestrictions.cantDraw(state, playerId, cardRegistry, predicateEvaluator)

    /** How many more cards [playerId] may draw this turn, or `null` when nothing caps them. */
    fun drawsLeftThisTurn(state: GameState, playerId: EntityId): Int? =
        DrawRestrictions.drawsLeftThisTurn(state, playerId, cardRegistry, predicateEvaluator)

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
