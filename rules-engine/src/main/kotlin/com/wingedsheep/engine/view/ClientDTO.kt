package com.wingedsheep.engine.view

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
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
    val combat: ClientCombatState?,

    /** Accumulated game log entries for this player */
    val gameLog: List<ClientEvent> = emptyList()
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

    /** Power for creatures (null if not a creature) - this is the projected/modified value */
    val power: Int?,

    /** Toughness for creatures (null if not a creature) - this is the projected/modified value */
    val toughness: Int?,

    /** Base power (before modifications) - for displaying buff indicators */
    val basePower: Int?,

    /** Base toughness (before modifications) - for displaying buff indicators */
    val baseToughness: Int?,

    /** Current damage on creature (only present on battlefield) */
    val damage: Int?,

    /** Keywords the card has (flying, haste, etc.) */
    val keywords: Set<Keyword>,

    /** Ability flags (non-keyword static abilities like "can't be blocked") */
    val abilityFlags: Set<AbilityFlag> = emptySet(),

    /** Protection colors (for colored protection shield icons) */
    val protections: List<Color> = emptyList(),

    /** Hexproof-from-color colors (for colored hexproof shield icons) */
    val hexproofFromColors: List<Color> = emptyList(),

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
    val zone: ZoneKey?,

    /** Attached to (for auras, equipment) */
    val attachedTo: EntityId?,

    /** What's attached to this card (auras, equipment on this permanent) */
    val attachments: List<EntityId>,

    /** Cards exiled by this permanent via linked exile (e.g., Suspension Field, Oblivion Ring) */
    val linkedExile: List<EntityId> = emptyList(),

    /** Whether this card is face-down (for morph, manifest, hidden info) */
    val isFaceDown: Boolean,

    /** Morph cost for face-down creatures (only visible to controller) */
    val morphCost: String? = null,

    /** Targets for spells/abilities on the stack (for targeting arrows) */
    val targets: List<ClientChosenTarget> = emptyList(),

    /** Image URI from card metadata (for rendering card images) */
    val imageUri: String? = null,

    /** Active effects on this card (e.g., "can't be blocked except by black creatures") */
    val activeEffects: List<ClientCardEffect> = emptyList(),

    /** Official rulings for this card (for card details view) */
    val rulings: List<ClientRuling> = emptyList(),

    /** Whether this spell was kicked (only present on stack) */
    val wasKicked: Boolean = false,

    /** Whether this spell promised a gift (Bloomburrow gift mechanic — only present on stack) */
    val giftPromised: Boolean = false,

    /** Whether this spell's optional Blight additional cost was paid (Lorwyn Eclipsed — only present on stack) */
    val wasBlightPaid: Boolean = false,

    /** Chosen X value for spells with X in their cost (only present on stack) */
    val chosenX: Int? = null,

    /** Copy index for storm/copy effects on the stack (1, 2, 3...) */
    val copyIndex: Int? = null,

    /** Total number of copies for storm/copy effects on the stack */
    val copyTotal: Int? = null,

    /** Chosen creature type for "as enters, choose a creature type" permanents (e.g., Doom Cannon) */
    val chosenCreatureType: String? = null,

    /** Chosen color for "as enters, choose a color" permanents (e.g., Riptide Replicator) */
    val chosenColor: String? = null,

    /** Triggering entity ID for triggered abilities on the stack (for source arrows) */
    val triggeringEntityId: EntityId? = null,

    /** Source zone for triggered abilities on the stack (e.g., "GRAVEYARD" for Gigapede) */
    val sourceZone: String? = null,

    /** Creature types of the sacrificed permanent (for spells like Endemic Plague on the stack) */
    val sacrificedCreatureTypes: Set<String>? = null,

    /** Specific ability text when on the stack (e.g., spell effect description, not full oracle text) */
    val stackText: String? = null,

    /**
     * Runtime descriptions of each chosen mode, in the order they were picked (700.2). Empty for
     * non-modal spells and for modal spells whose mode hasn't been selected yet. For opponent
     * visibility of choose-N commands (Brigid's Command, Sygg's Command, etc.).
     */
    val chosenModeDescriptions: List<String> = emptyList(),

    /**
     * Per-mode target groups for modal spells on the stack, aligned 1:1 with
     * [chosenModeDescriptions]. Each group carries the mode's description, the chosen targets
     * (for arrow rendering), and human-readable target names (for text rendering).
     */
    val perModeTargets: List<ClientPerModeTargetGroup> = emptyList(),

    /** Revealed name for face-down creatures that this player has peeked at (e.g., via Spy Network) */
    val revealedName: String? = null,

    /** Revealed image URI for face-down creatures that this player has peeked at */
    val revealedImageUri: String? = null,

    /** Whether this card can be played from exile (e.g., Mind's Desire impulse draw) */
    val playableFromExile: Boolean = false,

    /** Original card name when this permanent is a copy (e.g., "Clever Impersonator" copying a Wind Drake) */
    val copyOf: String? = null,

    /** Damage distribution for DividedDamageEffect spells on the stack (target entity ID -> damage amount) */
    val damageDistribution: Map<EntityId, Int>? = null,

    /** For Sagas: the total number of chapters (e.g., 3). Null for non-Sagas. */
    val sagaTotalChapters: Int? = null,

    /** For Class enchantments: the current class level (1, 2, or 3). Null for non-Classes. */
    val classLevel: Int? = null,

    /** For Class enchantments: the maximum class level (e.g., 3). Null for non-Classes. */
    val classMaxLevel: Int? = null,

    /**
     * For cards with a "threshold"-style static ability (one whose condition compares the
     * controller's graveyard size to a fixed number, e.g., "as long as there are seven or
     * more cards in your graveyard"). Lets the client render a progress badge.
     */
    val thresholdInfo: ClientThresholdInfo? = null,

    /** Whether this is a double-faced card (DFC). */
    val isDoubleFaced: Boolean = false,

    /** Which face is currently up on a DFC permanent: "FRONT" or "BACK" (null if not a DFC). */
    val currentFace: String? = null,

    /** Back face's display name for DFCs. Null if not a DFC. */
    val backFaceName: String? = null,

    /** Back face's type line for DFCs. */
    val backFaceTypeLine: String? = null,

    /** Back face's oracle text for DFCs. */
    val backFaceOracleText: String? = null,

    /** Back face's image URI for DFCs. */
    val backFaceImageUri: String? = null,

    /**
     * For planeswalkers on the battlefield: every loyalty ability on the card, in declaration
     * order. Lets the client show the full menu with unavailable abilities grayed out instead of
     * hiding them. Null for non-planeswalkers and for planeswalkers outside the battlefield.
     */
    val planeswalkerAbilities: List<ClientPlaneswalkerAbility>? = null
)

