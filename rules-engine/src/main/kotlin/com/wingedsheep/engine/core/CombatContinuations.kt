package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Resume combat damage assignment for creatures with DivideCombatDamageFreely.
 *
 * @property attackerId The attacking creature assigning damage
 * @property defendingPlayerId The defending player
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class DamageAssignmentContinuation(
    override val decisionId: String,
    val attackerId: EntityId,
    val defendingPlayerId: EntityId,
    val firstStrike: Boolean = false
) : ContinuationFrame

/**
 * Resume combat damage after player decides whether to assign damage as though unblocked.
 * Used for creatures with AssignCombatDamageAsUnblocked (e.g. Thorn Elemental).
 *
 * @property attackerId The attacking creature with the ability
 * @property defendingPlayerId The defending player
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class AssignAsUnblockedContinuation(
    override val decisionId: String,
    val attackerId: EntityId,
    val defendingPlayerId: EntityId,
    val firstStrike: Boolean = false
) : ContinuationFrame

/**
 * Resume after attacking player declares damage assignment order for blockers.
 *
 * Per MTG CR 509.2, after the defending player declares blockers, the attacking
 * player must declare the damage assignment order for each attacking creature
 * that's blocked by multiple creatures.
 *
 * @property attackingPlayerId The attacking player who must order the blockers
 * @property attackerId The attacking creature whose blockers are being ordered
 * @property attackerName Name of the attacker for display
 * @property remainingAttackers List of attackers that still need ordering after this one
 */
@Serializable
data class BlockerOrderContinuation(
    override val decisionId: String,
    val attackingPlayerId: EntityId,
    val attackerId: EntityId,
    val attackerName: String,
    val remainingAttackers: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the attacking player orders their attackers for a blocker's damage assignment.
 *
 * When a single blocker blocks multiple attackers, the attacking player must order those
 * attackers to determine how the blocker divides its combat damage (CR 509.3).
 *
 * @property attackingPlayerId The attacking player who must order their attackers
 * @property blockerId The blocking creature whose attackers are being ordered
 * @property blockerName Name of the blocker for display
 * @property remainingBlockers List of blockers that still need attacker ordering after this one
 */
@Serializable
data class AttackerOrderContinuation(
    override val decisionId: String,
    val attackingPlayerId: EntityId,
    val blockerId: EntityId,
    val blockerName: String,
    val remainingBlockers: List<EntityId>
) : ContinuationFrame

/**
 * Resume after player has distributed damage among targets.
 *
 * Used for effects like Forked Lightning where the player divides damage
 * among multiple targets. The continuation is pushed when there are multiple
 * targets, and the response contains the damage distribution.
 *
 * @property sourceId The spell/ability that is dealing the damage
 * @property controllerId The player who controls the effect
 * @property targets The targets that damage can be distributed among
 */
@Serializable
data class DistributeDamageContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val targets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after defending player distributes damage prevention among multiple combat damage sources.
 *
 * Per CR 615.7, when a prevention effect can't prevent all simultaneous damage from multiple
 * sources, the affected player chooses how to distribute the prevention.
 *
 * @property recipientId The player/creature receiving damage
 * @property shieldEffectId The floating effect ID of the PreventNextDamage shield
 * @property shieldAmount Total prevention available from the shield
 * @property damageBySource Map of attacker entity ID → raw damage amount
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class DamagePreventionContinuation(
    override val decisionId: String,
    val recipientId: EntityId,
    val shieldEffectId: EntityId,
    val shieldAmount: Int,
    val damageBySource: Map<EntityId, Int>,
    val firstStrike: Boolean
) : ContinuationFrame

/**
 * Resume after a player chooses a source of damage for Deflecting Palm-style effects.
 *
 * Creates a floating effect that prevents the next time the chosen source would deal
 * damage to the controller this turn, and deals that much damage to the source's controller.
 *
 * @property controllerId The player who controls the spell
 * @property sourceId The Deflecting Palm card entity (source of reflected damage)
 * @property sourceName Name of the source for display
 */
@Serializable
data class DeflectDamageSourceChoiceContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Continuation for PreventNextDamageFromChosenSourceEffect.
 *
 * Resume after a player chooses a damage source. Creates a prevention shield
 * on the target that only prevents damage from the chosen source.
 *
 * @property controllerId The player who controls the spell
 * @property targetId The entity receiving the prevention shield
 * @property amount The amount of damage to prevent
 * @property sourceId The spell/ability that created this effect
 * @property sourceName Name of the source for display
 */
@Serializable
data class PreventDamageFromChosenSourceContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val targetId: EntityId,
    val amount: Int,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame
