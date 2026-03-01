package com.wingedsheep.engine.core

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a decision the engine needs from a player before it can continue.
 *
 * When the engine encounters a point where player input is required (choosing targets,
 * selecting cards, making choices), it pauses and returns a PendingDecision describing
 * what input is needed.
 *
 * The game state is frozen until the player submits a DecisionResponse via SubmitDecision action.
 */
@Serializable
sealed interface PendingDecision {
    /** Unique identifier for this decision (used to correlate responses) */
    val id: String

    /** The player who must make this decision */
    val playerId: EntityId

    /** Human-readable prompt to display */
    val prompt: String

    /** Context about what triggered this decision (for UI display) */
    val context: DecisionContext
}

/**
 * Context information about why this decision is being requested.
 */
@Serializable
data class DecisionContext(
    /** The spell/ability that caused this decision (if any) */
    val sourceId: EntityId? = null,

    /** Name of the source for display purposes */
    val sourceName: String? = null,

    /** What phase of execution we're in */
    val phase: DecisionPhase = DecisionPhase.RESOLUTION,

    /** The entity that triggered this decision (e.g., the blocked creature for combat triggers) */
    val triggeringEntityId: EntityId? = null
)

/**
 * The phase of game execution when this decision was triggered.
 */
@Serializable
enum class DecisionPhase {
    /** During spell/ability casting (choosing targets, modes) */
    CASTING,

    /** During spell/ability resolution */
    RESOLUTION,

    /** During combat (declaring attackers/blockers) */
    COMBAT,

    /** State-based actions (legend rule, etc.) */
    STATE_BASED,

    /** Triggered ability handling */
    TRIGGER
}

// =============================================================================
// Specific Decision Types
// =============================================================================

/**
 * Player must choose targets for a spell or ability.
 *
 * @property targetRequirements Describes each target that needs to be chosen
 * @property legalTargets Map of requirement index to list of valid target IDs
 */
@Serializable
@SerialName("ChooseTargetsDecision")
data class ChooseTargetsDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val targetRequirements: List<TargetRequirementInfo>,
    val legalTargets: Map<Int, List<EntityId>>
) : PendingDecision

/**
 * Information about a single target requirement.
 */
@Serializable
data class TargetRequirementInfo(
    val index: Int,
    val description: String,
    val minTargets: Int = 1,
    val maxTargets: Int = 1
)

/**
 * Player must select cards from a set (e.g., discard, sacrifice, search library).
 *
 * @property options The entity IDs that can be selected
 * @property minSelections Minimum number that must be selected
 * @property maxSelections Maximum number that can be selected
 * @property cardInfo Optional card info for displaying hidden cards (e.g., opponent's library)
 * @property useTargetingUI If true, use the targeting UI (click on battlefield) instead of modal
 */
@Serializable
@SerialName("SelectCardsDecision")
data class SelectCardsDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val options: List<EntityId>,
    val minSelections: Int,
    val maxSelections: Int,
    /** Whether the order of selection matters */
    val ordered: Boolean = false,
    /** Card info for displaying hidden cards (null if cards are visible to the player) */
    val cardInfo: Map<EntityId, SearchCardInfo>? = null,
    /** If true, use the targeting UI (click on battlefield) instead of modal overlay */
    val useTargetingUI: Boolean = false,
    /** Label describing where selected cards go (e.g., "Put on bottom") */
    val selectedLabel: String? = null,
    /** Label describing where non-selected cards go (e.g., "Put on top") */
    val remainderLabel: String? = null
) : PendingDecision

/**
 * Player must make a yes/no decision (may abilities).
 */
@Serializable
@SerialName("YesNoDecision")
data class YesNoDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    /** What "yes" means (for UI button text) */
    val yesText: String = "Yes",
    /** What "no" means (for UI button text) */
    val noText: String = "No"
) : PendingDecision

/**
 * Player must choose from multiple modes (modal spells like Cryptic Command).
 *
 * @property modes Available modes with descriptions
 * @property minModes Minimum modes to select
 * @property maxModes Maximum modes to select
 */
@Serializable
@SerialName("ChooseModeDecision")
data class ChooseModeDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val modes: List<ModeOption>,
    val minModes: Int = 1,
    val maxModes: Int = 1
) : PendingDecision

/**
 * A single mode option.
 */
@Serializable
data class ModeOption(
    val index: Int,
    val text: String,
    val available: Boolean = true
)

/**
 * Player must choose a color (e.g., protection from color of your choice).
 */
@Serializable
@SerialName("ChooseColorDecision")
data class ChooseColorDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val availableColors: Set<Color> = Color.entries.toSet()
) : PendingDecision

