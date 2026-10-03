package com.wingedsheep.gym.contract

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stable serialized order for the per-player zone views in [TrainingObservation.zones].
 *
 * The order is written out rather than derived from [Zone.entries] because it is a schema
 * guarantee: reordering the enum's declarations must not silently permute model-facing inputs.
 * Together with [NON_PLAYER_KEYED_ZONES] this classifies every [Zone] the engine models, so a new
 * engine zone cannot be added without deciding here whether an agent observes it per player.
 *
 * Known gap: emblems are zone-less entities (they carry `EmblemSourceComponent` and live in no
 * zone at all), so no zone view reaches them. Surfacing them needs its own contract field.
 */
val TRAINING_OBSERVATION_ZONE_ORDER: List<Zone> = listOf(
    Zone.HAND,
    Zone.LIBRARY,
    Zone.GRAVEYARD,
    Zone.EXILE,
    Zone.BATTLEFIELD,
    Zone.COMMAND,
    Zone.SIDEBOARD,
)

/**
 * Zones deliberately absent from [TRAINING_OBSERVATION_ZONE_ORDER] because they are not keyed per
 * player: [TrainingObservation.stack] carries the stack's own ordered representation.
 */
val NON_PLAYER_KEYED_ZONES: Set<Zone> = setOf(Zone.STACK)

/**
 * The payload an agent receives from any gym environment after `observe` / `step`.
 *
 * A gym env is no longer only a game of Magic — deckbuilding is its own env type
 * ([DeckbuildObservation]) — so the wire observation is a discriminated union. The
 * `type` field tells a client which variant it holds: `"Game"` for an in-progress
 * match, `"Deckbuild"` for a sealed-pool build. The fields hoisted here are the ones
 * every env exposes so a generic driver loop (read `legalActions`, pick one, `step`,
 * stop on `terminated`) works without knowing the variant up front.
 */
@Serializable
sealed interface Observation {
    /** Sha256 of the canonical schema — clients compare to abort on drift. */
    val schemaHash: String

    /** The player/agent who must act next, or null when [terminated]. */
    val agentToAct: EntityId?

    /** Non-null only for in-game complex decisions; always null for deckbuild. */
    val pendingDecision: PendingDecisionView?

    /** Every action available to [agentToAct] this step. Action IDs are per-step. */
    val legalActions: List<LegalActionView>

    /** True once the env reached a terminal state (game over, or deck finalized). */
    val terminated: Boolean

    /** Deterministic hash of the observable state, for MCTS transposition tables. */
    val stateDigest: String
}

/**
 * Root payload for a **game** env, sent to a training agent after every `reset()` / `step()`.
 *
 * Designed for RL consumers (neural policies, MCTS) — not for human display.
 * The schema is stable across card sets; new mechanics appear as strings in
 * [EntityFeatures.types] / [EntityFeatures.subtypes] / [EntityFeatures.keywords]
 * rather than as new fields.
 *
 * Action IDs in [legalActions] are **per-step** — they are regenerated every
 * time the environment advances and must not be cached across steps.
 */
@Serializable
@SerialName("Game")
data class TrainingObservation(
    /** Sha256 of the canonical schema. Python clients compare this to abort on drift. */
    override val schemaHash: String,

    /** The player whose information-set this observation represents. */
    val perspectivePlayerId: EntityId,

    /** The player who needs to act next, or null if the game is over. */
    override val agentToAct: EntityId?,

    val turnNumber: Int,
    val phase: Phase,
    val step: Step,
    val activePlayerId: EntityId?,
    val priorityPlayerId: EntityId?,

    val players: List<PlayerView>,

    /**
     * Per-zone entity views: every player in turn order crossed with
     * [TRAINING_OBSERVATION_ZONE_ORDER], in that order, empty zones included. So a
     * `(ownerId, zoneType)` pair appears exactly once and the list is a fixed-width, fixed-order
     * input a consumer may index positionally.
     *
     * [ZoneView.hidden] means the zone is not wholly public to this perspective. [ZoneView.cards]
     * contains only the identities this perspective may know, so an individually revealed card can
     * appear while the zone remains hidden and [ZoneView.size] still reports its true size.
     */
    val zones: List<ZoneView>,

    /** Stack contents, ordered bottom → top (top of stack = last element). */
    val stack: List<StackItemView>,

    /** Non-null when the engine paused for a player decision. */
    override val pendingDecision: PendingDecisionView?,

    /** All actions available to [agentToAct]. Empty when the game is over. */
    override val legalActions: List<LegalActionView>,

    /** True if the game ended naturally. */
    override val terminated: Boolean,

    /** Set if [terminated] and there is a winner (null = draw or ongoing). */
    val winnerId: EntityId?,

    /**
     * Deterministic hash of the observable game state, intended for transposition
     * tables in MCTS. Two observations with the same digest describe the same
     * information-set from the same perspective.
     */
    override val stateDigest: String
) : Observation

/** Per-player summary. Counts reflect what [perspectivePlayerId] can see. */
@Serializable
data class PlayerView(
    val id: EntityId,
    val name: String,
    val lifeTotal: Int,
    val handSize: Int,
    val librarySize: Int,
    val graveyardSize: Int,
    val exileSize: Int,
    /** Mana currently floating in this player's mana pool (colorless bucket in `colorless`). */
    val manaPool: ManaPoolView,
    val isPerspective: Boolean,
    val isActive: Boolean,
    val hasPriority: Boolean,
    val hasLost: Boolean
)

@Serializable
data class ManaPoolView(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0
)

/**
 * A zone's contents from [TrainingObservation.perspectivePlayerId]'s point of view.
 *
 * [hidden] reports the structural fact that the zone is not wholly public to this perspective (an
 * opponent's hand, any library) — a stable feature, so an agent's input distribution does not shift
 * as reveals come and go. It is deliberately not "something here is unknown": [size] is the true
 * count while [cards] carries only the entries this perspective knows, and that subset can be the
 * whole zone. A hand-peek effect leaves the cards it showed visible to the player who saw them, so
 * a two-card hidden hand can legitimately list one card — or, for a one-card library under a public
 * top-card reveal, all of it. Hidden-zone slot IDs and the positions of known cards among unknown
 * ones are intentionally not part of this schema.
 */
@Serializable
data class ZoneView(
    val ownerId: EntityId,
    val zoneType: Zone,
    val hidden: Boolean,
    val size: Int,
    val cards: List<EntityFeatures>
)

/**
 * Flat feature bundle for a card/permanent. Values come from the projected
 * state (post-Rule 613), not base components, so control-changing and
 * type-changing effects are reflected.
 *
 * Not every field is populated for every zone:
 * - On the battlefield: all fields relevant to a permanent are set.
 * - Outside the battlefield (library/hand/graveyard/exile/command/sideboard): the card's static
 *   properties are set;
 *   dynamic properties (tapped, damage, counters) default to their "not present" values.
 */
@Serializable
data class EntityFeatures(
    val entityId: EntityId,
    /** Null for a face-down object the perspective player may not look at. */
    val cardDefinitionId: String?,
    /**
     * The projected name — what Layer 3 renamed the object to (Witness Protection), else the
     * printed one. `"Face-down creature"` / `"Face-down card"` for a face-down object the
     * perspective player may not look at, whose [oracleText], [manaCost] and [manaValue] are
     * blanked to match.
     */
    val name: String,
    val zone: Zone,
    val ownerId: EntityId?,
    /** Projected controller (battlefield only; null elsewhere). */
    val controllerId: EntityId?,

    /** Projected card types as strings (e.g., "CREATURE", "ARTIFACT", "LEGENDARY"). */
    val types: Set<String>,
    /** Projected subtypes as strings (e.g., "GOBLIN", "WARRIOR"). */
    val subtypes: Set<String>,
    /** Projected colors (e.g., "RED", "WHITE"). */
    val colors: Set<String>,
    /** Projected keywords (e.g., "FLYING", "TRAMPLE"). */
    val keywords: Set<String>,

    /** Canonical mana cost string, e.g. "{1}{R}{R}". Empty for lands and tokens. */
    val manaCost: String,
    val manaValue: Int,

    /**
     * The card's printed rules text (oracle text), e.g.
     * `"Flying\nWhen Dawnhand Eulogist dies, draw a card."`.
     *
     * Reflects the base card definition — it is *not* rewritten by Rule
     * 613 text-changing effects, so a Copy-Enchantment-style scenario
     * will occasionally lie to a strict reader. For most cards it is
     * the single most informative field an agent can read, and without
     * it an NN has no way to know what a card actually does.
     */
    val oracleText: String = "",

    /** Projected power — null if not a creature (via projection). */
    val power: Int?,
    /** Projected toughness — null if not a creature. */
    val toughness: Int?,

    val tapped: Boolean = false,
    /**
     * The restriction actually in force: the object entered under this controller too recently,
     * it is currently a creature, and it has no haste. Not the raw engine marker — a creature
     * with projected haste reports `false` even though the marker is still on it, and reverts to
     * `true` if the haste goes away while the marker stands.
     */
    val summoningSick: Boolean = false,
    val faceDown: Boolean = false,
    val damageMarked: Int = 0,
    /** Counter type name → count. */
    val counters: Map<String, Int> = emptyMap(),
    /** Non-null if attached (aura/equipment) to another entity. */
    val attachedTo: EntityId? = null,
    val attachments: List<EntityId> = emptyList()
)

/** An item on the stack (spell or ability). */
@Serializable
data class StackItemView(
    val entityId: EntityId,
    /** The caster of a spell, or the controller of an ability. */
    val controllerId: EntityId?,
    /** The spell's card name, or the source name of an ability. */
    val name: String,
    val kind: StackItemKind,
    /** Printed oracle text of the card, or an ability's description. */
    val oracleText: String = "",
    /**
     * The chosen targets, in the order they were chosen, flattened to entity ids. A player
     * target contributes the player's own entity id.
     */
    val targets: List<EntityId> = emptyList()
)

@Serializable
enum class StackItemKind { SPELL, TRIGGERED_ABILITY, ACTIVATED_ABILITY, OTHER }

/**
 * Compact view of a single legal action. Trainers post back the [actionId]
 * to commit. The registry mapping `Int → engine action` lives on the server
 * and is regenerated every step.
 *
 * Decision options (when [TrainingObservation.pendingDecision] is set and
 * the decision is simple enough to fold in — YesNo, ChooseNumber, ChooseMode,
 * ChooseOption, ChooseColor, and single-select SelectCards) also appear as
 * [LegalActionView]s in the same list, distinguished by [kind] == "DECISION".
 */
@Serializable
data class LegalActionView(
    val actionId: Int,
    val kind: String,
    val description: String,
    val affordable: Boolean,
    val sourceEntityId: EntityId? = null,
    val targetEntityIds: List<EntityId> = emptyList(),
    val manaCost: String? = null,
    val hasXCost: Boolean = false,
    val maxAffordableX: Int? = null,
    val minTargets: Int = 0,
    val maxTargets: Int = 0,
    val requiresDamageDistribution: Boolean = false,
    val isManaAbility: Boolean = false,
    /**
     * Creatures that may be declared as attackers (`kind == "DeclareAttackers"`), empty otherwise.
     * Pair each with one of [validAttackTargets] in `ActionParams.attackers` when stepping; step it
     * with no params to attack with nobody.
     */
    val validAttackers: List<EntityId> = emptyList(),
    /** Attackers that *must* attack if able (CR 508.1d) — a declaration omitting one is rejected. */
    val mandatoryAttackers: List<EntityId> = emptyList(),
    /** Players, planeswalkers and battles this player may attack. */
    val validAttackTargets: List<EntityId> = emptyList(),
    /**
     * Creatures that may be declared as blockers (`kind == "DeclareBlockers"`), empty otherwise.
     * Map each to the attackers it blocks in `ActionParams.blockers`.
     */
    val validBlockers: List<EntityId> = emptyList(),
    /**
     * How many attackers each blocker may block at once — absent means the default one (CR 509.1a).
     * A declaration exceeding a blocker's limit is rejected, so a caller building
     * `ActionParams.blockers` has to respect it.
     */
    val blockerMaxBlockCounts: Map<EntityId, Int> = emptyMap(),
    /**
     * Blocks that *must* be made if able (CR 509.1c) — blocker id → the attackers it is required to
     * block. Like [mandatoryAttackers] on the attack side, a declaration that obeys fewer of these
     * than it could is illegal, so this is not advisory.
     */
    val mandatoryBlockerAssignments: Map<EntityId, List<EntityId>> = emptyMap(),
    /** True when this entry was generated from [PendingDecisionView], not a GameAction. */
    val isDecisionOption: Boolean = false
)

/**
 * Summary of the currently-paused decision. When present, [LegalActionView]s
 * with `isDecisionOption = true` are the concrete choices the player can post.
 *
 * For complex decisions (multi-target ChooseTargets, DistributeDecision,
 * OrderObjectsDecision, SplitPilesDecision, ReorderLibraryDecision) the folded
 * action-ID space is not expressive enough; [legalActions] will be empty and
 * the trainer must submit a structured `DecisionResponse` (exposed via a
 * separate endpoint in Phase 3).
 */
@Serializable
data class PendingDecisionView(
    val decisionId: String,
    val kind: PendingDecisionKind,
    val playerId: EntityId,
    val prompt: String,
    val sourceEntityId: EntityId? = null,
    val sourceName: String? = null,
    val triggeringEntityId: EntityId? = null,
    val subjectEntityId: EntityId? = null,
    val effectHint: String? = null,
    /** True when no LegalActionView options were generated; structured response required. */
    val requiresStructuredResponse: Boolean = false,
    /** Extra hints about the decision shape (min/max selections, numeric range, etc.). */
    val shape: DecisionShape = DecisionShape()
)

@Serializable
enum class PendingDecisionKind {
    CHOOSE_TARGETS,
    SELECT_CARDS,
    YES_NO,
    CHOOSE_MODE,
    CHOOSE_COLOR,
    CHOOSE_NUMBER,
    DISTRIBUTE,
    ORDER_OBJECTS,
    SPLIT_PILES,
    CHOOSE_OPTION,
    PLAY_CARD,
    CHOOSE_REPLACEMENT,
    SEARCH_LIBRARY,
    REORDER_LIBRARY,
    ASSIGN_DAMAGE,
    COMBAT_RESOLUTION,
    SELECT_MANA_SOURCES,
    BUDGET_MODAL
}

@Serializable
data class DecisionShape(
    val minSelections: Int = 0,
    val maxSelections: Int = 0,
    val numericMin: Int? = null,
    val numericMax: Int? = null,
    val availableColors: Set<Color> = emptySet(),
    val totalToDistribute: Int? = null,
    val budget: Int? = null
)
