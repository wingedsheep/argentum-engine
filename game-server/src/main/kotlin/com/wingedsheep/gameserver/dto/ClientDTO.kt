package com.wingedsheep.gameserver.dto

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.game.Phase
import com.wingedsheep.rulesengine.game.Step
import kotlinx.serialization.Serializable

/**
 * Client-facing game state DTO.
 *
 * This is an explicit API contract for what the client receives.
 * All internal implementation details (components, etc.) are transformed
 * into explicit fields that the client needs for rendering.
 *
 * Benefits:
 * - Client doesn't need rules-engine component classes
 * - API is stable even if internal representation changes
 * - Information leakage is prevented by explicit field selection
 * - Smaller message size (only include what's needed)
 */
@Serializable
data class ClientGameState(
    /** The player viewing this state */
    val viewingPlayerId: EntityId,

    /** All visible cards/permanents */
    val cards: Map<EntityId, ClientCard>,

    /** Zone information */
    val zones: List<ClientZone>,

    /** Player information */
    val players: List<ClientPlayer>,

    /** Current phase and step */
    val currentPhase: Phase,
    val currentStep: Step,

    /** Whose turn it is */
    val activePlayerId: EntityId,

    /** Who currently has priority */
    val priorityPlayerId: EntityId,

    /** Turn number */
    val turnNumber: Int,

    /** Whether the game is over */
    val isGameOver: Boolean,

    /** The winner, if the game is over */
    val winnerId: EntityId?,

    /** Combat state, if in combat */
    val combat: ClientCombatState?
)

/**
 * Card/permanent information for client display.
 *
 * Contains all the information needed to render a card in the UI.
 * Does NOT include server-internal data like triggers, abilities, etc.
 */
@Serializable
data class ClientCard(
    /** Unique identifier */
    val id: EntityId,

    /** Card name for display */
    val name: String,

    /** Mana cost as a string (e.g., "{2}{G}{G}") */
    val manaCost: String,

    /** Converted mana cost / mana value */
    val manaValue: Int,

    /** Type line as displayed on the card (e.g., "Creature — Human Warrior") */
    val typeLine: String,

    /** Card types for filtering (creature, land, instant, etc.) */
    val cardTypes: Set<String>,

    /** Subtypes for display and filtering (e.g., "Human", "Warrior", "Forest") */
    val subtypes: Set<String>,

    /** Card colors */
    val colors: Set<Color>,

    /** Oracle text / rules text (for display in card details) */
    val oracleText: String,

    /** Power for creatures (null if not a creature) */
    val power: Int?,

    /** Toughness for creatures (null if not a creature) */
    val toughness: Int?,

    /** Current damage on creature (only present on battlefield) */
    val damage: Int?,

    /** Keywords the card has (flying, haste, etc.) */
    val keywords: Set<Keyword>,

    /** Counters on the card */
    val counters: Map<CounterType, Int>,

    /** State flags */
    val isTapped: Boolean,
    val hasSummoningSickness: Boolean,
    val isTransformed: Boolean,

    /** Combat state (if in combat) */
    val isAttacking: Boolean,
    val isBlocking: Boolean,
    val attackingTarget: EntityId?,
    val blockingTarget: EntityId?,

    /** Controller (who controls it now) */
    val controllerId: EntityId,

    /** Owner (who started with it in their deck) */
    val ownerId: EntityId,

    /** Whether this is a token */
    val isToken: Boolean,

    /** Zone this card is currently in */
    val zone: ZoneId?,

    /** Attached to (for auras, equipment) */
    val attachedTo: EntityId?,

    /** What's attached to this card (auras, equipment on this permanent) */
    val attachments: List<EntityId>,

    /** Whether this card is face-down (for morph, manifest, hidden info) */
    val isFaceDown: Boolean
)

/**
 * Zone information for client display.
 */
@Serializable
data class ClientZone(
    val zoneId: ZoneId,

    /** Card IDs in this zone, in order */
    val cardIds: List<EntityId>,

    /** Number of cards in the zone (always available, even for hidden zones) */
    val size: Int,

    /** Whether the contents are visible to the viewing player */
    val isVisible: Boolean
)

/**
 * Player information for client display.
 */
@Serializable
data class ClientPlayer(
    val playerId: EntityId,
    val name: String,
    val life: Int,
    val poisonCounters: Int,
    val handSize: Int,
    val librarySize: Int,
    val graveyardSize: Int,
    val landsPlayedThisTurn: Int,
    val hasLost: Boolean,

    /** Mana in mana pool (only visible for own player) */
    val manaPool: ClientManaPool?
)

/**
 * Mana pool state for client display.
 */
@Serializable
data class ClientManaPool(
    val white: Int,
    val blue: Int,
    val black: Int,
    val red: Int,
    val green: Int,
    val colorless: Int
) {
    val total: Int get() = white + blue + black + red + green + colorless
    val isEmpty: Boolean get() = total == 0
}

/**
 * Combat state for client display.
 */
@Serializable
data class ClientCombatState(
    /** Who is attacking */
    val attackingPlayerId: EntityId,

    /** Who is defending */
    val defendingPlayerId: EntityId,

    /** All declared attackers with their targets */
    val attackers: List<ClientAttacker>,

    /** All declared blockers with what they're blocking */
    val blockers: List<ClientBlocker>
)

/**
 * Attacker information for combat display.
 */
@Serializable
data class ClientAttacker(
    val creatureId: EntityId,
    val creatureName: String,
    val attackingTarget: ClientCombatTarget,
    val blockedBy: List<EntityId>
)

/**
 * What an attacker is attacking.
 */
@Serializable
sealed interface ClientCombatTarget {
    @Serializable
    data class Player(val playerId: EntityId) : ClientCombatTarget

    @Serializable
    data class Planeswalker(val permanentId: EntityId) : ClientCombatTarget
}

/**
 * Blocker information for combat display.
 */
@Serializable
data class ClientBlocker(
    val creatureId: EntityId,
    val creatureName: String,
    val blockingAttacker: EntityId
)
