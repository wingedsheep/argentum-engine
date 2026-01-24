package com.wingedsheep.engine.state.components.combat

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Marks a creature as attacking.
 */
@Serializable
data class AttackingComponent(
    val defenderId: EntityId  // Player or planeswalker being attacked
) : Component

/**
 * Marks a creature as blocking.
 */
@Serializable
data class BlockingComponent(
    val blockedAttackerIds: List<EntityId>
) : Component

/**
 * Marks a creature as blocked (one or more blockers declared).
 */
@Serializable
data class BlockedComponent(
    val blockerIds: List<EntityId>
) : Component

/**
 * Combat damage assignment for a creature.
 */
@Serializable
data class DamageAssignmentComponent(
    val assignments: Map<EntityId, Int>  // target -> damage amount
) : Component

/**
 * Stores the order in which blockers receive damage from an attacker.
 * Set during the Declare Blockers step when an attacker is blocked by multiple creatures.
 *
 * Per Rule 509.2: "The attacking player announces the damage assignment order
 * among the blocking creatures."
 *
 * @property orderedBlockers Index 0 is the first creature to receive damage
 */
@Serializable
data class DamageAssignmentOrderComponent(
    val orderedBlockers: List<EntityId>
) : Component

/**
 * Marks a creature as having dealt first strike damage this combat.
 */
@Serializable
data object DealtFirstStrikeDamageComponent : Component

/**
 * Marks an attacker as needing manual damage assignment.
 * Used when the attacking player wants to over-assign damage or
 * has trample and needs to decide damage split.
 */
@Serializable
data class RequiresManualDamageAssignmentComponent(
    val reason: DamageAssignmentReason
) : Component

/**
 * Reason why manual damage assignment is required.
 */
@Serializable
enum class DamageAssignmentReason {
    /** Attacker has trample and excess damage can go to player */
    TRAMPLE,
    /** Multiple blockers and player wants to over-assign */
    MULTIPLE_BLOCKERS,
    /** User preference to always manually assign */
    USER_PREFERENCE
}

/**
 * Marker component added to the active player when attackers have been declared this combat.
 * This is used to prevent the DeclareAttackers action from being offered again,
 * even if no creatures were selected to attack.
 * Cleared at END_COMBAT.
 */
@Serializable
data object AttackersDeclaredThisCombatComponent : Component

/**
 * Marker component added to the defending player when blockers have been declared this combat.
 * This is used to prevent the DeclareBlockers action from being offered again,
 * even if no creatures were selected to block.
 * Cleared at END_COMBAT.
 */
@Serializable
data object BlockersDeclaredThisCombatComponent : Component
