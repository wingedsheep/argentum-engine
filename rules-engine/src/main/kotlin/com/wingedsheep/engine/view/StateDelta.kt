package com.wingedsheep.engine.view

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Delta representation of a ClientGameState change.
 *
 * Only changed fields are populated. Null fields mean "unchanged from previous state".
 * The client applies this delta to its local copy of ClientGameState.
 */
@Serializable
data class StateDelta(
    /** Cards added since last update (new entities becoming visible) */
    val addedCards: Map<EntityId, ClientCard>? = null,

    /** Card IDs removed since last update (entities leaving visibility) */
    val removedCardIds: List<EntityId>? = null,

    /** Cards whose data changed since last update (full card sent) */
    val updatedCards: Map<EntityId, ClientCard>? = null,

    /** Zones whose contents changed (by zoneId key) */
    val updatedZones: List<ClientZone>? = null,

    /** Player info — always included (small, nearly always changes) */
    val players: List<ClientPlayer>,

    /** Scalars — only included if changed */
    val currentPhase: Phase? = null,
    val currentStep: Step? = null,
    val activePlayerId: EntityId? = null,
    val priorityPlayerId: EntityId? = null,
    val turnNumber: Int? = null,
    val isGameOver: Boolean? = null,
    val winnerId: EntityId? = null,

    /**
     * Day/night designation (CR 731). `null` means "unchanged" — which is unambiguous here because the
     * game never returns to the neither state once a designation is gained (CR 731.1), so a real change
     * is always to a non-null value.
     */
    val dayNight: com.wingedsheep.sdk.core.DayNight? = null,

    /** Combat state. Present = changed. combatCleared = true means combat ended (set to null). */
    val combat: ClientCombatState? = null,
    val combatCleared: Boolean? = null,

    /** New game log entries only (append to existing) */
    val newLogEntries: List<ClientEvent>? = null,

    /**
     * Hijack indicators (Mindslaver-style). Always present in the delta — overwrites the
     * corresponding field on the client unconditionally. Rare to change but cheap to send.
     */
    val youAreHijacking: EntityId? = null,
    val youAreHijackedBy: EntityId? = null,

    /** Hotseat indicator. Always present in the delta; overwrites the client field. */
    val hotseat: Boolean? = null,

    /**
     * The viewer's decklist, sent only when it changed. Its *composition* is fixed for a whole
     * game, so in practice this fires when a card's `remaining` count moves — a draw, a mill, a
     * tutor — and not on the many updates that only shuffle the battlefield around.
     */
    val deck: List<ClientDeckCard>? = null,
)
