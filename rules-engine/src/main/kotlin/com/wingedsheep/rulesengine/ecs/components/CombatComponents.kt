package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Combat-related components for tracking attackers, blockers, and damage assignment.
 *
 * These components are added during declare attackers/blockers steps
 * and removed at the end of combat.
 */

// =============================================================================
// Combat Target (What is being attacked)
// =============================================================================

/**
 * Represents the target of an attack.
 *
 * In MTG, creatures can attack:
 * - A player (the most common case)
 * - A planeswalker controlled by an opponent
 * - A battle (in newer sets)
 */
@Serializable
sealed interface CombatTarget {
    /** Attacking a player directly. */
    @Serializable
    data class Player(val playerId: EntityId) : CombatTarget

    /** Attacking a planeswalker. */
    @Serializable
    data class Planeswalker(val planeswalkerEntityId: EntityId) : CombatTarget

    /** Attacking a battle (Aftermath mechanic). */
    @Serializable
    data class Battle(val battleEntityId: EntityId) : CombatTarget
}

// =============================================================================
// Attacker Components
// =============================================================================

/**
 * Marks a creature as attacking in the current combat.
 *
 * Added during the declare attackers step.
 * Removed at end of combat.
 *
 * @property target The player, planeswalker, or battle being attacked
 * @property hasFirstStrike True if this creature has First Strike (from keywords or effects)
 * @property hasDoubleStrike True if this creature has Double Strike (from keywords or effects)
 * @property dealtFirstStrikeDamage True if this creature already dealt damage in the first strike step
 */
@Serializable
data class AttackingComponent(
    val target: CombatTarget,
    val hasFirstStrike: Boolean = false,
    val hasDoubleStrike: Boolean = false,
    val dealtFirstStrikeDamage: Boolean = false
) : Component {
    /**
     * Backward compatibility: get defending player ID directly.
     * Only works if target is a Player; throws otherwise.
     */
    @Deprecated(
        "Use target property instead",
        ReplaceWith("(target as CombatTarget.Player).playerId")
    )
    val defendingPlayerId: EntityId
        get() = when (target) {
            is CombatTarget.Player -> target.playerId
            is CombatTarget.Planeswalker -> target.planeswalkerEntityId
            is CombatTarget.Battle -> target.battleEntityId
        }

    /**
     * Check if this creature should deal damage in the first strike step.
     */
    val dealsFirstStrikeDamage: Boolean
        get() = hasFirstStrike || hasDoubleStrike

    /**
     * Check if this creature should deal damage in the regular damage step.
     */
    val dealsRegularDamage: Boolean
        get() = hasDoubleStrike || (!hasFirstStrike && !dealtFirstStrikeDamage)

    /**
     * Mark that this creature dealt first strike damage.
     */
    fun withFirstStrikeDamageDealt(): AttackingComponent =
        copy(dealtFirstStrikeDamage = true)

    companion object {
        /**
         * Create an AttackingComponent targeting a player.
         */
        fun attackingPlayer(
            playerId: EntityId,
            hasFirstStrike: Boolean = false,
            hasDoubleStrike: Boolean = false
        ): AttackingComponent = AttackingComponent(
            target = CombatTarget.Player(playerId),
            hasFirstStrike = hasFirstStrike,
            hasDoubleStrike = hasDoubleStrike
        )

        /**
         * Create an AttackingComponent targeting a planeswalker.
         */
        fun attackingPlaneswalker(
            planeswalkerEntityId: EntityId,
            hasFirstStrike: Boolean = false,
            hasDoubleStrike: Boolean = false
        ): AttackingComponent = AttackingComponent(
            target = CombatTarget.Planeswalker(planeswalkerEntityId),
            hasFirstStrike = hasFirstStrike,
            hasDoubleStrike = hasDoubleStrike
        )
    }
}

// =============================================================================
// Blocker Components
// =============================================================================

/**
 * Marks a creature as blocking in the current combat.
 *
 * Added during the declare blockers step.
 * Removed at end of combat.
 *
 * @property attackerId The attacking creature this creature is blocking
 * @property blockOrder The position in the blocking order for the attacker's damage assignment.
 *                      Lower numbers receive damage first. 0 = first in line.
 *                      This is set by the attacking player when ordering blockers.
 */
@Serializable
data class BlockingComponent(
    val attackerId: EntityId,
    val blockOrder: Int = 0
) : Component

/**
 * Tracks all blockers assigned to an attacking creature, in damage assignment order.
 *
 * Added to the ATTACKER when blockers are declared and ordered.
 * The attacking player chooses the order in which to assign damage to blockers.
 * Damage must be assigned to earlier blockers before later ones (lethal damage rule).
 *
 * @property blockerIds Ordered list of blocker entity IDs.
 *                      Index 0 = first to receive damage.
 */
@Serializable
data class BlockedByComponent(
    val blockerIds: List<EntityId>
) : Component {
    /** Check if this attacker has any blockers. */
    val isBlocked: Boolean get() = blockerIds.isNotEmpty()

    /** Number of creatures blocking this attacker. */
    val blockerCount: Int get() = blockerIds.size

    /** Get the first blocker in damage assignment order. */
    val firstBlocker: EntityId? get() = blockerIds.firstOrNull()

    /**
     * Add a blocker at the end of the order.
     */
    fun addBlocker(blockerId: EntityId): BlockedByComponent =
        copy(blockerIds = blockerIds + blockerId)

    /**
     * Set the complete blocker order (used when attacking player orders blockers).
     */
    fun setOrder(orderedBlockers: List<EntityId>): BlockedByComponent =
        copy(blockerIds = orderedBlockers)

    /**
     * Remove a blocker (e.g., if it leaves the battlefield).
     */
    fun removeBlocker(blockerId: EntityId): BlockedByComponent =
        copy(blockerIds = blockerIds.filter { it != blockerId })
}

// =============================================================================
// Combat Restriction Components
// =============================================================================

/**
 * Marks a creature that must be blocked if able.
 *
 * Used for effects like "This creature must be blocked if able" or
 * "All creatures able to block this creature must do so."
 *
 * This flag is typically temporary (until end of turn).
 */
@Serializable
data object MustBeBlockedComponent : Component

/**
 * Marks a creature that cannot be blocked.
 *
 * Used for "unblockable" effects or creatures with evasion
 * that bypasses all blockers.
 */
@Serializable
data object CantBeBlockedComponent : Component

/**
 * Marks a creature that cannot attack.
 *
 * Used for "can't attack" effects (distinct from Defender keyword).
 */
@Serializable
data object CantAttackComponent : Component

/**
 * Marks a creature that cannot block.
 *
 * Used for "can't block" effects.
 */
@Serializable
data object CantBlockComponent : Component

/**
 * Marks a creature that must attack if able.
 *
 * Used for effects like "attacks each turn if able" or berserker effects.
 */
@Serializable
data object MustAttackComponent : Component

/**
 * Marks a creature that must block if able.
 *
 * Used for effects like "blocks each turn if able".
 */
@Serializable
data object MustBlockComponent : Component

// =============================================================================
// Attack Cost Components
// =============================================================================

/**
 * Represents a cost that must be paid to attack with this creature.
 *
 * Used for Propaganda-style effects: "Creatures can't attack you unless
 * their controller pays {2} for each creature they control that's attacking you."
 *
 * @property manaCost The mana cost to pay (as a string like "{2}" or "{W}")
 * @property sourceId The entity imposing this cost (for tracking/removal)
 */
@Serializable
data class AttackCostComponent(
    val manaCost: String,
    val sourceId: EntityId
) : Component

/**
 * Represents a cost that must be paid to block with this creature.
 *
 * @property manaCost The mana cost to pay
 * @property sourceId The entity imposing this cost
 */
@Serializable
data class BlockCostComponent(
    val manaCost: String,
    val sourceId: EntityId
) : Component

// =============================================================================
// Damage Tracking Components
// =============================================================================

/**
 * Tracks combat damage that will be dealt by this creature.
 *
 * Used during damage calculation to track pending damage assignments
 * before they are actually applied.
 *
 * @property damageAssignments Map of target EntityId to damage amount
 */
@Serializable
data class PendingCombatDamageComponent(
    val damageAssignments: Map<EntityId, Int>
) : Component {
    /** Total damage this creature will deal. */
    val totalDamage: Int get() = damageAssignments.values.sum()

    /**
     * Add or update a damage assignment.
     */
    fun assignDamage(targetId: EntityId, amount: Int): PendingCombatDamageComponent {
        val current = damageAssignments[targetId] ?: 0
        return copy(damageAssignments = damageAssignments + (targetId to current + amount))
    }
}