/**
 * Player must choose a number (e.g., X value, divide damage).
 */
@Serializable
@SerialName("ChooseNumberDecision")
data class ChooseNumberDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val minValue: Int,
    val maxValue: Int
) : PendingDecision

/**
 * Player must distribute a value among targets (e.g., divide 4 damage).
 *
 * @property totalAmount The total to distribute
 * @property targets The targets to distribute among
 * @property minPerTarget Minimum each target must receive (usually 1 for damage)
 */
@Serializable
@SerialName("DistributeDecision")
data class DistributeDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val totalAmount: Int,
    val targets: List<EntityId>,
    val minPerTarget: Int = 0,
    val maxPerTarget: Map<EntityId, Int> = emptyMap(),
    val allowPartial: Boolean = false
) : PendingDecision

/**
 * Player must order objects (e.g., damage assignment order, scry).
 *
 * @property objects The entity IDs that need to be ordered
 * @property cardInfo Optional card info for UI display (used for blocker ordering in combat)
 */
@Serializable
@SerialName("OrderObjectsDecision")
data class OrderObjectsDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val objects: List<EntityId>,
    val cardInfo: Map<EntityId, SearchCardInfo>? = null
) : PendingDecision

/**
 * Player must split cards into piles (e.g., Fact or Fiction).
 */
@Serializable
@SerialName("SplitPilesDecision")
data class SplitPilesDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val cards: List<EntityId>,
    val numberOfPiles: Int = 2,
    /** Labels for the piles (e.g., ["Keep", "Discard"]) */
    val pileLabels: List<String> = emptyList(),
    /** Card info for displaying hidden cards (e.g., top of library during surveil) */
    val cardInfo: Map<EntityId, SearchCardInfo>? = null
) : PendingDecision

/**
 * Player must choose from a fixed set of options (generic choice).
 */
@Serializable
@SerialName("ChooseOptionDecision")
data class ChooseOptionDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val options: List<String>,
    val defaultSearch: String? = null,
    /** Maps option index to entity IDs of cards associated with that option (for preview) */
    val optionCardIds: Map<Int, List<EntityId>>? = null
) : PendingDecision

/**
 * Player must assign combat damage from an attacker to blockers.
 *
 * Per CR 510.1c: Damage must be assigned in order. A creature cannot be
 * assigned damage until all creatures before it in the order have been
 * assigned lethal damage.
 *
 * @property attackerId The attacking creature assigning damage
 * @property availablePower Total damage available to assign
 * @property orderedTargets Blockers in damage assignment order (first = first to receive damage)
 * @property defenderId The defending player (only receives damage if attacker has trample)
 * @property minimumAssignments Minimum damage each blocker must receive (lethal damage)
 * @property defaultAssignments Pre-computed optimal damage distribution (lethal to each blocker in order, remainder to last/player)
 * @property hasTrample Whether excess damage can go to defending player
 * @property hasDeathtouch If true, 1 damage is lethal to any creature
 */
@Serializable
@SerialName("AssignDamageDecision")
data class AssignDamageDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val attackerId: EntityId,
    val availablePower: Int,
    val orderedTargets: List<EntityId>,
    val defenderId: EntityId?,
    val minimumAssignments: Map<EntityId, Int>,
    val defaultAssignments: Map<EntityId, Int>,
    val hasTrample: Boolean,
    val hasDeathtouch: Boolean
) : PendingDecision

/**
 * Information about a card available for selection during library search.
 * This is embedded in the decision because library cards are normally hidden.
 */
@Serializable
data class SearchCardInfo(
    val name: String,
    val manaCost: String,
    val typeLine: String,
    val imageUri: String? = null
)

/**
 * Player must select cards from their library.
 *
 * Unlike SelectCardsDecision, this includes embedded card info because
 * library contents are normally hidden from the client. The client needs
 * the card metadata to display the search UI.
 *
 * @property options The entity IDs of cards matching the filter
 * @property minSelections Minimum cards to select (0 allows "fail to find")
 * @property maxSelections Maximum cards to select
 * @property cards Map of entity ID to card info for UI display
 * @property filterDescription Human-readable description of the filter
 */
@Serializable
@SerialName("SearchLibraryDecision")
data class SearchLibraryDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val options: List<EntityId>,
    val minSelections: Int,
    val maxSelections: Int,
    val cards: Map<EntityId, SearchCardInfo>,
    val filterDescription: String
) : PendingDecision

/**
 * Player must reorder cards from the top of their library.
 *
 * Used for "look at the top N cards and put them back in any order" effects
 * like Omen. The client displays the cards with drag-and-drop reordering,
 * with a clear indication of which end is the top of the library.
 *
 * @property cards List of entity IDs in their current order (first = top of library)
 * @property cardInfo Map of entity ID to card info for UI display
 */
