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
    val gameLog: List<ClientEvent> = emptyList(),

    /**
     * Whether the global Void condition is satisfied this turn (a nonland permanent left
     * the battlefield this turn or a spell was warped this turn). Drives UI cues for cards
     * with Void abilities (Edge of Eternities).
     */
    val voidActive: Boolean = false,

    /**
     * If non-null, the affected player whose turn the viewing player is currently
     * driving (Mindslaver-style hijack). Drives UI cues such as the controller banner
     * and promoting the affected player's hand to face-up.
     */
    val youAreHijacking: EntityId? = null,

    /**
     * If non-null, the controller currently driving the viewing player's turn.
     * Drives UI cues such as the affected-player banner and disabling click handlers.
     */
    val youAreHijackedBy: EntityId? = null,

    /**
     * True when the viewing player controls *every* seat for the whole game — the
     * single-client "hotseat" / play-against-yourself mode. Drives the "controlling both
     * players" banner and lets the client act for whichever seat currently has priority.
     * Distinct from [youAreHijacking], which is the per-turn Mindslaver effect; the two are
     * never set together.
     */
    val hotseat: Boolean = false,

    /**
     * The viewing player's active persistent yields (MTGO right-click yields — backlog §C). Masked
     * per-player: only the viewer's own yields are ever included. Drives the "Active yields" panel
     * (revoke / clear-all) and lets the client suppress re-prompting cues. Empty for spectators.
     */
    val activeYields: List<ClientYield> = emptyList()
)

/**
 * Client-facing form of [com.wingedsheep.sdk.scripting.AbilityIdentity] — the stable
 * (cardDefinitionId, abilityId) key an ability is yielded against (backlog §C).
 */
@Serializable
data class ClientAbilityIdentity(
    val cardDefinitionId: String,
    val abilityId: String
)

/**
 * One ability the viewing player has set a yield on, flattened from [PlayerYields] for display.
 * Keyed by its [com.wingedsheep.sdk.scripting.AbilityIdentity] ([cardDefinitionId] + [abilityId]);
 * [displayName] is the human-readable card name derived from the definition id.
 */
@Serializable
data class ClientYield(
    val cardDefinitionId: String,
    val abilityId: String,
    val displayName: String,
    /** Auto-pass priority on this ability's stack objects until end of turn. */
    val untilEndOfTurn: Boolean = false,
    /** Auto-pass priority on this ability's stack objects for the rest of the game. */
    val wholeGame: Boolean = false,
    /** Remembered may-question answer (`true`=always yes, `false`=always no), or null if none. */
    val autoAnswer: Boolean? = null
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

    /** Hexproof from monocolored (CR 105.2) — shows an uncolored hexproof shield chip */
    val hexproofFromMonocolored: Boolean = false,

    /** Counters on the card */
    val counters: Map<CounterType, Int>,

    /** State flags */
    val isTapped: Boolean,
    val hasSummoningSickness: Boolean,
    val isTransformed: Boolean,

    /** Phased out (Rule 702.26) — treated as though it doesn't exist; rendered translucent */
    val isPhasedOut: Boolean = false,

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

    /**
     * True iff this card carries `CommanderComponent` (Commander format). Set on the card
     * regardless of which zone it's in — including the battlefield — so the client can render a
     * crown / gold border at all times. Token copies of a commander never carry this (CR 903.10a).
     */
    val isCommander: Boolean = false,

    /**
     * True iff this permanent carries `RingBearerComponent` — i.e. it is its controller's
     * Ring-bearer (CR 701.54). Battlefield only; the designation is stripped on a real control
     * change so a token/stolen permanent never falsely shows it. Lets the UI render a prominent
     * golden Ring icon on the bearer.
     */
    val isRingBearer: Boolean = false,

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

    /**
     * Whether this face-down permanent is a manifested permanent (CR 701.40) rather than a morph.
     * Public information; drives which face-down token art the client renders.
     */
    val isManifested: Boolean = false,

    /** Whether this permanent is suspected (CR 701.60 — has menace and can't block). Battlefield only. */
    val isSuspected: Boolean = false,

    /** Whether this card is plotted in exile (CR 718 — Plot keyword, castable for free on a later turn). Exile only. */
    val isPlotted: Boolean = false,

    /** Whether this permanent is prepared (Secrets of Strixhaven — Prepared keyword): a copy of its
     * prepare spell sits castable in its controller's exile until cast. Battlefield only. */
    val isPrepared: Boolean = false,

    /** Whether this exiled card is the prepare-spell copy of a prepared permanent (Secrets of
     * Strixhaven). It shows up as a castable ghost card in its controller's hand; this flag lets the
     * client badge it so the player sees it originates from a prepared creature. Exile only. */
    val isPreparedSpell: Boolean = false,

    /** Whether this permanent was cast for its warp cost (CR 702.185, Edge of Eternities): it will be
     * exiled at the beginning of the next end step, after which it can be recast from exile. Drives the
     * cosmic "warped" cue on the battlefield. Battlefield only. */
    val isWarped: Boolean = false,

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

    /**
     * For a triggered/activated ability on the stack: the definition-scoped identity of that
     * ability (backlog §C). Lets the stack-item context menu offer "yield / always yes-no to this
     * ability". Null for spells and for ability sources with no card definition.
     */
    val abilityIdentity: ClientAbilityIdentity? = null,

    /** Copy index for storm/copy effects on the stack (1, 2, 3...) */
    val copyIndex: Int? = null,

    /** Total number of copies for storm/copy effects on the stack */
    val copyTotal: Int? = null,

    /** Chosen creature type for "as enters, choose a creature type" permanents (e.g., Doom Cannon) */
    val chosenCreatureType: String? = null,

    /** Chosen color for "as enters, choose a color" permanents (e.g., Riptide Replicator) */
    val chosenColor: String? = null,

    /**
     * Chosen mode label for "as enters, choose X or Y" permanents (e.g., the Siege cycle).
     * Rendered as a badge on the permanent so the player can see which mode is active.
     */
    val chosenMode: String? = null,

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

    /**
     * True when this permanent's printed card has the Legendary supertype but its current
     * type line does not — i.e. a copy effect explicitly stripped legendariness
     * ("except it isn't legendary" / Impostor Syndrome). Lets the UI flag the difference
     * between an original legendary creature and a non-legendary token copy of it.
     */
    val nonLegendaryCopy: Boolean = false,

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
    val planeswalkerAbilities: List<ClientPlaneswalkerAbility>? = null,

    /**
     * Whether this card is a split-layout Room (CR 709.5 + 205.3h, "Enchantment — Room"). Drives
     * split-card rendering in hand/stack/battlefield and the lock-state overlay on the
     * battlefield. False for normal cards and for non-Room split layouts (Aftermath etc.).
     */
    val isRoom: Boolean = false,

    /**
     * For split-layout cards (currently Rooms): one entry per face with that face's name, mana
     * cost, type line, oracle text, and (on the battlefield) whether the door is unlocked. Empty
     * for normal single-face cards.
     */
    val cardFaces: List<ClientCardFace> = emptyList(),

    /**
     * For Rooms on the stack: index into [cardFaces] of the face that was cast. The stack should
     * show only this face's name (CR 709.3b). Null for normal cards and for Rooms outside the
     * stack.
     */
    val castFaceIndex: Int? = null,

    /**
     * Impending alternative cost (CR 702.176), derived from the card's `KeywordAbility.Impending`.
     * Present on any card whose definition has impending, regardless of zone, so the client can
     * always offer the impending cast option alongside the normal cast — graying out whichever the
     * player can't currently pay for — and annotate it with a time-counter glyph. Null for cards
     * without impending.
     */
    val impending: ClientImpending? = null
)

/**
 * The impending option on a card (CR 702.176): its reduced mana cost and the number of time
 * counters the permanent enters with when cast this way. Used by the client to render the
 * impending cast button (with a time-counter glyph showing [time]) even when it's unaffordable.
 */
