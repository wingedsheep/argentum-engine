package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.targeting.Target
import com.wingedsheep.rulesengine.targeting.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Represents a decision that a player needs to make during the game.
 * Each decision type includes the context needed to make the decision
 * and defines what a valid response looks like.
 */
@Serializable
sealed interface PlayerDecision {
    /** The player who must make this decision */
    val playerId: PlayerId

    /** Human-readable description of what the player needs to decide */
    val description: String
}

// =============================================================================
// Target Selection
// =============================================================================

/**
 * Player must choose targets for a spell or ability.
 */
@Serializable
data class ChooseTargets(
    override val playerId: PlayerId,
    val sourceCardId: CardId,
    val sourceName: String,
    val requirements: List<TargetRequirement>,
    val legalTargets: Map<Int, List<Target>> // Index -> list of legal targets for that requirement
) : PlayerDecision {
    override val description: String = "Choose targets for $sourceName"
}

/**
 * Response to a ChooseTargets decision.
 */
@Serializable
data class TargetsChoice(
    val selectedTargets: Map<Int, List<Target>> // Index -> selected targets for that requirement
) : DecisionResponse

// =============================================================================
// Combat Decisions
// =============================================================================

/**
 * Player must choose which creatures to attack with.
 */
@Serializable
data class ChooseAttackers(
    override val playerId: PlayerId,
    val legalAttackers: List<CardId>,
    val defendingPlayer: PlayerId
) : PlayerDecision {
    override val description: String = "Choose attackers"
}

/**
 * Response to a ChooseAttackers decision.
 */
@Serializable
data class AttackersChoice(
    val attackerIds: List<CardId>
) : DecisionResponse

/**
 * Player must choose which creatures to block with and what they block.
 */
@Serializable
data class ChooseBlockers(
    override val playerId: PlayerId,
    val legalBlockers: List<CardId>,
    val attackers: List<CardId>,
    /** Map of blocker -> list of attackers it can legally block */
    val legalBlocks: Map<CardId, List<CardId>>
) : PlayerDecision {
    override val description: String = "Choose blockers"
}

/**
 * Response to a ChooseBlockers decision.
 */
@Serializable
data class BlockersChoice(
    /** Map of blocker -> attacker it is blocking */
    val blocks: Map<CardId, CardId>
) : DecisionResponse

/**
 * Player must choose the order in which blockers receive damage.
 */
@Serializable
data class ChooseDamageAssignmentOrder(
    override val playerId: PlayerId,
    val attackerId: CardId,
    val blockerIds: List<CardId>
) : PlayerDecision {
    override val description: String = "Choose damage assignment order for blockers"
}

/**
 * Response to a ChooseDamageAssignmentOrder decision.
 */
@Serializable
data class DamageAssignmentOrderChoice(
    val orderedBlockerIds: List<CardId>
) : DecisionResponse

// =============================================================================
// Mana Decisions
// =============================================================================

/**
 * Player must choose how to pay a mana cost.
 * This is used when there are multiple ways to pay (e.g., generic mana can be paid with any color).
 */
@Serializable
data class ChooseManaPayment(
    override val playerId: PlayerId,
    val cardName: String,
    val requiredWhite: Int = 0,
    val requiredBlue: Int = 0,
    val requiredBlack: Int = 0,
    val requiredRed: Int = 0,
    val requiredGreen: Int = 0,
    val requiredColorless: Int = 0,
    val requiredGeneric: Int = 0,
    val availableMana: Map<Color, Int>,
    val availableColorless: Int
) : PlayerDecision {
    override val description: String = "Choose mana payment for $cardName"
}

/**
 * Response to a ChooseManaPayment decision.
 * Specifies how much of each color to spend on generic costs.
 */
@Serializable
data class ManaPaymentChoice(
    val whiteForGeneric: Int = 0,
    val blueForGeneric: Int = 0,
    val blackForGeneric: Int = 0,
    val redForGeneric: Int = 0,
    val greenForGeneric: Int = 0,
    val colorlessForGeneric: Int = 0
) : DecisionResponse {
    val totalForGeneric: Int
        get() = whiteForGeneric + blueForGeneric + blackForGeneric + redForGeneric + greenForGeneric + colorlessForGeneric
}

// =============================================================================
// Yes/No Decisions
// =============================================================================

/**
 * Player must make a yes/no decision.
 * Used for optional effects, "may" abilities, etc.
 */
@Serializable
data class YesNoDecision(
    override val playerId: PlayerId,
    override val description: String,
    val sourceCardId: CardId? = null,
    val sourceName: String? = null
) : PlayerDecision

/**
 * Response to a YesNoDecision.
 */
@Serializable
data class YesNoChoice(
    val choice: Boolean
) : DecisionResponse

// =============================================================================
// Card Selection
// =============================================================================

