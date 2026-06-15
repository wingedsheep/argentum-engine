package com.wingedsheep.engine.state.components.combat

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Marks a creature as attacking.
 *
 * @property bandId If non-null, this attacker is part of a band (CR 702.22). Every attacker
 *   in the same band shares this id, attacks the same defender, and is blocked as a group.
 */
@Serializable
data class AttackingComponent(
    val defenderId: EntityId,  // Player or planeswalker being attacked
    val bandId: String? = null
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
 * Marks a creature that blocked, or was blocked by, a legendary creature at some point during
 * the current turn. Stamped at block-declaration time (capturing the partner's legendary status
 * at the moment of the pairing, so it survives that legendary creature leaving or losing
 * legendary-ness), and cleared at end-of-turn cleanup. Backs
 * [com.wingedsheep.sdk.scripting.predicates.StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn]
 * (You Cannot Pass!, LTR).
 */
@Serializable
data object BlockedOrWasBlockedByLegendaryThisTurnComponent : Component

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
 * Per Rule 510.1c: an attacker blocked by 2+ creatures has its damage divided
 * among them as its controller chooses; the engine surfaces this as an explicit
 * ordering of blockers.
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

/**
 * Stores the order in which attackers receive damage from a blocker that blocks multiple attackers.
 * Set during the Declare Blockers step when a blocker blocks multiple attacking creatures.
 *
 * Per Rule 510.1d: a blocker blocking 2+ attackers has its damage divided
 * among them as its controller chooses; the engine surfaces this as an explicit
 * ordering of attackers.
 *
 * @property orderedAttackers Index 0 is the first creature to receive damage
 */
@Serializable
data class AttackerOrderComponent(
    val orderedAttackers: List<EntityId>
) : Component

/**
 * Marks a player whose creatures must attack a specific defender if able.
 * Used by Taunt and similar effects.
 *
 * This component is added to the player who was targeted by Taunt.
 * During their next turn, all creatures they control that can attack
 * must attack the specified defender (the Taunt caster).
 *
 * The component is removed after that player's combat phase ends.
 */
@Serializable
data class MustAttackPlayerComponent(
    /** The defender that must be attacked (the Taunt caster) */
    val defenderId: EntityId,
    /** Whether this is active for the current turn (set to true at start of affected player's turn) */
    val activeThisTurn: Boolean = false
) : Component

/**
 * Marks a creature that must attack this turn if able.
 * Used by Walking Desecration and similar effects that force specific creatures to attack.
 *
 * Unlike MustAttackPlayerComponent (which is on a player and forces all their creatures),
 * this is placed on individual creatures. The creature can attack any legal defender.
 *
 * Removed at end of turn during cleanup.
 */
@Serializable
data object MustAttackThisTurnComponent : Component

/**
 * Marker component letting a creature attack this turn as though it didn't have defender.
 *
 * Added by [com.wingedsheep.sdk.scripting.effects.CanAttackDespiteDefenderThisTurnEffect] (e.g. Krotiq
 * Nestguard's "{2}{G}: This creature can attack this turn as though it didn't have defender").
 * The defender attack-restriction rule honors it, and it is removed at end of turn during cleanup.
 */
@Serializable
data object CanAttackDespiteDefenderThisTurnComponent : Component

/**
 * Marker component added to a player when they have declared at least one attacker this turn.
 * Used for Raid abilities and similar "if you attacked this turn" checks.
 * Persists past END_COMBAT; removed at end of turn during cleanup.
 */
@Serializable
data object PlayerAttackedThisTurnComponent : Component

/**
 * Records the entity IDs of every creature that the player has declared as an attacker
 * during the current turn. Used by abilities that count attackers matching a filter
 * (e.g., Deepway Navigator: "as long as you attacked with three or more Merfolk this turn").
 *
 * The set is the union across all combat phases this turn. Cleared at end of turn.
 */
@Serializable
data class PlayerAttackersThisTurnComponent(
    val attackerIds: Set<EntityId>
) : Component

/**
 * Records which players this player has "attacked" this turn (CR 508.6): the set of
 * defending players against whom they declared one or more attackers. A creature's
 * defending player is the player it's attacking, or the controller of the planeswalker /
 * protector of the battle it's attacking (CR 508.5), so attacking an opponent's
 * planeswalker counts as having attacked that opponent.
 *
 * Stamped on the attacking player in [com.wingedsheep.engine.mechanics.combat.AttackPhaseManager]
 * at declare-attackers time (union across all combat phases this turn). Cleared at end of
 * turn during cleanup. Read by the `PlayerAttackedPlayerThisTurn` condition (Faramir, Prince
 * of Ithilien: "you draw a card if they didn't attack you that turn").
 */
@Serializable
data class PlayerAttackedPlayersThisTurnComponent(
    val defendingPlayerIds: Set<EntityId>
) : Component

/**
 * The creature carries the "goaded" designation (CR 701.15).
 *
 * Each entry in [goaderIds] is a player who has goaded this creature; the designation
 * lasts until that player's next turn (CR 701.15a) — entries are removed by
 * [com.wingedsheep.engine.core.CleanupPhaseManager.expireGoadedDesignationFor] after
 * the untap step of that goader's next turn (same hook as the floating-effect
 * `Duration.UntilYourNextTurn` path), and the whole component is removed when the set
 * becomes empty.
 *
 * A goaded creature attacks each combat if able (handled by `AttackPhaseManager`'s
 * mandatory-attacker validation) and must attack a player other than each goader if
 * able (handled by per-creature defender validation). Multiple goaders compound those
 * requirements (CR 701.15c); a player re-goading is a no-op via set semantics
 * (CR 701.15d). The designation is neither an ability nor part of the permanent's
 * copiable values (CR 701.15b), so it lives entirely as runtime state rather than as
 * a card-definition static ability.
 */
@Serializable
data class GoadedComponent(
    val goaderIds: Set<EntityId>
) : Component
