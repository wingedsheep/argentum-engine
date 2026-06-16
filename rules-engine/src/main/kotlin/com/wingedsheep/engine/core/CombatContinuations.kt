package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
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
 * Resume a [CombatResolutionDecision] (the bipartite combat-damage board).
 *
 * @property firstStrike Whether this is the first-strike combat damage step.
 * @property pendingChoosers The choosers still to confirm, in CR 510.1c order (attacker-side
 *   editors first, then blocker-side). The head is the current chooser; the resumer filters
 *   the response to edges they own and re-pauses for the next chooser until the queue empties.
 *   For the two-actor banding case this carries both players (CR 702.22j + 702.22k).
 * @property decisionShape The cached decision (attackers/blockers/defenders/edges). The resumer
 *   reads [DamageEdge.editableBy] and [DamageEdge.sourceId]/[DamageEdge.targetId] from here
 *   rather than re-deriving them, so edge ids never need to be parsed.
 */
@Serializable
data class CombatResolutionContinuation(
    override val decisionId: String,
    val firstStrike: Boolean,
    val pendingChoosers: List<EntityId>,
    val decisionShape: CombatResolutionDecision,
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
    val sourceName: String?,
    /** Arbitrary follow-up effect run when the chosen source's damage is prevented (null = pure prevention). */
    val onPrevented: Effect? = null,
    /** When false, the chosen source's damage is not prevented — it still hits, the reaction still fires (Eye for an Eye). */
    val preventDamage: Boolean = true
) : ContinuationFrame

/**
 * Continuation for PreventNextDamageFromChosenSourceEffect.
 *
 * Resume after a player chooses a damage source. Creates a prevention shield
 * on the target that only prevents damage from the chosen source.
 *
 * @property controllerId The player who controls the spell
 * @property targetId The entity receiving the prevention shield
 * @property amount The amount of damage to prevent; null means prevent all damage from the chosen
 *   source for the rest of the turn (Samite Ministration)
 * @property gainLifeFromColors Color enum names whose prevented damage gives the affected player
 *   that much life (only used when [amount] is null)
 * @property sourceId The spell/ability that created this effect
 * @property sourceName Name of the source for display
 */
@Serializable
data class PreventDamageFromChosenSourceContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val targetId: EntityId,
    val amount: Int?,
    val gainLifeFromColors: Set<String> = emptySet(),
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame
