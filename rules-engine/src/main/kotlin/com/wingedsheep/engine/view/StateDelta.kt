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

    /** Combat state. Present = changed. combatCleared = true means combat ended (set to null). */
    val combat: ClientCombatState? = null,
    val combatCleared: Boolean? = null,

    /** New game log entries only (append to existing) */
    val newLogEntries: List<ClientEvent>? = null,
)