@Serializable
data class ClientImpending(
    /** Reduced mana cost paid to cast for impending, e.g. "{2}{W}{W}". */
    val cost: String,
    /** Number of time counters the permanent enters with (e.g. 4). */
    val time: Int
)

/**
 * One face of a split-layout card (CR 709). Carries every per-face value the client needs to
 * render either half of the card; for a Room on the battlefield, [isUnlocked] reflects the door
 * state captured by [com.wingedsheep.engine.state.components.identity.RoomComponent].
 *
 * In zones other than the battlefield (hand, stack, graveyard, library, exile), [isUnlocked] is
 * always `false` — there's no Room permanent yet, just a card with two halves.
 */
@Serializable
data class ClientCardFace(
    /** Stable face id — currently the face's printed name (matches RoomFaceId). */
    val faceId: String,
    val name: String,
    val manaCost: String,
    val typeLine: String,
    val oracleText: String,
    val isUnlocked: Boolean
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
    val activeEffects: List<ClientPlayerEffect> = emptyList(),

    /**
     * Per-commander cumulative combat damage dealt to this player (CR 903.10a). One entry per
     * commander that has dealt at least 1 damage. Empty outside `Format.Commander`. The client
     * renders these as progress badges (e.g., `⚔ Atraxa 14/21`) under the life orb.
     */
    val commanderDamage: List<ClientCommanderDamage> = emptyList()
)

/**
 * Per-commander commander-damage tally against a single defending player. Carried inside
 * [ClientPlayer.commanderDamage].
 *
 * @property threshold Single-source loss threshold from `Format.Commander.commanderDamageThreshold`
 *   (21 in classic Commander, 16 in the BRAWL preset). Included per-entry so the client doesn't
 *   need to know format internals to render `amount/threshold`.
 */
@Serializable
data class ClientCommanderDamage(
    val commanderId: EntityId,
    val commanderName: String,
    /** Current controller (may differ from owner under control-changing effects). */
    val controllerId: EntityId,
    val amount: Int,
    val threshold: Int,
    val imageUri: String? = null,
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
    val icon: String? = null,
    /**
     * Optional image URL for the badge — typically a Scryfall marker-card image
     * (e.g., the "City's Blessing" marker). When present, the UI shows the image
     * in place of the emoji-style [icon].
     */
    val imageUri: String? = null,
    /**
     * Optional progression for cumulative effects that climb toward a cap — e.g.
     * The Ring's four-step temptation (CR 701.54c). The UI can render this as
     * filled/empty pips so the player sees how far the effect has advanced.
     */
    val progress: ClientEffectProgress? = null
)

/**
 * A staged progression for a cumulative player effect: [current] steps reached out
 * of [total] meaningful steps. [current] may exceed [total] (e.g. the Ring can tempt
 * more than four times); consumers cap the visualization at [total].
 */
@Serializable
data class ClientEffectProgress(
    val current: Int,
    val total: Int
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
    val colorless: Int,
    val restrictedMana: List<ClientRestrictedManaEntry> = emptyList()
) {
    val total: Int get() = white + blue + black + red + green + colorless + restrictedMana.size
    val isEmpty: Boolean get() = total == 0
}

/**
 * A single unit of restricted mana for client display.
 *
 * @property color Mana color symbol ("W", "U", "B", "R", "G") or null for colorless.
 * @property restrictionDescription Human-readable restriction (e.g., "Spend this mana only to cast spells with mana value 4 or greater").
 */
@Serializable
data class ClientRestrictedManaEntry(
    val color: String?,
    val restrictionDescription: String
)

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
    /**
     * Banding band id (CR 702.22) shared by every attacker in the same band, or null if this
     * attacker isn't banded. Lets the client keep showing the band grouping after attacks are
     * declared (during the defender's blocks and combat), not just during declare-attackers.
     */
    val bandId: String? = null,
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