/**
 * Player must choose one or more cards from a set of options.
 * Used for mulligan, discard, sacrifice, etc.
 */
@Serializable
data class ChooseCards(
    override val playerId: PlayerId,
    override val description: String,
    val cards: List<CardId>,
    val minCount: Int,
    val maxCount: Int,
    val sourceCardId: CardId? = null,
    val sourceName: String? = null
) : PlayerDecision {
    /** Whether a specific number of cards must be chosen */
    val isExactCount: Boolean get() = minCount == maxCount
}

/**
 * Response to a ChooseCards decision.
 */
@Serializable
data class CardsChoice(
    val selectedCardIds: List<CardId>
) : DecisionResponse

// =============================================================================
// Order Selection
// =============================================================================

/**
 * Player must order a set of items (cards, triggers, etc.).
 * Used for APNAP ordering, library manipulation, etc.
 */
@Serializable
data class ChooseOrder<T>(
    override val playerId: PlayerId,
    override val description: String,
    val items: List<T>,
    val itemDescriptions: List<String>
) : PlayerDecision

/**
 * Response to a ChooseOrder decision.
 */
@Serializable
data class OrderChoice(
    val orderedIndices: List<Int>
) : DecisionResponse

// =============================================================================
// Modal Choices
// =============================================================================

/**
 * Player must choose one or more modes from available options.
 * Used for modal spells and abilities.
 */
@Serializable
data class ChooseMode(
    override val playerId: PlayerId,
    val sourceCardId: CardId,
    val sourceName: String,
    val modes: List<ModeOption>,
    val minModes: Int = 1,
    val maxModes: Int = 1,
    val canRepeatModes: Boolean = false
) : PlayerDecision {
    override val description: String = "Choose mode for $sourceName"
}

/**
 * A single mode option.
 */
@Serializable
data class ModeOption(
    val index: Int,
    val description: String,
    val isAvailable: Boolean = true
)

/**
 * Response to a ChooseMode decision.
 */
@Serializable
data class ModeChoice(
    val selectedModeIndices: List<Int>
) : DecisionResponse

// =============================================================================
// Number Selection
// =============================================================================

/**
 * Player must choose a number (for X costs, divide damage, etc.).
 */
@Serializable
data class ChooseNumber(
    override val playerId: PlayerId,
    override val description: String,
    val minimum: Int,
    val maximum: Int,
    val sourceCardId: CardId? = null,
    val sourceName: String? = null
) : PlayerDecision

/**
 * Response to a ChooseNumber decision.
 */
@Serializable
data class NumberChoice(
    val number: Int
) : DecisionResponse

// =============================================================================
// Priority/Pass Decisions
// =============================================================================

/**
 * Player has priority and must decide whether to act or pass.
 */
@Serializable
data class PriorityDecision(
    override val playerId: PlayerId,
    val canCastSpells: Boolean,
    val canActivateAbilities: Boolean,
    val canPlayLand: Boolean,
    val stackIsEmpty: Boolean
) : PlayerDecision {
    override val description: String = "You have priority"
}

/**
 * Response to a PriorityDecision.
 */
@Serializable
sealed interface PriorityChoice : DecisionResponse {
    @Serializable
    data object Pass : PriorityChoice

    @Serializable
    data class CastSpell(val cardId: CardId) : PriorityChoice

    @Serializable
    data class ActivateAbility(val sourceCardId: CardId, val abilityIndex: Int = 0) : PriorityChoice

    @Serializable
    data class PlayLand(val cardId: CardId) : PriorityChoice
}

// =============================================================================
// Mulligan Decision
// =============================================================================

/**
 * Player must decide whether to mulligan.
 */
@Serializable
data class MulliganDecision(
    override val playerId: PlayerId,
    val handSize: Int,
    val mulliganNumber: Int
) : PlayerDecision {
    override val description: String = if (mulliganNumber == 0) {
        "Keep your opening hand or mulligan?"
    } else {
        "Keep this hand (putting $mulliganNumber card(s) on the bottom) or mulligan again?"
    }
}

/**
 * Response to a MulliganDecision.
 */
@Serializable
sealed interface MulliganChoice : DecisionResponse {
    @Serializable
    data object Keep : MulliganChoice

    @Serializable
    data object Mulligan : MulliganChoice
}

/**
 * Player must choose cards to put on the bottom of their library after mulliganing.
 */
@Serializable
data class ChooseMulliganBottomCards(
    override val playerId: PlayerId,
    val hand: List<CardId>,
    val cardsToPutOnBottom: Int
) : PlayerDecision {
    override val description: String = "Choose $cardsToPutOnBottom card(s) to put on the bottom of your library"
}

// =============================================================================
// Base Response Interface
// =============================================================================

/**
 * Marker interface for all decision responses.
 */
@Serializable
sealed interface DecisionResponse
