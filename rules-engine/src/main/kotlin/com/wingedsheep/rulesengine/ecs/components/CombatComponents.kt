package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Combat-related components for tracking attackers and blockers.
 *
 * These components are added during declare attackers/blockers steps
 * and removed at the end of combat.
 */

/**
 * Marks a creature as attacking in the current combat.
 *
 * Added during the declare attackers step.
 * Removed at end of combat.
 *
 * @property defendingPlayerId The player (or planeswalker) being attacked
 */
@Serializable
data class AttackingComponent(
    val defendingPlayerId: EntityId
) : Component

/**
 * Marks a creature as blocking in the current combat.
 *
 * Added during the declare blockers step.
 * Removed at end of combat.
 *
 * @property attackerId The attacking creature this creature is blocking
 */
@Serializable
data class BlockingComponent(
    val attackerId: EntityId
) : Component

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
