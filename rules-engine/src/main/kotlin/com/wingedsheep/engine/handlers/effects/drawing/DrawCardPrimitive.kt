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
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition

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
 * When the card drawn is the first of the turn and has miracle, [Result.miracle] reports it; the
 * miracle reveal question (CR 702.94a) is the draw loop's to ask.
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
     * @property miracle the drawn card's miracle ability (printed or granted in hand) when it is the
     *     first card its owner drew this turn, i.e. when CR 702.94a lets them reveal it; the caller
     *     asks. Null otherwise.
     * @property optionalRevealSources the permanents whose optional [RevealFirstDrawEachTurn]
     *     applies to this first draw of the turn ("you may reveal … as you draw it"); the caller asks
     *     once per source. Empty otherwise.
     */
    data class Result(
        val state: GameState,
        val events: List<GameEvent>,
        val drawnCardId: EntityId?,
        val failed: Boolean,
        val miracle: com.wingedsheep.sdk.scripting.KeywordAbility.Miracle? = null,
        val optionalRevealSources: List<EntityId> = emptyList()
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
        var miracle: com.wingedsheep.sdk.scripting.KeywordAbility.Miracle? = null
        var optionalRevealSources = emptyList<EntityId>()
        if (drawCountBefore == 0) {
            val (mandatory, optional) = revealFirstDrawSources(newState, playerId).partition { !it.second }
            if (mandatory.isNotEmpty()) {
                firstDrawRevealEvent(newState, playerId, cardId, mandatory.map { it.first })?.let(events::add)
            }
            optionalRevealSources = optional.map { it.first }

            // Miracle (CR 702.94a): the first card drawn this turn may be revealed for its miracle
            // if it has one (printed or granted in hand). Revealing is its owner's choice, so this
            // only reports it — the draw loop asks before the next card is drawn.
            val cardDef = newState.getEntity(cardId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            miracle = com.wingedsheep.engine.mechanics.MiracleGrants.effectiveMiracle(
                newState, cardId, cardDef, playerId, cardRegistry, predicateEvaluator
            )
        }

        return Result(
            state = newState,
            events = events,
            drawnCardId = cardId,
            failed = false,
            miracle = miracle,
            optionalRevealSources = optionalRevealSources
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
     * The permanents [playerId] controls whose [RevealFirstDrawEachTurn] applies to their first draw
     * of the turn (a `yourTurnsOnly` one only on [playerId]'s turn), each paired with whether its
     * reveal is optional. Only called on the first draw of the turn.
     */
    private fun revealFirstDrawSources(
        state: GameState,
        playerId: EntityId
    ): List<Pair<EntityId, Boolean>> {
        val projected = state.projectedState
        val isTheirTurn = state.isActiveTurnFor(playerId)
        return projected.getBattlefieldControlledBy(playerId).mapNotNull { permanentId ->
            val cardDef = state.getEntity(permanentId)?.faceUpCardDefinition(cardRegistry) ?: return@mapNotNull null
            val reveals = cardDef.script.staticAbilities
                .filterIsInstance<RevealFirstDrawEachTurn>()
                .filter { !it.yourTurnsOnly || isTheirTurn }
            if (reveals.isEmpty()) null
            else permanentId to reveals.all { it.optional }
        }
    }

    companion object {
        /**
         * The [CardRevealedFromDrawEvent] for [drawnCardId], revealed by [revealedBy] — the
         * mandatory reveals as the card is drawn, or one optional source its controller said yes to.
         */
        fun firstDrawRevealEvent(
            state: GameState,
            playerId: EntityId,
            drawnCardId: EntityId,
            revealedBy: List<EntityId>
        ): CardRevealedFromDrawEvent? {
            val drawnCard = state.getEntity(drawnCardId)?.get<CardComponent>() ?: return null
            return CardRevealedFromDrawEvent(
                playerId = playerId,
                cardEntityId = drawnCardId,
                cardName = drawnCard.name,
                isCreature = drawnCard.typeLine.isCreature,
                revealedBy = revealedBy
            )
        }
    }
}
