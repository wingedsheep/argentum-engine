package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.targeting.Target
import com.wingedsheep.rulesengine.targeting.TargetRequirement
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a decision that a player needs to make during the game.
 * Each decision type includes the context needed to make the decision
 * and defines what a valid response looks like.
 *
 * When an effect handler needs player input, it returns a decision
 * in the result. The game loop presents this to the player,
 * collects the response, and continues with the selection.
 */
@Serializable
sealed interface PlayerDecision {
    /** Unique identifier for this decision (used to match responses) */
    val decisionId: String

    /** The player who must make this decision */
    val playerId: EntityId

    /** Human-readable description of what the player needs to decide */
    val description: String

    companion object {
        fun generateId(): String = UUID.randomUUID().toString()
    }
}

// =============================================================================
// Target Selection
// =============================================================================

/**
 * Player must choose targets for a spell or ability.
 */
@Serializable
data class ChooseTargets(
    override val decisionId: String,
    override val playerId: EntityId,
    val sourceEntityId: EntityId,
    val sourceName: String,
    val requirements: List<TargetRequirement>,
    val legalTargets: Map<Int, List<Target>> // Index -> list of legal targets for that requirement
) : PlayerDecision {
    override val description: String = "Choose targets for $sourceName"

    companion object {
        fun create(
            playerId: EntityId,
            sourceEntityId: EntityId,
            sourceName: String,
            requirements: List<TargetRequirement>,
            legalTargets: Map<Int, List<Target>>
        ) = ChooseTargets(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            sourceEntityId = sourceEntityId,
            sourceName = sourceName,
            requirements = requirements,
            legalTargets = legalTargets
        )
    }
}

/**
 * Response to a ChooseTargets decision.
 */
@Serializable
data class TargetsChoice(
    override val decisionId: String,
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
    override val decisionId: String,
    override val playerId: EntityId,
    val legalAttackers: List<EntityId>,
    val defendingPlayer: EntityId
) : PlayerDecision {
    override val description: String = "Choose attackers"

    companion object {
        fun create(
            playerId: EntityId,
            legalAttackers: List<EntityId>,
            defendingPlayer: EntityId
        ) = ChooseAttackers(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            legalAttackers = legalAttackers,
            defendingPlayer = defendingPlayer
        )
    }
}

/**
 * Response to a ChooseAttackers decision.
 */
@Serializable
data class AttackersChoice(
    override val decisionId: String,
    val attackerIds: List<EntityId>
) : DecisionResponse

/**
 * Player must choose which creatures to block with and what they block.
 */
@Serializable
data class ChooseBlockers(
    override val decisionId: String,
    override val playerId: EntityId,
    val legalBlockers: List<EntityId>,
    val attackers: List<EntityId>,
    /** Map of blocker -> list of attackers it can legally block */
    val legalBlocks: Map<EntityId, List<EntityId>>
) : PlayerDecision {
    override val description: String = "Choose blockers"

    companion object {
        fun create(
            playerId: EntityId,
            legalBlockers: List<EntityId>,
            attackers: List<EntityId>,
            legalBlocks: Map<EntityId, List<EntityId>>
        ) = ChooseBlockers(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            legalBlockers = legalBlockers,
            attackers = attackers,
            legalBlocks = legalBlocks
        )
    }
}

/**
 * Response to a ChooseBlockers decision.
 */
@Serializable
data class BlockersChoice(
    override val decisionId: String,
    /** Map of blocker -> attacker it is blocking */
    val blocks: Map<EntityId, EntityId>
) : DecisionResponse

/**
 * Player must choose the order in which blockers receive damage.
 */
@Serializable
data class ChooseDamageAssignmentOrder(
    override val decisionId: String,
    override val playerId: EntityId,
    val attackerId: EntityId,
    val blockerIds: List<EntityId>
) : PlayerDecision {
    override val description: String = "Choose damage assignment order for blockers"

    companion object {
        fun create(
            playerId: EntityId,
            attackerId: EntityId,
            blockerIds: List<EntityId>
        ) = ChooseDamageAssignmentOrder(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            attackerId = attackerId,
            blockerIds = blockerIds
        )
    }
}