/**
 * One loyalty ability on a planeswalker, for rendering the full ability menu (including
 * abilities the controller can't currently pay for, rendered grayed out). The client matches
 * by [abilityId] against the `action.abilityId` in `legalActions` to decide availability.
 */
@Serializable
data class ClientPlaneswalkerAbility(
    /** Stable id — matches `ActivateAbility.abilityId` in legal actions. */
    val abilityId: String,
    /** Signed loyalty change (e.g., +1, -2, -8). */
    val loyaltyChange: Int,
    /** Ability text (e.g., "Create a 1/1 green and white Kithkin creature token"). */
    val description: String
)

/**
 * Threshold progress for cards whose static ability turns on at a graveyard-size milestone.
 * Both `current` and `required` are graveyard card counts for the card's controller.
 */
@Serializable
data class ClientThresholdInfo(
    val current: Int,
    val required: Int,
    val active: Boolean
)

/**
 * Zone information for client display.
 */
@Serializable
data class ClientZone(
    val zoneId: ZoneKey,

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
    val exileSize: Int,
    val landsPlayedThisTurn: Int,
    val hasLost: Boolean,

    /** Mana in mana pool (only visible for own player) */
    val manaPool: ClientManaPool?,

    /** Active effects on this player (e.g., "Skip Combat" from False Peace) */
    val activeEffects: List<ClientPlayerEffect> = emptyList()
)