@Serializable
@SerialName("ReorderLibraryDecision")
data class ReorderLibraryDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val cards: List<EntityId>,
    val cardInfo: Map<EntityId, SearchCardInfo>
) : PendingDecision

/**
 * Player must select mana sources to pay a cost (e.g., Lightning Rift's {1}).
 *
 * Shown after the player agrees to pay, before target selection.
 * Includes an "Auto Pay" shortcut that uses the solver's suggestion.
 *
 * @property availableSources List of mana source entity IDs + metadata for display
 * @property requiredCost The mana cost string to display
 * @property autoPaySuggestion Pre-computed auto-tap suggestion (entity IDs to tap)
 */
@Serializable
@SerialName("SelectManaSourcesDecision")
data class SelectManaSourcesDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val availableSources: List<ManaSourceOption>,
    val requiredCost: String,
    val autoPaySuggestion: List<EntityId>,
    val canDecline: Boolean = false
) : PendingDecision

// =============================================================================
// Decision Responses
// =============================================================================

/**
 * A player's response to a PendingDecision.
 */
@Serializable
sealed interface DecisionResponse {
    /** Must match the PendingDecision.id */
    val decisionId: String
}

/**
 * Response to ChooseTargetsDecision.
 */
@Serializable
@SerialName("TargetsResponse")
data class TargetsResponse(
    override val decisionId: String,
    /** Map of requirement index to chosen target IDs */
    val selectedTargets: Map<Int, List<EntityId>>
) : DecisionResponse

/**
 * Response to SelectCardsDecision.
 */
@Serializable
@SerialName("CardsSelectedResponse")
data class CardsSelectedResponse(
    override val decisionId: String,
    val selectedCards: List<EntityId>
) : DecisionResponse

/**
 * Response to YesNoDecision.
 */
@Serializable
@SerialName("YesNoResponse")
data class YesNoResponse(
    override val decisionId: String,
    val choice: Boolean
) : DecisionResponse

/**
 * Response to ChooseModeDecision.
 */
@Serializable
@SerialName("ModesChosenResponse")
data class ModesChosenResponse(
    override val decisionId: String,
    val selectedModes: List<Int>
) : DecisionResponse

/**
 * Response to ChooseColorDecision.
 */
@Serializable
@SerialName("ColorChosenResponse")
data class ColorChosenResponse(
    override val decisionId: String,
    val color: Color
) : DecisionResponse

/**
 * Response to ChooseNumberDecision.
 */
@Serializable
@SerialName("NumberChosenResponse")
data class NumberChosenResponse(
    override val decisionId: String,
    val number: Int
) : DecisionResponse

/**
 * Response to DistributeDecision.
 */
@Serializable
@SerialName("DistributionResponse")
data class DistributionResponse(
    override val decisionId: String,
    /** Map of target ID to amount assigned */
    val distribution: Map<EntityId, Int>
) : DecisionResponse

/**
 * Response to OrderObjectsDecision.
 */
@Serializable
@SerialName("OrderedResponse")
data class OrderedResponse(
    override val decisionId: String,
    val orderedObjects: List<EntityId>
) : DecisionResponse

/**
 * Response to SplitPilesDecision.
 */
@Serializable
@SerialName("PilesSplitResponse")
data class PilesSplitResponse(
    override val decisionId: String,
    /** List of piles, each containing entity IDs */
    val piles: List<List<EntityId>>
) : DecisionResponse

/**
 * Response to ChooseOptionDecision.
 */
@Serializable
@SerialName("OptionChosenResponse")
data class OptionChosenResponse(
    override val decisionId: String,
    val optionIndex: Int
) : DecisionResponse

/**
 * Response to AssignDamageDecision.
 */
@Serializable
@SerialName("DamageAssignmentResponse")
data class DamageAssignmentResponse(
    override val decisionId: String,
    /** Map of target ID (creature or player) to damage amount */
    val assignments: Map<EntityId, Int>
) : DecisionResponse

/**
 * Response to SelectManaSourcesDecision.
 *
 * @property selectedSources Entity IDs of the mana sources to tap (ignored if autoPay is true)
 * @property autoPay If true, use the auto-tap suggestion instead of manual selection
 */
@Serializable
@SerialName("ManaSourcesSelectedResponse")
data class ManaSourcesSelectedResponse(
    override val decisionId: String,
    val selectedSources: List<EntityId> = emptyList(),
    val autoPay: Boolean = false
) : DecisionResponse