/**
 * Response to a ChooseDamageAssignmentOrder decision.
 */
@Serializable
data class DamageAssignmentOrderChoice(
    override val decisionId: String,
    val orderedBlockerIds: List<EntityId>
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
    override val decisionId: String,
    override val playerId: EntityId,
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

    companion object {
        fun create(
            playerId: EntityId,
            cardName: String,
            requiredWhite: Int = 0,
            requiredBlue: Int = 0,
            requiredBlack: Int = 0,
            requiredRed: Int = 0,
            requiredGreen: Int = 0,
            requiredColorless: Int = 0,
            requiredGeneric: Int = 0,
            availableMana: Map<Color, Int>,
            availableColorless: Int
        ) = ChooseManaPayment(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            cardName = cardName,
            requiredWhite = requiredWhite,
            requiredBlue = requiredBlue,
            requiredBlack = requiredBlack,
            requiredRed = requiredRed,
            requiredGreen = requiredGreen,
            requiredColorless = requiredColorless,
            requiredGeneric = requiredGeneric,
            availableMana = availableMana,
            availableColorless = availableColorless
        )
    }
}

/**
 * Response to a ChooseManaPayment decision.
 * Specifies how much of each color to spend on generic costs.
 */
@Serializable
data class ManaPaymentChoice(
    override val decisionId: String,
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
    override val decisionId: String,
    override val playerId: EntityId,
    override val description: String,
    val sourceEntityId: EntityId? = null,
    val sourceName: String? = null
) : PlayerDecision {
    companion object {
        fun create(
            playerId: EntityId,
            description: String,
            sourceEntityId: EntityId? = null,
            sourceName: String? = null
        ) = YesNoDecision(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            description = description,
            sourceEntityId = sourceEntityId,
            sourceName = sourceName
        )
    }
}

/**
 * Response to a YesNoDecision.
 */
@Serializable
data class YesNoChoice(
    override val decisionId: String,
    val choice: Boolean
) : DecisionResponse

// =============================================================================
// Card Selection
// =============================================================================

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
 * Player must choose one or more cards from a set of options.
 * Used for mulligan, discard, sacrifice, library search, etc.
 *
 * @param cards The cards available to choose from (with display info)
 * @param minCount Minimum number of cards that must be chosen
 * @param maxCount Maximum number of cards that can be chosen
 * @param mayChooseNone If true, player can choose 0 cards even if minCount > 0 ("may" effects)
 * @param filterDescription Description of the filter being applied (for display purposes)
 */
@Serializable
data class ChooseCards(
    override val decisionId: String,
    override val playerId: EntityId,
    override val description: String,
    val cards: List<CardOption>,
    val minCount: Int,
    val maxCount: Int,
    val mayChooseNone: Boolean = false,
    val sourceEntityId: EntityId? = null,
    val sourceName: String? = null,
    val filterDescription: String? = null
) : PlayerDecision {
    /** Whether a specific number of cards must be chosen */
    val isExactCount: Boolean get() = minCount == maxCount

    /** Get just the entity IDs for backward compatibility */
    val cardIds: List<EntityId> get() = cards.map { it.entityId }

    companion object {
        fun create(
            playerId: EntityId,
            description: String,
            cards: List<CardOption>,
            minCount: Int,
            maxCount: Int,
            mayChooseNone: Boolean = false,
            sourceEntityId: EntityId? = null,
            sourceName: String? = null,
            filterDescription: String? = null
        ) = ChooseCards(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            description = description,
            cards = cards,
            minCount = minCount,
            maxCount = maxCount,
            mayChooseNone = mayChooseNone,
            sourceEntityId = sourceEntityId,
            sourceName = sourceName,
            filterDescription = filterDescription
        )

        /** Create from entity IDs only (for simpler cases) */
        fun fromEntityIds(
            playerId: EntityId,
            description: String,
            cardIds: List<EntityId>,
            minCount: Int,
            maxCount: Int,
            sourceEntityId: EntityId? = null,
            sourceName: String? = null
        ) = ChooseCards(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            description = description,
            cards = cardIds.map { CardOption(it, "Card") },
            minCount = minCount,
            maxCount = maxCount,
            sourceEntityId = sourceEntityId,
            sourceName = sourceName
        )
    }
}

