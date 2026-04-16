package com.wingedsheep.gym.contract

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Root payload sent to a training agent after every `reset()` / `step()`.
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
data class TrainingObservation(
    /** Sha256 of the canonical schema. Python clients compare this to abort on drift. */
    val schemaHash: String,

    /** The player whose information-set this observation represents. */
    val perspectivePlayerId: EntityId,

    /** The player who needs to act next, or null if the game is over. */
    val agentToAct: EntityId?,

    val turnNumber: Int,
    val phase: Phase,
    val step: Step,
    val activePlayerId: EntityId?,
    val priorityPlayerId: EntityId?,

    val players: List<PlayerView>,

    /**
     * Per-zone entity views. A `(ownerId, zoneType)` pair appears at most once.
     * Hidden zones (opponent hand, libraries) expose [ZoneView.hidden] = true
     * and [ZoneView.cards] is empty (only [ZoneView.size] is populated).
     */
    val zones: List<ZoneView>,

    /** Stack contents, ordered bottom → top (top of stack = last element). */
    val stack: List<StackItemView>,

    /** Non-null when the engine paused for a player decision. */
    val pendingDecision: PendingDecisionView?,

    /** All actions available to [agentToAct]. Empty when the game is over. */
    val legalActions: List<LegalActionView>,

    /** True if the game ended naturally. */
    val terminated: Boolean,

    /** Set if [terminated] and there is a winner (null = draw or ongoing). */
    val winnerId: EntityId?,

    /**
     * Deterministic hash of the observable game state, intended for transposition
     * tables in MCTS. Two observations with the same digest describe the same
     * information-set from the same perspective.
     */
    val stateDigest: String
)

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
 * When [hidden] is true (opponent's hand, any library), [cards] is empty and
 * only [size] is meaningful. This mirrors real-MTG information hiding.
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
 * - In the library/hand/graveyard/exile: the card's static properties are set;
 *   dynamic properties (tapped, damage, counters) default to their "not present" values.
 */
@Serializable
data class EntityFeatures(
    val entityId: EntityId,
    val cardDefinitionId: String?,
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
    val controllerId: EntityId?,
    val name: String,
    val kind: StackItemKind,
    /** Printed oracle text of the card backing this stack item — empty for stackless triggers. */
    val oracleText: String = "",
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
    SEARCH_LIBRARY,
    REORDER_LIBRARY,
    ASSIGN_DAMAGE,
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
