package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Resume combat damage assignment for the legacy unblocked DivideCombatDamageFreely
 * flow (paired with [DistributeDecision] for cards like Butcher Orgg that hit
 * an arbitrary set of defending creatures + defender).
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
 * Resume after attacking player declares damage assignment order for blockers.
 *
 * Per MTG CR 510.1c, an attacking creature blocked by two or more creatures
 * assigns its combat damage to those creatures divided as its controller chooses.
 * The engine surfaces this as an explicit ordering decision so the attacking
 * player can specify the division before damage is dealt.
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
 * Per CR 510.1d, when a single blocker blocks multiple attackers, the attacking
 * player orders those attackers to determine how the blocker divides its combat
 * damage.
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
/**
 * Resume after a player submits a [CombatResolutionDecision]. The resumer
 * folds edges back into per-attacker / per-blocker
 * [com.wingedsheep.engine.state.components.combat.DamageAssignmentComponent]
 * plus order-component updates, then re-enters
 * `applyCombatDamage(firstStrike)`.
 *
 * For the banding two-actor case (CR 702.22j/k) the continuation tracks which
 * players still need to confirm. When [pendingChoosers] still has entries
 * after applying one player's response, the engine re-pauses on a fresh
 * decision id with the next chooser as `playerId` and the prior chooser's
 * submitted amounts baked into [decisionShape.edges]. The full
 * [decisionShape] is persisted so re-emission doesn't re-derive PlanCandidate
 * state from a possibly-stale snapshot.
 *
 * @property firstStrike Whether the plan was for the first-strike damage step.
 * @property pendingChoosers Players still expected to confirm. The head of
 *   the list is the player currently being prompted; tail are queued. Empty
 *   means the resumer should apply assignments and proceed.
 * @property decisionShape Cached decision payload — attackers, blockers,
 *   defenders, and edges (with the latest amounts) — so the resumer can
 *   re-pause the next chooser without re-deriving the graph from scratch.
 *   Null when the resolution board feature flag is off and the continuation
 *   isn't used in the multi-chooser path.
 */
@Serializable
data class CombatResolutionContinuation(
    override val decisionId: String,
    val firstStrike: Boolean,
    val pendingChoosers: List<EntityId> = emptyList(),
    val decisionShape: CombatResolutionDecision? = null,
) : ContinuationFrame

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