/**
 * Response to a ChooseCards decision.
 */
@Serializable
data class CardsChoice(
    override val decisionId: String,
    val selectedCardIds: List<EntityId>
) : DecisionResponse

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
data class SacrificeUnlessDecision(
    override val decisionId: String,
    override val playerId: EntityId,
    override val description: String,
    val permanentToSacrifice: EntityId,
    val permanentName: String,
    val costDescription: String,
    val validCostTargets: List<CardOption>,
    val requiredCount: Int,
    val sourceEntityId: EntityId? = null
) : PlayerDecision {
    /** Whether the player can pay the cost (has enough valid targets) */
    val canPayCost: Boolean get() = validCostTargets.size >= requiredCount

    companion object {
        fun create(
            playerId: EntityId,
            description: String,
            permanentToSacrifice: EntityId,
            permanentName: String,
            costDescription: String,
            validCostTargets: List<CardOption>,
            requiredCount: Int,
            sourceEntityId: EntityId? = null
        ) = SacrificeUnlessDecision(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            description = description,
            permanentToSacrifice = permanentToSacrifice,
            permanentName = permanentName,
            costDescription = costDescription,
            validCostTargets = validCostTargets,
            requiredCount = requiredCount,
            sourceEntityId = sourceEntityId
        )
    }
}

/**
 * Response to a SacrificeUnlessDecision.
 *
 * @param payCost If true, player chose to pay the cost by sacrificing the selected permanents.
 *                If false, player chose to sacrifice the permanent instead.
 * @param sacrificedPermanents The permanents sacrificed to pay the cost (only used if payCost=true)
 */
@Serializable
data class SacrificeUnlessChoice(
    override val decisionId: String,
    val payCost: Boolean,
    val sacrificedPermanents: List<EntityId> = emptyList()
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
    override val decisionId: String,
    override val playerId: EntityId,
    override val description: String,
    val items: List<T>,
    val itemDescriptions: List<String>
) : PlayerDecision {
    companion object {
        fun <T> create(
            playerId: EntityId,
            description: String,
            items: List<T>,
            itemDescriptions: List<String>
        ) = ChooseOrder(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            description = description,
            items = items,
            itemDescriptions = itemDescriptions
        )
    }
}

/**
 * Response to a ChooseOrder decision.
 */
@Serializable
data class OrderChoice(
    override val decisionId: String,
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
    override val decisionId: String,
    override val playerId: EntityId,
    val sourceEntityId: EntityId,
    val sourceName: String,
    val modes: List<ModeOption>,
    val minModes: Int = 1,
    val maxModes: Int = 1,
    val canRepeatModes: Boolean = false
) : PlayerDecision {
    override val description: String = "Choose mode for $sourceName"

    companion object {
        fun create(
            playerId: EntityId,
            sourceEntityId: EntityId,
            sourceName: String,
            modes: List<ModeOption>,
            minModes: Int = 1,
            maxModes: Int = 1,
            canRepeatModes: Boolean = false
        ) = ChooseMode(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            sourceEntityId = sourceEntityId,
            sourceName = sourceName,
            modes = modes,
            minModes = minModes,
            maxModes = maxModes,
            canRepeatModes = canRepeatModes
        )
    }
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
    override val decisionId: String,
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
    override val decisionId: String,
    override val playerId: EntityId,
    override val description: String,
    val minimum: Int,
    val maximum: Int,
    val sourceEntityId: EntityId? = null,
    val sourceName: String? = null
) : PlayerDecision {
    companion object {
        fun create(
            playerId: EntityId,
            description: String,
            minimum: Int,
            maximum: Int,
            sourceEntityId: EntityId? = null,
            sourceName: String? = null
        ) = ChooseNumber(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            description = description,
            minimum = minimum,
            maximum = maximum,
            sourceEntityId = sourceEntityId,
            sourceName = sourceName
        )
    }
}

