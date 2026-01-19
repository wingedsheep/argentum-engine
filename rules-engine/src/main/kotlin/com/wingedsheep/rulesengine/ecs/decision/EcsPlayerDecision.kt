package com.wingedsheep.rulesengine.ecs.decision

import com.wingedsheep.rulesengine.ability.CardFilter
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Represents a decision that a player needs to make during the game.
 * ECS-compatible version using EntityId instead of legacy CardId/PlayerId.
 *
 * When an effect handler needs player input, it returns a PendingDecision
 * in the ExecutionResult. The game loop presents this to the player,
 * collects the response, and continues the effect with the selection.
 */
@Serializable
sealed interface EcsPlayerDecision {
    /** The player who must make this decision */
    val playerId: EntityId

    /** Human-readable description of what the player needs to decide */
    val description: String

    /** Unique identifier for this decision (used to match responses) */
    val decisionId: String
}

// =============================================================================
// Card Selection Decisions
// =============================================================================

/**
 * Player must choose one or more cards from a set of options.
 * Used for library search, discard, sacrifice, etc.
 *
 * @param playerId The player who must choose
 * @param description Human-readable description
 * @param cards The cards available to choose from (with their names for display)
 * @param minCount Minimum number of cards that must be chosen
 * @param maxCount Maximum number of cards that can be chosen
 * @param mayChooseNone If true, player can choose 0 cards even if minCount > 0 ("may" effects)
 * @param sourceEntityId The entity that created this decision (spell/ability)
 * @param sourceName Human-readable name of the source
 * @param filter The filter being applied (for display purposes)
 */
@Serializable
data class EcsChooseCards(
    override val playerId: EntityId,
    override val description: String,
    override val decisionId: String,
    val cards: List<CardOption>,
    val minCount: Int,
    val maxCount: Int,
    val mayChooseNone: Boolean = false,
    val sourceEntityId: EntityId? = null,
    val sourceName: String? = null,
    val filterDescription: String? = null
) : EcsPlayerDecision {
    /** Whether a specific number of cards must be chosen */
    val isExactCount: Boolean get() = minCount == maxCount
}

/**
 * A card that can be selected, with display information.
 */
@Serializable
data class CardOption(
    val entityId: EntityId,
    val name: String,
    val typeLine: String? = null,
    val manaCost: String? = null
)

/**
 * Response to an EcsChooseCards decision.
 */
@Serializable
data class EcsCardsChoice(
    override val decisionId: String,
    val selectedCardIds: List<EntityId>
) : EcsDecisionResponse

// =============================================================================
// Yes/No Decisions
// =============================================================================

/**
 * Player must make a yes/no decision.
 * Used for optional effects, "may" abilities, etc.
 */
@Serializable
data class EcsYesNoDecision(
    override val playerId: EntityId,
    override val description: String,
    override val decisionId: String,
    val sourceEntityId: EntityId? = null,
    val sourceName: String? = null
) : EcsPlayerDecision

/**
 * Response to an EcsYesNoDecision.
 */
@Serializable
data class EcsYesNoChoice(
    override val decisionId: String,
    val choice: Boolean
) : EcsDecisionResponse

// =============================================================================
// Sacrifice Unless Decisions
// =============================================================================

/**
 * Player must decide whether to pay a sacrifice cost or sacrifice a permanent.
 * Used for cards like Primeval Force: "sacrifice it unless you sacrifice three Forests"
 *
 * @param permanentToSacrifice The permanent that will be sacrificed if cost not paid
 * @param permanentName Name of the permanent for display
 * @param costDescription Description of the cost (e.g., "three Forests")
 * @param validCostTargets The permanents that can be sacrificed to pay the cost
 * @param requiredCount How many permanents must be sacrificed
 */
@Serializable
data class EcsSacrificeUnlessDecision(
    override val playerId: EntityId,
    override val description: String,
    override val decisionId: String,
    val permanentToSacrifice: EntityId,
    val permanentName: String,
    val costDescription: String,
    val validCostTargets: List<CardOption>,
    val requiredCount: Int,
    val sourceEntityId: EntityId? = null
) : EcsPlayerDecision {
    /** Whether the player can pay the cost (has enough valid targets) */
    val canPayCost: Boolean get() = validCostTargets.size >= requiredCount
}

/**
 * Response to an EcsSacrificeUnlessDecision.
 *
 * @param payCost If true, player chose to pay the cost by sacrificing the selected permanents.
 *                If false, player chose to sacrifice the permanent instead.
 * @param sacrificedPermanents The permanents sacrificed to pay the cost (only used if payCost=true)
 */
@Serializable
data class EcsSacrificeUnlessChoice(
    override val decisionId: String,
    val payCost: Boolean,
    val sacrificedPermanents: List<EntityId> = emptyList()
) : EcsDecisionResponse

// =============================================================================
// Base Response Interface
// =============================================================================

/**
 * Marker interface for all ECS decision responses.
 */
@Serializable
sealed interface EcsDecisionResponse {
    val decisionId: String
}