/**
 * An active effect on a player that should be displayed as a badge.
 */
@Serializable
data class ClientPlayerEffect(
    /** Unique identifier for the effect type */
    val effectId: String,
    /** Human-readable name for display */
    val name: String,
    /** Optional description/tooltip text */
    val description: String? = null,
    /** Optional icon identifier for UI rendering */
    val icon: String? = null
)

/**
 * An active effect on a card that should be displayed as a badge.
 * Used for temporary effects like "can't be blocked except by black creatures".
 */
@Serializable
data class ClientCardEffect(
    /** Unique identifier for the effect type */
    val effectId: String,
    /** Human-readable name for display */
    val name: String,
    /** Optional description/tooltip text */
    val description: String? = null,
    /** Optional icon identifier for UI rendering */
    val icon: String? = null
)

/**
 * An official ruling for a card.
 * Displayed in card details view to clarify complex interactions.
 */
@Serializable
data class ClientRuling(
    /** Date of the ruling (e.g., "6/8/2016") */
    val date: String,
    /** The ruling text */
    val text: String
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
    val blockedBy: List<EntityId>,
    /** True if all creatures that can block this creature must do so (Alluring Scent) */
    val mustBeBlockedByAll: Boolean = false,
    /** Ordered list of blockers for damage assignment (first receives damage first). Null if not yet ordered. */
    val damageAssignmentOrder: List<EntityId>? = null,
    /** Damage assigned to each target (blocker ID or player ID -> damage amount). Null if not yet assigned. */
    val damageAssignments: Map<EntityId, Int>? = null
)

/**
 * What an attacker is attacking.
 */
@Serializable
sealed interface ClientCombatTarget {
    @Serializable
    @kotlinx.serialization.SerialName("Player")
    data class Player(val playerId: EntityId) : ClientCombatTarget

    @Serializable
    @kotlinx.serialization.SerialName("Planeswalker")
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

/**
 * A group of targets chosen for a single mode of a modal spell on the stack. The index refers to
 * the position within [ClientCard.perModeTargets], not the original [Mode] index (the same mode
 * can appear twice with [ModalEffect.allowRepeat]).
 */
@Serializable
data class ClientPerModeTargetGroup(
    /** The mode index in the spell's [ModalEffect.modes] list. */
    val modeIndex: Int,
    /** Runtime description of the mode, with dynamic amounts evaluated. */
    val modeDescription: String,
    /** Chosen targets for this mode, for arrow rendering. */
    val targets: List<ClientChosenTarget> = emptyList(),
    /**
     * Human-readable target names aligned 1:1 with [targets]. For hidden-zone targets (e.g., a
     * card in an opponent's hand), the name is generic ("a card in Opponent's hand") to avoid
     * leaking hidden information.
     */
    val targetNames: List<String> = emptyList()
)

/**
 * Represents a chosen target for a spell or ability on the stack.
 * Used to draw targeting arrows in the UI.
 */
@Serializable
sealed interface ClientChosenTarget {
    @Serializable
    @kotlinx.serialization.SerialName("Player")
    data class Player(val playerId: EntityId) : ClientChosenTarget

    @Serializable
    @kotlinx.serialization.SerialName("Permanent")
    data class Permanent(val entityId: EntityId) : ClientChosenTarget

    @Serializable
    @kotlinx.serialization.SerialName("Spell")
    data class Spell(val spellEntityId: EntityId) : ClientChosenTarget

    @Serializable
    @kotlinx.serialization.SerialName("Card")
    data class Card(val cardId: EntityId) : ClientChosenTarget
}