/**
 * Response to a ChooseNumber decision.
 */
@Serializable
data class NumberChoice(
    override val decisionId: String,
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
    override val decisionId: String,
    override val playerId: EntityId,
    val canCastSpells: Boolean,
    val canActivateAbilities: Boolean,
    val canPlayLand: Boolean,
    val stackIsEmpty: Boolean
) : PlayerDecision {
    override val description: String = "You have priority"

    companion object {
        fun create(
            playerId: EntityId,
            canCastSpells: Boolean,
            canActivateAbilities: Boolean,
            canPlayLand: Boolean,
            stackIsEmpty: Boolean
        ) = PriorityDecision(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            canCastSpells = canCastSpells,
            canActivateAbilities = canActivateAbilities,
            canPlayLand = canPlayLand,
            stackIsEmpty = stackIsEmpty
        )
    }
}

/**
 * Response to a PriorityDecision.
 */
@Serializable
sealed interface PriorityChoice : DecisionResponse {
    @Serializable
    data class Pass(override val decisionId: String) : PriorityChoice

    @Serializable
    data class CastSpell(override val decisionId: String, val cardId: EntityId) : PriorityChoice

    @Serializable
    data class ActivateAbility(override val decisionId: String, val sourceEntityId: EntityId, val abilityIndex: Int = 0) : PriorityChoice

    @Serializable
    data class PlayLand(override val decisionId: String, val cardId: EntityId) : PriorityChoice
}

// =============================================================================
// Mulligan Decision
// =============================================================================

/**
 * Player must decide whether to mulligan.
 */
@Serializable
data class MulliganDecision(
    override val decisionId: String,
    override val playerId: EntityId,
    val handSize: Int,
    val mulliganNumber: Int
) : PlayerDecision {
    override val description: String = if (mulliganNumber == 0) {
        "Keep your opening hand or mulligan?"
    } else {
        "Keep this hand (putting $mulliganNumber card(s) on the bottom) or mulligan again?"
    }

    companion object {
        fun create(
            playerId: EntityId,
            handSize: Int,
            mulliganNumber: Int
        ) = MulliganDecision(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            handSize = handSize,
            mulliganNumber = mulliganNumber
        )
    }
}

/**
 * Response to a MulliganDecision.
 */
@Serializable
sealed interface MulliganChoice : DecisionResponse {
    @Serializable
    data class Keep(override val decisionId: String) : MulliganChoice

    @Serializable
    data class Mulligan(override val decisionId: String) : MulliganChoice
}

/**
 * Player must choose cards to put on the bottom of their library after mulliganing.
 */
@Serializable
data class ChooseMulliganBottomCards(
    override val decisionId: String,
    override val playerId: EntityId,
    val hand: List<EntityId>,
    val cardsToPutOnBottom: Int
) : PlayerDecision {
    override val description: String = "Choose $cardsToPutOnBottom card(s) to put on the bottom of your library"

    companion object {
        fun create(
            playerId: EntityId,
            hand: List<EntityId>,
            cardsToPutOnBottom: Int
        ) = ChooseMulliganBottomCards(
            decisionId = PlayerDecision.generateId(),
            playerId = playerId,
            hand = hand,
            cardsToPutOnBottom = cardsToPutOnBottom
        )
    }
}

// =============================================================================
// Base Response Interface
// =============================================================================

/**
 * Marker interface for all decision responses.
 * Each response includes the decisionId to match it with the original decision.
 */
@Serializable
sealed interface DecisionResponse {
    val decisionId: String
}
