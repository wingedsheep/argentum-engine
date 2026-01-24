package com.wingedsheep.gameserver.masking

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import kotlinx.serialization.Serializable

/**
 * A client-safe view of the game state with hidden information masked.
 *
 * State masking rules:
 * - Hand: Player sees full details, opponent sees card count only
 * - Library: Both players see card count only
 * - Graveyard: Full details for both
 * - Battlefield: Full details for both
 * - Stack: Full details for both
 * - Exile: Full details for both
 */
@Serializable
data class MaskedGameState(
    /**
     * The player viewing this state.
     */
    val viewingPlayerId: EntityId,

    /**
     * All visible entities with their components.
     * Hidden cards are represented with minimal info.
     */
    val entities: Map<EntityId, MaskedEntity>,

    /**
     * Zone contents.
     * For hidden zones (library), only size is provided.
     * For hand, opponent's hand shows only count.
     * Use findZone() or getZone() to look up zones by ZoneKey.
     */
    val zones: List<MaskedZone>,

    /**
     * Player information visible to this player.
     */
    val players: List<MaskedPlayer>,

    /**
     * Current phase and step.
     */
    val currentPhase: Phase,
    val currentStep: Step,

    /**
     * Whose turn it is.
     */
    val activePlayerId: EntityId?,

    /**
     * Who currently has priority.
     */
    val priorityPlayerId: EntityId?,

    /**
     * Turn number.
     */
    val turnNumber: Int,

    /**
     * Whether the game is over.
     */
    val isGameOver: Boolean,

    /**
     * The winner, if the game is over.
     */
    val winnerId: EntityId?
)

/**
 * A masked representation of an entity.
 */
@Serializable
data class MaskedEntity(
    val id: EntityId,
    /**
     * Whether this entity is fully visible.
     * If false, only the id is available (face-down card).
     */
    val isVisible: Boolean,
    /**
     * Components, only present if visible.
     */
    val components: ComponentContainer? = null
)

/**
 * A masked representation of a zone.
 */
@Serializable
data class MaskedZone(
    val zoneKey: ZoneKey,
    /**
     * Entity IDs in this zone, in order.
     * For hidden zones, this may be empty.
     */
    val entityIds: List<EntityId>,
    /**
     * Number of cards in the zone.
     * Always available, even for hidden zones.
     */
    val size: Int,
    /**
     * Whether the contents are visible to the viewing player.
     */
    val isVisible: Boolean
)

/**
 * Public information about a player.
 */
@Serializable
data class MaskedPlayer(
    val playerId: EntityId,
    val name: String,
    val life: Int,
    val poisonCounters: Int,
    val handSize: Int,
    val librarySize: Int,
    val graveyardSize: Int,
    val landsPlayedThisTurn: Int,
    val hasLost: Boolean
)
